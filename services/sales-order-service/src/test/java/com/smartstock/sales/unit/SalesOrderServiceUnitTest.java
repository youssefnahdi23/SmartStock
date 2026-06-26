package com.smartstock.sales.unit;

import com.smartstock.sales.api.dto.request.CancelSalesOrderRequest;
import com.smartstock.sales.api.dto.request.ConfirmSalesOrderRequest;
import com.smartstock.sales.api.dto.request.CreateSalesOrderRequest;
import com.smartstock.sales.api.dto.request.CreateShipmentRequest;
import com.smartstock.sales.api.dto.request.PickSalesOrderRequest;
import com.smartstock.sales.api.dto.request.RegisterDeliveryRequest;
import com.smartstock.sales.api.dto.response.PagedResponse;
import com.smartstock.sales.api.dto.response.PickingResponse;
import com.smartstock.sales.api.dto.response.SalesOrderResponse;
import com.smartstock.sales.api.dto.response.SalesOrderSummaryResponse;
import com.smartstock.sales.api.dto.response.ShipmentResponse;
import com.smartstock.sales.domain.model.OrderShipment;
import com.smartstock.sales.domain.model.SOLineItem;
import com.smartstock.sales.domain.model.SalesOrder;
import com.smartstock.sales.domain.repository.OrderReturnRepository;
import com.smartstock.sales.domain.repository.OrderShipmentRepository;
import com.smartstock.sales.domain.repository.SOLineItemRepository;
import com.smartstock.sales.domain.repository.SalesOrderRepository;
import com.smartstock.sales.exception.DuplicateSONumberException;
import com.smartstock.sales.exception.InvalidSalesOrderStateException;
import com.smartstock.sales.exception.SalesOrderNotFoundException;
import com.smartstock.sales.service.SalesOrderEventPublisher;
import com.smartstock.sales.service.SalesOrderService;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesOrderService Unit Tests")
class SalesOrderServiceUnitTest {

    @Mock private SalesOrderRepository salesOrderRepository;
    @Mock private SOLineItemRepository lineItemRepository;
    @Mock private OrderShipmentRepository shipmentRepository;
    @Mock private OrderReturnRepository returnRepository;
    @Mock private SalesOrderEventPublisher eventPublisher;

    @InjectMocks
    private SalesOrderService salesOrderService;

    private SalesOrder sampleSO;

