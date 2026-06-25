package com.smartstock.supplier.unit;

import com.smartstock.supplier.api.dto.request.ConfirmDeliveryRequest;
import com.smartstock.supplier.api.dto.request.RegisterDeliveryRequest;
import com.smartstock.supplier.api.dto.response.DeliveryResponse;
import com.smartstock.supplier.api.dto.response.SupplierPerformanceResponse;
import com.smartstock.supplier.domain.model.Supplier;
import com.smartstock.supplier.domain.model.SupplierDelivery;
import com.smartstock.supplier.domain.model.SupplierMetrics;
import com.smartstock.supplier.domain.repository.SupplierDeliveryRepository;
import com.smartstock.supplier.domain.repository.SupplierMetricsRepository;
import com.smartstock.supplier.exception.BusinessException;
import com.smartstock.supplier.service.SupplierDeliveryService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierDeliveryService Unit Tests")
class SupplierDeliveryServiceUnitTest {

    @Mock
    private SupplierDeliveryRepository deliveryRepository;

    @Mock
    private SupplierMetricsRepository metricsRepository;

    @Mock
    private SupplierService supplierService;

    @Mock
    private SupplierEventPublisher eventPublisher;

    @InjectMocks
    private SupplierDeliveryService deliveryService;

    private Supplier supplier;

