package com.smartstock.purchase.unit;

import com.smartstock.purchase.api.dto.request.CancelPurchaseOrderRequest;
import com.smartstock.purchase.api.dto.request.ConfirmPurchaseOrderRequest;
import com.smartstock.purchase.api.dto.request.CreatePurchaseOrderRequest;
import com.smartstock.purchase.api.dto.response.PagedResponse;
import com.smartstock.purchase.api.dto.response.PurchaseOrderResponse;
import com.smartstock.purchase.api.dto.response.PurchaseOrderSummaryResponse;
import com.smartstock.purchase.domain.model.POLineItem;
import com.smartstock.purchase.domain.model.PurchaseOrder;
import com.smartstock.purchase.domain.repository.DeliveryTrackingRepository;
import com.smartstock.purchase.domain.repository.POLineItemRepository;
import com.smartstock.purchase.domain.repository.PurchaseOrderRepository;
import com.smartstock.purchase.domain.repository.QualityIssueRepository;
import com.smartstock.purchase.exception.DuplicatePONumberException;
import com.smartstock.purchase.exception.InvalidPurchaseOrderStateException;
import com.smartstock.purchase.exception.PurchaseOrderNotFoundException;
import com.smartstock.purchase.service.PurchaseOrderEventPublisher;
import com.smartstock.purchase.service.PurchaseOrderService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseOrderService Unit Tests")
class PurchaseOrderServiceUnitTest {

    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private POLineItemRepository lineItemRepository;
    @Mock private DeliveryTrackingRepository deliveryTrackingRepository;
    @Mock private QualityIssueRepository qualityIssueRepository;
    @Mock private PurchaseOrderEventPublisher eventPublisher;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private PurchaseOrder samplePO;

