package com.smartstock.supplier.unit;

import com.smartstock.supplier.api.dto.request.CreateSupplierRequest;
import com.smartstock.supplier.api.dto.request.SuspendSupplierRequest;
import com.smartstock.supplier.api.dto.request.UpdateSupplierRequest;
import com.smartstock.supplier.api.dto.response.PagedResponse;
import com.smartstock.supplier.api.dto.response.SupplierResponse;
import com.smartstock.supplier.api.dto.response.SupplierSummaryResponse;
import com.smartstock.supplier.domain.model.Supplier;
import com.smartstock.supplier.domain.repository.SupplierRepository;
import com.smartstock.supplier.exception.SupplierCodeExistsException;
import com.smartstock.supplier.exception.SupplierNotFoundException;
import com.smartstock.supplier.service.SupplierEventPublisher;
import com.smartstock.supplier.service.SupplierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierService Unit Tests")
class SupplierServiceUnitTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private SupplierEventPublisher eventPublisher;

    @InjectMocks
    private SupplierService supplierService;

    private Supplier activeSupplier;

    @BeforeEach
    void setUp() {
        activeSupplier = Supplier.builder()
                .id("sup-001")
                .supplierCode("SUPP-001")
                .supplierName("Acme Corp")
                .supplierType("MANUFACTURER")
                .isActive(true)
                .isVerified(false)
                .riskRating("MEDIUM")
                .currencyCode("USD")
                .countryCode("US")
                .totalOrders(0)
                .totalSpent(BigDecimal.ZERO)
                .createdBy("user-01")
                .updatedBy("user-01")
                .build();
    }

    @Nested
    @DisplayName("createSupplier")
    class CreateSupplier {

        @Test
        @DisplayName("creates supplier when code is unique")
        void createSupplier_success() {
            CreateSupplierRequest req = new CreateSupplierRequest();
            req.setSupplierCode("SUPP-001");
            req.setSupplierName("Acme Corp");
            req.setSupplierType("MANUFACTURER");

            when(supplierRepository.existsBySupplierCode("SUPP-001")).thenReturn(false);
            when(supplierRepository.save(any(Supplier.class))).thenReturn(activeSupplier);
            doNothing().when(eventPublisher).publishSupplierCreated(any());

            SupplierResponse result = supplierService.createSupplier(req, "user-01");

            assertThat(result).isNotNull();
            assertThat(result.getSupplierCode()).isEqualTo("SUPP-001");
            assertThat(result.getSupplierName()).isEqualTo("Acme Corp");
            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            verify(supplierRepository).save(any(Supplier.class));
            verify(eventPublisher).publishSupplierCreated(any());
        }

        @Test
        @DisplayName("throws SupplierCodeExistsException when code already used")
        void createSupplier_duplicateCode() {
            CreateSupplierRequest req = new CreateSupplierRequest();
            req.setSupplierCode("SUPP-001");
            req.setSupplierName("Acme Corp");

            when(supplierRepository.existsBySupplierCode("SUPP-001")).thenReturn(true);

            assertThatThrownBy(() -> supplierService.createSupplier(req, "user-01"))
                    .isInstanceOf(SupplierCodeExistsException.class);

            verify(supplierRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getSupplier")
    class GetSupplier {

        @Test
        @DisplayName("returns supplier when found")
        void getSupplier_found() {
            when(supplierRepository.findById("sup-001")).thenReturn(Optional.of(activeSupplier));

            SupplierResponse result = supplierService.getSupplier("sup-001");

            assertThat(result.getId()).isEqualTo("sup-001");
            assertThat(result.getSupplierCode()).isEqualTo("SUPP-001");
        }

        @Test
        @DisplayName("throws SupplierNotFoundException when not found")
        void getSupplier_notFound() {
            when(supplierRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> supplierService.getSupplier("missing"))
                    .isInstanceOf(SupplierNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateSupplier")
    class UpdateSupplier {

        @Test
        @DisplayName("updates fields and publishes event")
        void updateSupplier_success() {
            UpdateSupplierRequest req = new UpdateSupplierRequest();
            req.setSupplierName("Acme Corp Updated");
            req.setCity("New York");

            when(supplierRepository.findById("sup-001")).thenReturn(Optional.of(activeSupplier));
            when(supplierRepository.save(any())).thenReturn(activeSupplier);
            doNothing().when(eventPublisher).publishSupplierUpdated(any());

            supplierService.updateSupplier("sup-001", req, "user-01");

            verify(supplierRepository).save(any());
            verify(eventPublisher).publishSupplierUpdated(any());
        }
    }

    @Nested
    @DisplayName("suspendSupplier / resumeSupplier")
    class SuspendResume {

        @Test
        @DisplayName("suspends an active supplier")
        void suspendSupplier_success() {
            SuspendSupplierRequest req = new SuspendSupplierRequest();
            req.setReason("Quality issues");
            req.setResumeDate(LocalDate.now().plusDays(30));

            when(supplierRepository.findById("sup-001")).thenReturn(Optional.of(activeSupplier));
            when(supplierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(eventPublisher).publishSupplierSuspended(any());

            SupplierResponse result = supplierService.suspendSupplier("sup-001", req, "user-01");

            assertThat(result.getStatus()).isEqualTo("SUSPENDED");
            verify(eventPublisher).publishSupplierSuspended(any());
        }

        @Test
        @DisplayName("resumes a suspended supplier")
        void resumeSupplier_success() {
            activeSupplier.suspend("Quality issues", LocalDate.now().plusDays(30));

            when(supplierRepository.findById("sup-001")).thenReturn(Optional.of(activeSupplier));
            when(supplierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(eventPublisher).publishSupplierResumed(any());

            SupplierResponse result = supplierService.resumeSupplier("sup-001", "user-01");

            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            verify(eventPublisher).publishSupplierResumed(any());
        }
    }

    @Nested
    @DisplayName("listSuppliers")
    class ListSuppliers {

        @Test
        @DisplayName("returns paged list of suppliers")
        void listSuppliers_paged() {
            PageRequest pageable = PageRequest.of(0, 20);
            when(supplierRepository.findWithFilters(any(), any(), any(), any(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(activeSupplier), pageable, 1));

            PagedResponse<SupplierSummaryResponse> result =
                    supplierService.listSuppliers(null, null, null, null, pageable);

            assertThat(result.getData()).hasSize(1);
            assertThat(result.getMeta().getTotal()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Supplier domain model")
    class SupplierDomainModel {

        @Test
        @DisplayName("resolves status correctly")
        void statusResolution() {
            Supplier s = Supplier.builder()
                    .isActive(true)
                    .createdBy("u").updatedBy("u")
                    .supplierCode("X").supplierName("X")
                    .totalOrders(0).totalSpent(BigDecimal.ZERO)
                    .build();
            assertThat(s.isActive()).isTrue();
            assertThat(s.isSuspended()).isFalse();

            s.suspend("reason", LocalDate.now().plusDays(10));
            assertThat(s.isActive()).isFalse();
            assertThat(s.isSuspended()).isTrue();

            s.activate();
            assertThat(s.isActive()).isTrue();
            assertThat(s.isSuspended()).isFalse();
        }

        @Test
        @DisplayName("increments order count and spend correctly")
        void incrementOrderCount() {
            Supplier s = Supplier.builder()
                    .isActive(true)
                    .createdBy("u").updatedBy("u")
                    .supplierCode("X").supplierName("X")
                    .totalOrders(5).totalSpent(BigDecimal.valueOf(1000))
                    .build();

            s.incrementOrderCount(BigDecimal.valueOf(500));

            assertThat(s.getTotalOrders()).isEqualTo(6);
            assertThat(s.getTotalSpent()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        }
    }
}