    @BeforeEach
    void setUp() {
        supplier = Supplier.builder()
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
    @DisplayName("registerDelivery")
    class RegisterDelivery {

        @Test
        @DisplayName("registers delivery successfully when delivery number is unique")
        void registerDelivery_success() {
            RegisterDeliveryRequest req = new RegisterDeliveryRequest();
            req.setDeliveryNumber("DEL-001");
            req.setOrderDate(LocalDate.now().minusDays(3));
            req.setPromisedDeliveryDate(LocalDate.now().plusDays(4));
            req.setQuantityOrdered(100);

            SupplierDelivery saved = SupplierDelivery.builder()
                    .id("del-001")
                    .supplierId("sup-001")
                    .deliveryNumber("DEL-001")
                    .orderDate(req.getOrderDate())
                    .promisedDeliveryDate(req.getPromisedDeliveryDate())
                    .quantityOrdered(100)
                    .deliveryStatus("PENDING")
                    .build();

            when(supplierService.findById("sup-001")).thenReturn(supplier);
            when(deliveryRepository.existsByDeliveryNumber("DEL-001")).thenReturn(false);
            when(deliveryRepository.save(any(SupplierDelivery.class))).thenReturn(saved);
            doNothing().when(eventPublisher).publishDeliveryRegistered(any());

            DeliveryResponse result = deliveryService.registerDelivery("sup-001", req, "user-01");

            assertThat(result).isNotNull();
            assertThat(result.getDeliveryNumber()).isEqualTo("DEL-001");
            assertThat(result.getDeliveryStatus()).isEqualTo("PENDING");
            verify(deliveryRepository).save(any(SupplierDelivery.class));
            verify(eventPublisher).publishDeliveryRegistered(any());
        }

        @Test
        @DisplayName("throws BusinessException when delivery number already exists")
        void registerDelivery_duplicateNumber() {
            RegisterDeliveryRequest req = new RegisterDeliveryRequest();
            req.setDeliveryNumber("DEL-001");
            req.setOrderDate(LocalDate.now());
            req.setPromisedDeliveryDate(LocalDate.now().plusDays(7));
            req.setQuantityOrdered(50);

            when(supplierService.findById("sup-001")).thenReturn(supplier);
            when(deliveryRepository.existsByDeliveryNumber("DEL-001")).thenReturn(true);

            assertThatThrownBy(() -> deliveryService.registerDelivery("sup-001", req, "user-01"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("DEL-001");

            verify(deliveryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("confirmDelivery")
    class ConfirmDelivery {

        @Test
        @DisplayName("confirms delivery, updates status, and triggers metric recalculation")
        void confirmDelivery_success() {
            LocalDate orderDate = LocalDate.now().minusDays(10);
            LocalDate promised = LocalDate.now().minusDays(3);
            LocalDate actual = LocalDate.now().minusDays(4); // one day early — on time

            SupplierDelivery existing = SupplierDelivery.builder()
                    .id("del-001")
                    .supplierId("sup-001")
                    .deliveryNumber("DEL-001")
                    .orderDate(orderDate)
                    .promisedDeliveryDate(promised)
                    .quantityOrdered(100)
                    .deliveryStatus("PENDING")
                    .build();

            SupplierDelivery confirmed = SupplierDelivery.builder()
                    .id("del-001")
                    .supplierId("sup-001")
                    .deliveryNumber("DEL-001")
                    .orderDate(orderDate)
                    .promisedDeliveryDate(promised)
                    .actualDeliveryDate(actual)
                    .quantityOrdered(100)
                    .quantityReceived(100)
                    .quantityRejected(0)
                    .deliveryStatus("DELIVERED")
                    .onTime(true)
                    .onTimeDaysVariance(-1)
                    .qualityIssuesFound(0)
                    .qualityInspectionStatus("PASSED")
                    .build();

            ConfirmDeliveryRequest req = new ConfirmDeliveryRequest();
            req.setActualDeliveryDate(actual);
            req.setQuantityReceived(100);
            req.setQuantityRejected(0);
            req.setQualityIssuesFound(0);
            req.setQualityInspectionStatus("PASSED");

            when(deliveryRepository.findById("del-001")).thenReturn(Optional.of(existing));
            when(deliveryRepository.save(any(SupplierDelivery.class))).thenReturn(confirmed);
            // recalculateMetrics calls findDeliveredInPeriod — return the confirmed delivery
            when(deliveryRepository.findDeliveredInPeriod(eq("sup-001"), any(), any()))
                    .thenReturn(List.of(confirmed));
            when(metricsRepository.findBySupplierIdAndMetricDate(eq("sup-001"), any()))
                    .thenReturn(Optional.empty());
            when(metricsRepository.save(any(SupplierMetrics.class))).thenAnswer(inv -> inv.getArgument(0));
            when(supplierService.findById("sup-001")).thenReturn(supplier);
            doNothing().when(eventPublisher).publishPerformanceUpdated(any());

            DeliveryResponse result = deliveryService.confirmDelivery("sup-001", "del-001", req, "user-01");

            assertThat(result).isNotNull();
            assertThat(result.getDeliveryStatus()).isEqualTo("DELIVERED");
            assertThat(result.getOnTime()).isTrue();
            verify(metricsRepository).save(any(SupplierMetrics.class));
            verify(eventPublisher).publishPerformanceUpdated(any());
        }

        @Test
        @DisplayName("throws BusinessException when delivery not found for supplier")
        void confirmDelivery_notFound() {
            ConfirmDeliveryRequest req = new ConfirmDeliveryRequest();
            req.setActualDeliveryDate(LocalDate.now());
            req.setQuantityReceived(50);

            when(deliveryRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryService.confirmDelivery("sup-001", "missing", req, "user-01"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("getPerformance")
    class GetPerformance {

        @Test
        @DisplayName("returns performance data aggregated from deliveries in period")
        void getPerformance_success() {
            LocalDate from = LocalDate.now().minusDays(30);
            LocalDate to = LocalDate.now();

            SupplierDelivery d1 = SupplierDelivery.builder()
                    .id("del-001").supplierId("sup-001").deliveryNumber("DEL-001")
                    .orderDate(from.plusDays(5)).promisedDeliveryDate(from.plusDays(10))
                    .actualDeliveryDate(from.plusDays(9))
                    .quantityOrdered(100).quantityReceived(100).quantityRejected(0)
                    .deliveryStatus("DELIVERED").onTime(true).qualityIssuesFound(0)
                    .totalValue(BigDecimal.valueOf(500))
                    .build();

            SupplierDelivery d2 = SupplierDelivery.builder()
                    .id("del-002").supplierId("sup-001").deliveryNumber("DEL-002")
                    .orderDate(from.plusDays(12)).promisedDeliveryDate(from.plusDays(19))
                    .actualDeliveryDate(from.plusDays(21))
                    .quantityOrdered(50).quantityReceived(45).quantityRejected(5)
                    .deliveryStatus("DELIVERED").onTime(false).qualityIssuesFound(2)
                    .totalValue(BigDecimal.valueOf(225))
                    .build();

            when(supplierService.findById("sup-001")).thenReturn(supplier);
            when(deliveryRepository.findDeliveredInPeriod("sup-001", from, to))
                    .thenReturn(List.of(d1, d2));

            SupplierPerformanceResponse result = deliveryService.getPerformance("sup-001", from, to);

            assertThat(result).isNotNull();
            assertThat(result.getSupplierId()).isEqualTo("sup-001");
            assertThat(result.getTotalOrders()).isEqualTo(2L);
            assertThat(result.getOnTimeDeliveries()).isEqualTo(1L);
            assertThat(result.getLateDeliveries()).isEqualTo(1L);
            assertThat(result.getQualityIssuesCount()).isEqualTo(2);
            assertThat(result.getTotalValueReceived())
                    .isEqualByComparingTo(BigDecimal.valueOf(725));
        }

        @Test
        @DisplayName("returns zero metrics when no deliveries in period")
        void getPerformance_noDeliveries() {
            LocalDate from = LocalDate.now().minusDays(30);
            LocalDate to = LocalDate.now();

            when(supplierService.findById("sup-001")).thenReturn(supplier);
            when(deliveryRepository.findDeliveredInPeriod("sup-001", from, to))
                    .thenReturn(List.of());

            SupplierPerformanceResponse result = deliveryService.getPerformance("sup-001", from, to);

            assertThat(result.getTotalOrders()).isZero();
            assertThat(result.getOnTimeDeliveryRate()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getQualityPassRate()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
