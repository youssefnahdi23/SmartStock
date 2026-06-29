package com.smartstock.warehouse.unit;

import com.smartstock.warehouse.api.dto.request.CreateWarehouseRequest;
import com.smartstock.warehouse.api.dto.response.WarehouseResponse;
import com.smartstock.warehouse.domain.model.Warehouse;
import com.smartstock.warehouse.domain.repository.WarehouseBinRepository;
import com.smartstock.warehouse.domain.repository.WarehouseRepository;
import com.smartstock.warehouse.domain.repository.WarehouseZoneRepository;
import com.smartstock.warehouse.exception.WarehouseCodeAlreadyExistsException;
import com.smartstock.warehouse.exception.WarehouseNotFoundException;
import com.smartstock.warehouse.service.WarehouseEventPublisher;
import com.smartstock.warehouse.service.WarehouseService;
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
@DisplayName("WarehouseService unit tests")
class WarehouseServiceUnitTest {

    @Mock private WarehouseRepository    warehouseRepository;
    @Mock private WarehouseZoneRepository zoneRepository;
    @Mock private WarehouseBinRepository  binRepository;
    @Mock private WarehouseEventPublisher eventPublisher;

    @InjectMocks
    private WarehouseService warehouseService;

    private static final String USER_ID = "user-001";

    @Test
    @DisplayName("createWarehouse — happy path returns response with correct fields")
    void createWarehouse_happyPath_returnsResponse() {
        CreateWarehouseRequest req = CreateWarehouseRequest.builder()
                .code("W01")
                .name("Main Warehouse")
                .type("GENERAL")
                .build();

        when(warehouseRepository.existsByCodeAndDeletedAtIsNull("W01")).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(inv -> {
            Warehouse w = inv.getArgument(0);
            w.setId("wh-001");
            return w;
        });

        WarehouseResponse response = warehouseService.createWarehouse(req, USER_ID);

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("W01");
        assertThat(response.getName()).isEqualTo("Main Warehouse");
        verify(eventPublisher).publishWarehouseCreated(any());
    }

    @Test
    @DisplayName("createWarehouse — duplicate code throws WarehouseCodeAlreadyExistsException")
    void createWarehouse_duplicateCode_throwsException() {
        CreateWarehouseRequest req = CreateWarehouseRequest.builder()
                .code("DUPE")
                .name("Duplicate")
                .build();

        when(warehouseRepository.existsByCodeAndDeletedAtIsNull("DUPE")).thenReturn(true);

        assertThatThrownBy(() -> warehouseService.createWarehouse(req, USER_ID))
                .isInstanceOf(WarehouseCodeAlreadyExistsException.class)
                .hasMessageContaining("DUPE");
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    @DisplayName("getWarehouse — not found throws WarehouseNotFoundException")
    void getWarehouse_notFound_throwsException() {
        when(warehouseRepository.findByIdAndNotDeleted("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.getWarehouse("missing"))
                .isInstanceOf(WarehouseNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    @DisplayName("deactivateWarehouse — sets active=false and publishes event")
    void deactivateWarehouse_setsInactiveAndPublishesEvent() {
        Warehouse warehouse = Warehouse.builder()
                .id("wh-001").code("W01").name("Main Warehouse")
                .active(true).type("GENERAL")
                .createdBy(USER_ID).updatedBy(USER_ID)
                .build();

        when(warehouseRepository.findByIdAndNotDeleted("wh-001")).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(inv -> inv.getArgument(0));

        WarehouseResponse response = warehouseService.deactivateWarehouse("wh-001", USER_ID);

        assertThat(response.isActive()).isFalse();
        verify(eventPublisher).publishWarehouseDeactivated(any());
    }

    @Test
    @DisplayName("reactivateWarehouse — sets active=true")
    void reactivateWarehouse_setsActive() {
        Warehouse warehouse = Warehouse.builder()
                .id("wh-001").code("W01").name("Main Warehouse")
                .active(false).type("GENERAL")
                .createdBy(USER_ID).updatedBy(USER_ID)
                .build();

        when(warehouseRepository.findByIdAndNotDeleted("wh-001")).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(inv -> inv.getArgument(0));

        WarehouseResponse response = warehouseService.reactivateWarehouse("wh-001", USER_ID);

        assertThat(response.isActive()).isTrue();
    }
}