    @BeforeEach
    void setUp() {
        samplePO = PurchaseOrder.builder()
                .id("po-001")
                .poNumber("PO-2026-001")
                .supplierId("sup-001")
                .supplierName("Acme Supplies")
                .status("CREATED")
                .deliveryWarehouseId("W01")
                .orderDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(7))
                .expectedDeliveryDate(LocalDate.now().plusDays(7))
                .totalQuantity(100)
                .deliveredQuantity(0)
                .totalAmount(BigDecimal.valueOf(5000))
                .totalLineAmount(BigDecimal.valueOf(5000))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .deliveryStatus("NOT_RECEIVED")
                .paymentStatus("UNPAID")
                .lineItems(new ArrayList<>())
                .qualityIssues(new ArrayList<>())
                .createdBy("user-01")
                .updatedBy("user-01")
                .build();
    }

    @Nested
    @DisplayName("createPurchaseOrder")
    class CreatePurchaseOrder {

        @Test
        @DisplayName("creates PO when PO number is unique")
        void createPurchaseOrder_success() {
            CreatePurchaseOrderRequest req = new CreatePurchaseOrderRequest();
            req.setPoNumber("PO-2026-001");
            req.setSupplierId("sup-001");
            req.setOrderDate(LocalDate.now());
            req.setDueDate(LocalDate.now().plusDays(7));
            req.setExpectedDeliveryDate(LocalDate.now().plusDays(7));
            req.setDeliveryWarehouseId("W01");

            CreatePurchaseOrderRequest.CreateLineItemRequest item = new CreatePurchaseOrderRequest.CreateLineItemRequest();
            item.setProductId("prod-001");
            item.setQuantity(100);
            item.setUnitPrice(BigDecimal.valueOf(50));
            req.setItems(List.of(item));

            when(purchaseOrderRepository.existsByPoNumber("PO-2026-001")).thenReturn(false);
            when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(samplePO);
            when(lineItemRepository.saveAll(anyList())).thenReturn(List.of());
            doNothing().when(eventPublisher).publishPurchaseOrderCreated(any());

            PurchaseOrderResponse result = purchaseOrderService.createPurchaseOrder(req, "user-01");

            assertThat(result).isNotNull();
            assertThat(result.getPoNumber()).isEqualTo("PO-2026-001");
            assertThat(result.getStatus()).isEqualTo("CREATED");
            verify(purchaseOrderRepository, atLeast(1)).save(any(PurchaseOrder.class));
            verify(eventPublisher).publishPurchaseOrderCreated(any());
        }

        @Test
        @DisplayName("throws DuplicatePONumberException when PO number already exists")
        void createPurchaseOrder_duplicateNumber() {
            CreatePurchaseOrderRequest req = new CreatePurchaseOrderRequest();
            req.setPoNumber("PO-2026-001");
            req.setSupplierId("sup-001");
            req.setOrderDate(LocalDate.now());
            req.setDueDate(LocalDate.now().plusDays(7));
            req.setExpectedDeliveryDate(LocalDate.now().plusDays(7));
            req.setDeliveryWarehouseId("W01");
            req.setItems(List.of());

            when(purchaseOrderRepository.existsByPoNumber("PO-2026-001")).thenReturn(true);

            assertThatThrownBy(() -> purchaseOrderService.createPurchaseOrder(req, "user-01"))
                    .isInstanceOf(DuplicatePONumberException.class);

            verify(purchaseOrderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getPurchaseOrder")
    class GetPurchaseOrder {

        @Test
        @DisplayName("returns PO when found")
        void getPurchaseOrder_found() {
            when(purchaseOrderRepository.findById("po-001")).thenReturn(Optional.of(samplePO));

            PurchaseOrderResponse result = purchaseOrderService.getPurchaseOrder("po-001");

            assertThat(result.getPoId()).isEqualTo("po-001");
            assertThat(result.getPoNumber()).isEqualTo("PO-2026-001");
        }

        @Test
        @DisplayName("throws PurchaseOrderNotFoundException when not found")
        void getPurchaseOrder_notFound() {
            when(purchaseOrderRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> purchaseOrderService.getPurchaseOrder("missing"))
                    .isInstanceOf(PurchaseOrderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("confirmPurchaseOrder")
    class ConfirmPurchaseOrder {

        @Test
        @DisplayName("confirms a CREATED purchase order")
        void confirmPurchaseOrder_success() {
            when(purchaseOrderRepository.findById("po-001")).thenReturn(Optional.of(samplePO));
            when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(eventPublisher).publishPurchaseOrderConfirmed(any());

            ConfirmPurchaseOrderRequest req = new ConfirmPurchaseOrderRequest();
            req.setNotes("Confirmed with supplier");

            PurchaseOrderResponse result = purchaseOrderService.confirmPurchaseOrder("po-001", req, "user-01");

            assertThat(result.getStatus()).isEqualTo("CONFIRMED");
            assertThat(result.getConfirmationNumber()).isNotNull();
            verify(eventPublisher).publishPurchaseOrderConfirmed(any());
        }

        @Test
        @DisplayName("throws InvalidPurchaseOrderStateException when already confirmed")
        void confirmPurchaseOrder_alreadyConfirmed() {
            samplePO.setStatus("CONFIRMED");

            when(purchaseOrderRepository.findById("po-001")).thenReturn(Optional.of(samplePO));

            assertThatThrownBy(() -> purchaseOrderService.confirmPurchaseOrder("po-001", new ConfirmPurchaseOrderRequest(), "user-01"))
                    .isInstanceOf(InvalidPurchaseOrderStateException.class);
        }
    }

    @Nested
    @DisplayName("cancelPurchaseOrder")
    class CancelPurchaseOrder {

        @Test
        @DisplayName("cancels a CREATED purchase order")
        void cancelPurchaseOrder_success() {
            when(purchaseOrderRepository.findById("po-001")).thenReturn(Optional.of(samplePO));
            when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(eventPublisher).publishPurchaseOrderCancelled(any());

            CancelPurchaseOrderRequest req = new CancelPurchaseOrderRequest();
            req.setReason("Supplier unavailable");

            PurchaseOrderResponse result = purchaseOrderService.cancelPurchaseOrder("po-001", req, "user-01");

            assertThat(result.getStatus()).isEqualTo("CANCELLED");
            assertThat(result.getCancellationReason()).isEqualTo("Supplier unavailable");
            verify(eventPublisher).publishPurchaseOrderCancelled(any());
        }

        @Test
        @DisplayName("throws InvalidPurchaseOrderStateException when already received")
        void cancelPurchaseOrder_alreadyReceived() {
            samplePO.setStatus("RECEIVED");

            when(purchaseOrderRepository.findById("po-001")).thenReturn(Optional.of(samplePO));

            CancelPurchaseOrderRequest req = new CancelPurchaseOrderRequest();
            req.setReason("Too late");

            assertThatThrownBy(() -> purchaseOrderService.cancelPurchaseOrder("po-001", req, "user-01"))
                    .isInstanceOf(InvalidPurchaseOrderStateException.class);
        }
    }

    @Nested
    @DisplayName("listPurchaseOrders")
    class ListPurchaseOrders {

        @Test
        @DisplayName("returns paged list of purchase orders")
        void listPurchaseOrders_paged() {
            PageRequest pageable = PageRequest.of(0, 20);
            when(purchaseOrderRepository.findWithFilters(any(), any(), any(), any(), any(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(samplePO), pageable, 1));

            PagedResponse<PurchaseOrderSummaryResponse> result =
                    purchaseOrderService.listPurchaseOrders(null, null, null, null, null, pageable);

            assertThat(result.getData()).hasSize(1);
            assertThat(result.getMeta().getTotal()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("PurchaseOrder domain model")
    class PurchaseOrderDomainModel {

        @Test
        @DisplayName("status transitions are correct")
        void statusTransitions() {
            PurchaseOrder po = PurchaseOrder.builder()
                    .status("CREATED")
                    .createdBy("u").updatedBy("u")
                    .poNumber("X").supplierId("s")
                    .lineItems(new ArrayList<>())
                    .qualityIssues(new ArrayList<>())
                    .totalQuantity(0).deliveredQuantity(0)
                    .totalAmount(BigDecimal.ZERO).totalLineAmount(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO).taxAmount(BigDecimal.ZERO)
                    .paidAmount(BigDecimal.ZERO)
                    .deliveryStatus("NOT_RECEIVED").paymentStatus("UNPAID")
                    .build();

            assertThat(po.canBeConfirmed()).isTrue();
            assertThat(po.canBeCancelled()).isTrue();
            assertThat(po.canReceiveDelivery()).isFalse();

            po.confirm("CONF-001", "user-01");
            assertThat(po.getStatus()).isEqualTo("CONFIRMED");
            assertThat(po.canBeConfirmed()).isFalse();
            assertThat(po.canReceiveDelivery()).isTrue();

            po.cancel("Test reason", "user-01");
            assertThat(po.getStatus()).isEqualTo("CANCELLED");
            assertThat(po.canBeCancelled()).isFalse();
        }

        @Test
        @DisplayName("recalculates totals from line items")
        void recalculateTotals() {
            POLineItem lineItem = POLineItem.builder()
                    .quantityOrdered(10)
                    .unitPrice(BigDecimal.valueOf(100))
                    .discountPercentage(BigDecimal.ZERO)
                    .lineTotal(BigDecimal.valueOf(1000))
                    .build();

            PurchaseOrder po = PurchaseOrder.builder()
                    .status("CREATED")
                    .createdBy("u").updatedBy("u")
                    .poNumber("X").supplierId("s")
                    .lineItems(new ArrayList<>(List.of(lineItem)))
                    .qualityIssues(new ArrayList<>())
                    .totalQuantity(0).deliveredQuantity(0)
                    .totalAmount(BigDecimal.ZERO).totalLineAmount(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO).taxAmount(BigDecimal.ZERO)
                    .paidAmount(BigDecimal.ZERO)
                    .deliveryStatus("NOT_RECEIVED").paymentStatus("UNPAID")
                    .build();

            po.recalculateTotals();

            assertThat(po.getTotalQuantity()).isEqualTo(10);
            assertThat(po.getTotalLineAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        }
    }
}
