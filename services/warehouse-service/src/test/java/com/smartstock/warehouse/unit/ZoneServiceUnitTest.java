package com.smartstock.warehouse.unit;

import com.smartstock.warehouse.api.dto.request.CreateZoneRequest;
import com.smartstock.warehouse.api.dto.response.ZoneResponse;
import com.smartstock.warehouse.domain.model.Warehouse;
import com.smartstock.warehouse.domain.model.WarehouseZone;
import com.smartstock.warehouse.domain.repository.*;
import com.smartstock.warehouse.exception.WarehouseNotFoundException;
import com.smartstock.warehouse.service.WarehouseEventPublisher;
import com.smartstock.warehouse.service.ZoneService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ZoneService unit tests")
class ZoneServiceUnitTest {

    @Mock private WarehouseRepository     warehouseRepository;
    @Mock private WarehouseZoneRepository zoneRepository;
    @Mock private WarehouseShelfRepository shelfRepository;
    @Mock private WarehouseBinRepository  binRepository;
    @Mock private WarehouseEventPublisher eventPublisher;

    @InjectMocks
    private ZoneService zoneService;

    private static final String USER_ID      = "user-001";
    private static final String WAREHOUSE_ID = "wh-001";

    @Test
    @DisplayName("createZone — happy path creates zone and publishes event")
    void createZone_happyPath_success() {
        Warehouse warehouse = Warehouse.builder()
                .id(WAREHOUSE_ID).code("W01").name("Main Warehouse")
                .active(true).createdBy(USER_ID).updatedBy(USER_ID).build();

        CreateZoneRequest req = CreateZoneRequest.builder()
                .code("ZONE-A")
                .name("Storage Zone A")
                .type("STORAGE")
                .build();

        when(warehouseRepository.findByIdAndNotDeleted(WAREHOUSE_ID)).thenReturn(Optional.of(warehouse));
        when(zoneRepository.existsByWarehouseIdAndCodeAndDeletedAtIsNull(WAREHOUSE_ID, "ZONE-A")).thenReturn(false);
        when(zoneRepository.save(any(WarehouseZone.class))).thenAnswer(inv -> {
            WarehouseZone z = inv.getArgument(0);
            z.setId("zone-001");
            return z;
        });

        ZoneResponse response = zoneService.createZone(WAREHOUSE_ID, req, USER_ID);

        assertThat(response.getCode()).isEqualTo("ZONE-A");
        assertThat(response.getName()).isEqualTo("Storage Zone A");
        assertThat(response.getType()).isEqualTo("STORAGE");
        verify(eventPublisher).publishZoneCreated(any());
    }

    @Test
    @DisplayName("createZone — warehouse not found throws exception")
    void createZone_warehouseNotFound_throwsException() {
        when(warehouseRepository.findByIdAndNotDeleted("missing")).thenReturn(Optional.empty());

        CreateZoneRequest req = CreateZoneRequest.builder()
                .code("ZONE-A").name("Zone").type("STORAGE").build();

        assertThatThrownBy(() -> zoneService.createZone("missing", req, USER_ID))
                .isInstanceOf(WarehouseNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    @DisplayName("createZone — duplicate code throws conflict exception")
    void createZone_duplicateCode_throwsException() {
        Warehouse warehouse = Warehouse.builder()
                .id(WAREHOUSE_ID).code("W01").name("Main Warehouse")
                .active(true).createdBy(USER_ID).updatedBy(USER_ID).build();

        when(warehouseRepository.findByIdAndNotDeleted(WAREHOUSE_ID)).thenReturn(Optional.of(warehouse));
        when(zoneRepository.existsByWarehouseIdAndCodeAndDeletedAtIsNull(WAREHOUSE_ID, "DUPE")).thenReturn(true);

        CreateZoneRequest req = CreateZoneRequest.builder()
                .code("DUPE").name("Zone").type("STORAGE").build();

        assertThatThrownBy(() -> zoneService.createZone(WAREHOUSE_ID, req, USER_ID))
                .hasMessageContaining("DUPE");
        verify(zoneRepository, never()).save(any());
    }
}