    @BeforeEach
    void setUp() {
        sampleSO = SalesOrder.builder()
                .id("so-001")
                .soNumber("SO-2026-001")
                .customerId("cust-001")
                .customerName("ABC Corporation")
                .status("CREATED")
                .fulfillmentStatus("PENDING")
                .paymentStatus("UNPAID")
                .orderDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(10))
                .totalQuantity(100)
                .pickedQuantity(0)
                .shippedQuantity(0)
                .totalAmount(BigDecimal.valueOf(9999))
                .totalLineAmount(BigDecimal.valueOf(9999))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .lineItems(new ArrayList<>())
                .shipments(new ArrayList<>())
                .createdBy("user-01")
                .updatedBy("user-01")
                .build();
    }

    @Nested
    @DisplayName("createSalesOrder")
    class CreateSalesOrder {

        @Test
        @DisplayName("creates SO when SO number is unique")
        void createSalesOrder_success() {
            CreateSalesOrderRequest req = new CreateSalesOrderRequest();
            req.setSoNumber("SO-2026-001");
            req.setCustomerId("cust-001");
            req.setOrderDate(LocalDate.now());
            req.setDueDate(LocalDate.now().plusDays(10));

            CreateSalesOrderRequest.CreateLineItemRequest item = new CreateSalesOrderRequest.CreateLineItemRequest();
            item.setProductId("prod-001");
            item.setQuantity(100);
            item.setUnitPrice(BigDecimal.valueOf(99.99));
            req.setItems(List.of(item));

            when(salesOrderRepository.existsBySoNumber("SO-2026-001")).thenReturn(false);
            when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(sampleSO);
            when(lineItemRepository.saveAll(anyList())).thenReturn(List.of());
            doNothing().when(eventPublisher).publishSalesOrderCreated(any());

            SalesOrderResponse result = salesOrderService.createSalesOrder(req, "user-01");

            assertThat(result).isNotNull();
            assertThat(result.getSoNumber()).isEqualTo("SO-2026-001");
            assertThat(result.getStatus()).isEqualTo("CREATED");
            verify(salesOrderRepository, atLeast(1)).save(any(SalesOrder.class));
            verify(eventPublisher).publishSalesOrderCreated(any());
        }

        @Test
        @DisplayName("throws DuplicateSONumberException when SO number already exists")
        void createSalesOrder_duplicateNumber() {
            CreateSalesOrderRequest req = new CreateSalesOrderRequest();
            req.setSoNumber("SO-2026-001");
            req.setCustomerId("cust-001");
            req.setItems(List.of());

            when(salesOrderRepository.existsBySoNumber("SO-2026-001")).thenReturn(true);

            assertThatThrownBy(() -> salesOrderService.createSalesOrder(req, "user-01"))
                    .isInstanceOf(DuplicateSONumberException.class);

            verify(salesOrderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getSalesOrder")
    class GetSalesOrder {

        @Test
        @DisplayName("returns SO when found")
        void getSalesOrder_found() {
            when(salesOrderRepository.findById("so-001")).thenReturn(Optional.of(sampleSO));

            SalesOrderResponse result = salesOrderService.getSalesOrder("so-001");

            assertThat(result.getSoId()).isEqualTo("so-001");
            assertThat(result.getSoNumber()).isEqualTo("SO-2026-001");
        }

        @Test
        @DisplayName("throws SalesOrderNotFoundException when not found")
        void getSalesOrder_notFound() {
            when(salesOrderRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> salesOrderService.getSalesOrder("missing"))
                    .isInstanceOf(SalesOrderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("confirmSalesOrder")
    class ConfirmSalesOrder {

        @Test
        @DisplayName("confirms a CREATED sales order")
        void confirmSalesOrder_success() {
            when(salesOrderRepository.findById("so-001")).thenReturn(Optional.of(sampleSO));
            when(salesOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(eventPublisher).publishSalesOrderConfirmed(any());

            ConfirmSalesOrderRequest req = new ConfirmSalesOrderRequest();
            req.setWarehouseId("W01");
            req.setNotes("Confirmed");

            SalesOrderResponse result = salesOrderService.confirmSalesOrder("so-001", req, "user-01");

            assertThat(result.getStatus()).isEqualTo("CONFIRMED");
            assertThat(result.getPickingWarehouseId()).isEqualTo("W01");
            verify(eventPublisher).publishSalesOrderConfirmed(any());
        }

        @Test
        @DisplayName("throws InvalidSalesOrderStateException when already confirmed")
        void confirmSalesOrder_alreadyConfirmed() {
            sampleSO.setStatus("CONFIRMED");
            when(salesOrderRepository.findById("so-001")).thenReturn(Optional.of(sampleSO));

            assertThatThrownBy(() -> salesOrderService.confirmSalesOrder("so-001", new ConfirmSalesOrderRequest(), "user-01"))
                    .isInstanceOf(InvalidSalesOrderStateException.class);
        }
    }

    @Nested
    @DisplayName("cancelSalesOrder")
    class CancelSalesOrder {

        @Test
        @DisplayName("cancels a CREATED sales order")
        void cancelSalesOrder_success() {
            when(salesOrderRepository.findById("so-001")).thenReturn(Optional.of(sampleSO));
            when(salesOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(eventPublisher).publishSalesOrderCancelled(any());

            CancelSalesOrderRequest req = new CancelSalesOrderRequest();
            req.setReason("Customer request");

            SalesOrderResponse result = salesOrderService.cancelSalesOrder("so-001", req, "user-01");

            assertThat(result.getStatus()).isEqualTo("CANCELLED");
            assertThat(result.getCancellationReason()).isEqualTo("Customer request");
            verify(eventPublisher).publishSalesOrderCancelled(any());
        }

        @Test
        @DisplayName("throws InvalidSalesOrderStateException when already shipped")
        void cancelSalesOrder_alreadyShipped() {
            sampleSO.setStatus("SHIPPED");
            when(salesOrderRepository.findById("so-001")).thenReturn(Optional.of(sampleSO));

            CancelSalesOrderRequest req = new CancelSalesOrderRequest();
            req.setReason("Too late");

            assertThatThrownBy(() -> salesOrderService.cancelSalesOrder("so-001", req, "user-01"))
                    .isInstanceOf(InvalidSalesOrderStateException.class);
        }
    }

    @Nested
    @DisplayName("listSalesOrders")
    class ListSalesOrders {

        @Test
        @DisplayName("returns paged list of sales orders")
        void listSalesOrders_paged() {
            PageRequest pageable = PageRequest.of(0, 20);
            when(salesOrderRepository.findWithFilters(any(), any(), any(), any(), any(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(sampleSO), pageable, 1));

            PagedResponse<SalesOrderSummaryResponse> result =
                    salesOrderService.listSalesOrders(null, null, null, null, null, pageable);

            assertThat(result.getData()).hasSize(1);
            assertThat(result.getMeta().getTotal()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("pickSalesOrder")
    class PickSalesOrder {

        @Test
        @DisplayName("picks items from a CONFIRMED order and transitions to PICKING_COMPLETE when all picked")
        void pickSalesOrder_success_allPicked() {
            sampleSO.setStatus("CONFIRMED");
            sampleSO.setTotalQuantity(50);
            sampleSO.setPickedQuantity(0);

            SOLineItem lineItem = SOLineItem.builder()
                    .id("li-001").productId("prod-001")
                    .quantityOrdered(50).quantityPicked(0).quantityShipped(0)
                    .unitPrice(BigDecimal.valueOf(100)).discountPercentage(BigDecimal.ZERO)
                    .lineTotal(BigDecimal.valueOf(5000)).status("PENDING")
                    .build();

            when(salesOrderRepository.findById("so-001")).thenReturn(Optional.of(sampleSO));
            when(lineItemRepository.findByIdAndSalesOrderId("li-001", "so-001")).thenReturn(Optional.of(lineItem));
            when(lineItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(salesOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(eventPublisher).publishOrderPickingStarted(any());
            doNothing().when(eventPublisher).publishOrderPickingCompleted(any());

            PickSalesOrderRequest req = new PickSalesOrderRequest();
            PickSalesOrderRequest.PickItemRequest pickItem = new PickSalesOrderRequest.PickItemRequest();
            pickItem.setLineId("li-001");
            pickItem.setQuantity(50);
            req.setItems(List.of(pickItem));

            PickingResponse result = salesOrderService.pickSalesOrder("so-001", req, "user-01");

            assertThat(result.getSoId()).isEqualTo("so-001");
            assertThat(result.getPickedQuantity()).isEqualTo(50);
            assertThat(result.getFulfillmentStatus()).isEqualTo("PICKING_COMPLETE");
            assertThat(result.getItems()).hasSize(1);
            verify(eventPublisher).publishOrderPickingStarted(any());
            verify(eventPublisher).publishOrderPickingCompleted(any());
        }

        @Test
        @DisplayName("throws InvalidSalesOrderStateException when order is not in CONFIRMED or PICKING state")
        void pickSalesOrder_invalidState() {
            when(salesOrderRepository.findById("so-001")).thenReturn(Optional.of(sampleSO));

            PickSalesOrderRequest req = new PickSalesOrderRequest();
            req.setItems(List.of());

            assertThatThrownBy(() -> salesOrderService.pickSalesOrder("so-001", req, "user-01"))
                    .isInstanceOf(InvalidSalesOrderStateException.class);

            verify(eventPublisher, never()).publishOrderPickingStarted(any());
        }
    }

    @Nested
    @DisplayName("createShipment")
    class CreateShipmentTests {

        @Test
        @DisplayName("creates a shipment for a CONFIRMED order and publishes ShipmentCreatedEvent")
        void createShipment_success() {
            sampleSO.setStatus("CONFIRMED");
            sampleSO.setTotalQuantity(20);
            sampleSO.setShippedQuantity(0);

            SOLineItem lineItem = SOLineItem.builder()
                    .id("li-001").productId("prod-001")
                    .quantityOrdered(20).quantityPicked(20).quantityShipped(0)
                    .unitPrice(BigDecimal.valueOf(50)).discountPercentage(BigDecimal.ZERO)
                    .lineTotal(BigDecimal.valueOf(1000)).status("PICKED")
                    .build();

            OrderShipment savedShipment = OrderShipment.builder()
                    .id("ship-001").salesOrder(sampleSO)
                    .shipmentNumber("SHIP-ABCD1234").carrierName("FedEx")
                    .trackingNumber("1Z-TEST").shippedQuantity(20)
                    .status("SHIPPED").shippedAt(Instant.now())
                    .build();

            when(salesOrderRepository.findById("so-001")).thenReturn(Optional.of(sampleSO));
            when(lineItemRepository.findByIdAndSalesOrderId("li-001", "so-001")).thenReturn(Optional.of(lineItem));
            when(lineItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(shipmentRepository.save(any())).thenReturn(savedShipment);
            when(salesOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(eventPublisher).publishShipmentCreated(any());

            CreateShipmentRequest req = new CreateShipmentRequest();
            req.setCarrierName("FedEx");
            req.setTrackingNumber("1Z-TEST");
            req.setEstimatedDeliveryDate(LocalDate.now().plusDays(3));
            CreateShipmentRequest.ShipmentItemRequest item = new CreateShipmentRequest.ShipmentItemRequest();
            item.setLineId("li-001");
            item.setQuantity(20);
            req.setItems(List.of(item));

            ShipmentResponse result = salesOrderService.createShipment("so-001", req, "user-01");

            assertThat(result.getShipmentId()).isEqualTo("ship-001");
            assertThat(result.getCarrierName()).isEqualTo("FedEx");
            assertThat(result.getShippedQuantity()).isEqualTo(20);
            verify(eventPublisher).publishShipmentCreated(any());
        }

        @Test
        @DisplayName("throws InvalidSalesOrderStateException when order is CANCELLED")
        void createShipment_invalidState() {
            sampleSO.setStatus("CANCELLED");
            when(salesOrderRepository.findById("so-001")).thenReturn(Optional.of(sampleSO));

            assertThatThrownBy(() -> salesOrderService.createShipment("so-001", new CreateShipmentRequest(), "user-01"))
                    .isInstanceOf(InvalidSalesOrderStateException.class);

            verify(eventPublisher, never()).publishShipmentCreated(any());
        }
    }

    @Nested
    @DisplayName("registerDelivery")
    class RegisterDelivery {

        @Test
        @DisplayName("marks shipment as DELIVERED and transitions order to DELIVERED when all shipments done")
        void registerDelivery_success() {
            sampleSO.setStatus("SHIPPED");
            sampleSO.setFulfillmentStatus("SHIPPED");

            OrderShipment shipment = OrderShipment.builder()
                    .id("ship-001").salesOrder(sampleSO)
                    .shipmentNumber("SHIP-XYZ").carrierName("UPS")
                    .shippedQuantity(20).status("SHIPPED").shippedAt(Instant.now())
                    .build();

            when(salesOrderRepository.findById("so-001")).thenReturn(Optional.of(sampleSO));
            when(shipmentRepository.findByIdAndSalesOrderId("ship-001", "so-001")).thenReturn(Optional.of(shipment));
            when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(salesOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(eventPublisher).publishDeliveryCompleted(any());

            RegisterDeliveryRequest req = new RegisterDeliveryRequest();
            req.setDeliveryDate(LocalDate.now());
            req.setSignedBy("John Doe");

            ShipmentResponse result = salesOrderService.registerDelivery("so-001", "ship-001", req, "user-01");

            assertThat(result.getStatus()).isEqualTo("DELIVERED");
            assertThat(result.getSignedBy()).isEqualTo("John Doe");
            assertThat(sampleSO.getStatus()).isEqualTo("DELIVERED");
            verify(eventPublisher).publishDeliveryCompleted(any());
        }
    }

    @Nested
    @DisplayName("SalesOrder domain model")
    class SalesOrderDomainModel {

        @Test
        @DisplayName("status transitions are correct")
        void statusTransitions() {
            SalesOrder so = SalesOrder.builder()
                    .status("CREATED")
                    .soNumber("SO-TEST").customerId("c1")
                    .createdBy("u").updatedBy("u")
                    .lineItems(new ArrayList<>())
                    .shipments(new ArrayList<>())
                    .totalQuantity(0).pickedQuantity(0).shippedQuantity(0)
                    .totalAmount(BigDecimal.ZERO).totalLineAmount(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO).taxAmount(BigDecimal.ZERO)
                    .paidAmount(BigDecimal.ZERO)
                    .fulfillmentStatus("PENDING").paymentStatus("UNPAID")
                    .build();

            assertThat(so.canBeConfirmed()).isTrue();
            assertThat(so.canBeCancelled()).isTrue();
            assertThat(so.canBePicked()).isFalse();
            assertThat(so.canBeShipped()).isFalse();

            so.confirm("W01", "user-01");
            assertThat(so.getStatus()).isEqualTo("CONFIRMED");
            assertThat(so.getPickingWarehouseId()).isEqualTo("W01");
            assertThat(so.canBeConfirmed()).isFalse();
            assertThat(so.canBePicked()).isTrue();
            assertThat(so.canBeShipped()).isTrue();

            so.cancel("Test", "user-01");
            assertThat(so.getStatus()).isEqualTo("CANCELLED");
            assertThat(so.canBeCancelled()).isFalse();
        }

        @Test
        @DisplayName("recalculates totals from line items")
        void recalculateTotals() {
            SOLineItem lineItem = SOLineItem.builder()
                    .quantityOrdered(10)
                    .unitPrice(BigDecimal.valueOf(100))
                    .discountPercentage(BigDecimal.ZERO)
                    .lineTotal(BigDecimal.valueOf(1000))
                    .quantityPicked(0).quantityShipped(0)
                    .status("PENDING")
                    .build();

            SalesOrder so = SalesOrder.builder()
                    .status("CREATED")
                    .soNumber("SO-TEST").customerId("c1")
                    .createdBy("u").updatedBy("u")
                    .lineItems(new ArrayList<>(List.of(lineItem)))
                    .shipments(new ArrayList<>())
                    .totalQuantity(0).pickedQuantity(0).shippedQuantity(0)
                    .totalAmount(BigDecimal.ZERO).totalLineAmount(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO).taxAmount(BigDecimal.ZERO)
                    .paidAmount(BigDecimal.ZERO)
                    .fulfillmentStatus("PENDING").paymentStatus("UNPAID")
                    .build();

            so.recalculateTotals();

            assertThat(so.getTotalQuantity()).isEqualTo(10);
            assertThat(so.getTotalLineAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
            assertThat(so.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        }
    }
}
