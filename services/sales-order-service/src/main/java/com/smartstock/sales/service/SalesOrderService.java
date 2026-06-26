package com.smartstock.sales.service;

import com.smartstock.sales.api.dto.request.*;
import com.smartstock.sales.api.dto.response.*;
import com.smartstock.sales.domain.event.*;
import com.smartstock.sales.domain.model.OrderReturn;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final SOLineItemRepository lineItemRepository;
    private final OrderShipmentRepository shipmentRepository;
    private final OrderReturnRepository returnRepository;
    private final SalesOrderEventPublisher eventPublisher;

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_sales-order:create')")
    public SalesOrderResponse createSalesOrder(CreateSalesOrderRequest req, String actorId) {
        if (salesOrderRepository.existsBySoNumber(req.getSoNumber())) {
            throw new DuplicateSONumberException(req.getSoNumber());
        }

        SalesOrder so = SalesOrder.builder()
                .soNumber(req.getSoNumber())
                .customerId(req.getCustomerId())
                .orderDate(req.getOrderDate() != null ? req.getOrderDate() : LocalDate.now())
                .dueDate(req.getDueDate())
                .shippingAddress(req.getShippingAddress())
                .shippingMethod(req.getShippingMethod())
                .paymentTerms(req.getPaymentTerms())
                .notes(req.getNotes())
                .status("CREATED")
                .fulfillmentStatus("PENDING")
                .paymentStatus("UNPAID")
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();

        so = salesOrderRepository.save(so);

        List<SOLineItem> lineItems = buildLineItems(so, req.getItems());
        lineItemRepository.saveAll(lineItems);
        so.setLineItems(lineItems);
        so.recalculateTotals();
        so = salesOrderRepository.save(so);

        log.info("SalesOrder created: soNumber={} by={}", so.getSoNumber(), actorId);

        List<SalesOrderCreatedEvent.OrderItemPayload> itemPayloads = lineItems.stream()
                .map(li -> new SalesOrderCreatedEvent.OrderItemPayload(
                        li.getProductId(), li.getQuantityOrdered(), li.getUnitPrice()))
                .toList();

        eventPublisher.publishSalesOrderCreated(new SalesOrderCreatedEvent(
                so.getId(), so.getSoNumber(), so.getCustomerId(), so.getCustomerName(),
                so.getOrderDate(), so.getDueDate(),
                so.getTotalAmount(), so.getTotalQuantity(), itemPayloads, actorId));

        return toResponse(so);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_sales-order:read')")
    public SalesOrderResponse getSalesOrder(String soId) {
        return toResponse(findById(soId));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_sales-order:read')")
    public PagedResponse<SalesOrderSummaryResponse> listSalesOrders(
            String status, String customerId, String fulfillmentStatus,
            LocalDate fromDate, LocalDate toDate, Pageable pageable) {

        Page<SalesOrder> page = salesOrderRepository.findWithFilters(
                status, customerId, fulfillmentStatus, fromDate, toDate, pageable);

        List<SalesOrderSummaryResponse> items = page.getContent().stream().map(this::toSummary).toList();
        return PagedResponse.<SalesOrderSummaryResponse>builder()
                .data(items)
                .meta(PagedResponse.Meta.builder()
                        .timestamp(Instant.now())
                        .page(pageable.getPageNumber())
                        .size(pageable.getPageSize())
                        .total(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_sales-order:confirm')")
    public SalesOrderResponse confirmSalesOrder(String soId, ConfirmSalesOrderRequest req, String actorId) {
        SalesOrder so = findById(soId);
        if (!so.canBeConfirmed()) {
            throw new InvalidSalesOrderStateException("confirm", so.getStatus());
        }

        so.confirm(req.getWarehouseId(), actorId);
        if (req.getNotes() != null) so.setNotes(req.getNotes());
        so = salesOrderRepository.save(so);

        log.info("SalesOrder confirmed: soId={} warehouseId={} by={}", soId, req.getWarehouseId(), actorId);

        eventPublisher.publishSalesOrderConfirmed(new SalesOrderConfirmedEvent(
                so.getId(), so.getSoNumber(), so.getCustomerId(),
                so.getPickingWarehouseId(), so.getConfirmationDate(), actorId));

        return toResponse(so);
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_sales-order:pick')")
    public PickingResponse pickSalesOrder(String soId, PickSalesOrderRequest req, String actorId) {
        SalesOrder so = findById(soId);
        if (!so.canBePicked()) {
            throw new InvalidSalesOrderStateException("pick", so.getStatus());
        }

        boolean pickingStartedNow = "CONFIRMED".equals(so.getStatus());
        so.setStatus("PICKING");
        so.setFulfillmentStatus("PICKING");
        so.setUpdatedBy(actorId);

        List<PickingResponse.PickedItemResult> pickedItems = new ArrayList<>();
        int totalPicked = 0;

        for (PickSalesOrderRequest.PickItemRequest pickItem : req.getItems()) {
            SOLineItem lineItem = lineItemRepository.findByIdAndSalesOrderId(pickItem.getLineId(), soId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Line item " + pickItem.getLineId() + " does not belong to sales order " + soId));

            int picked = pickItem.getQuantity();
            lineItem.setQuantityPicked(lineItem.getQuantityPicked() + picked);
            if (lineItem.getQuantityPicked() >= lineItem.getQuantityOrdered()) {
                lineItem.setStatus("PICKED");
            }
            if (pickItem.getLocation() != null) {
                lineItem.setBinLocation(pickItem.getLocation());
            }
            lineItemRepository.save(lineItem);
            totalPicked += picked;

            pickedItems.add(PickingResponse.PickedItemResult.builder()
                    .lineId(pickItem.getLineId())
                    .productId(lineItem.getProductId())
                    .pickedQuantity(picked)
                    .location(pickItem.getLocation())
                    .build());
        }

        int newPickedTotal = so.getPickedQuantity() + totalPicked;
        so.setPickedQuantity(newPickedTotal);

        boolean allPicked = newPickedTotal >= so.getTotalQuantity();
        Instant completedAt = null;
        if (allPicked) {
            so.setFulfillmentStatus("PICKING_COMPLETE");
            completedAt = Instant.now();
        }

        so = salesOrderRepository.save(so);

        if (pickingStartedNow) {
            eventPublisher.publishOrderPickingStarted(new OrderPickingStartedEvent(
                    so.getId(), so.getSoNumber(), so.getCustomerId(),
                    so.getPickingWarehouseId(), actorId));
        }

        if (allPicked) {
            eventPublisher.publishOrderPickingCompleted(new OrderPickingCompletedEvent(
                    so.getId(), so.getSoNumber(), so.getCustomerId(),
                    so.getPickingWarehouseId(), newPickedTotal, completedAt, actorId));
        }

        log.info("SalesOrder picking: soId={} picked={} total={} by={}", soId, totalPicked, newPickedTotal, actorId);

        return PickingResponse.builder()
                .soId(soId)
                .fulfillmentStatus(so.getFulfillmentStatus())
                .pickedQuantity(newPickedTotal)
                .pickingCompletedAt(completedAt)
                .items(pickedItems)
                .build();
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_sales-order:ship')")
    public ShipmentResponse createShipment(String soId, CreateShipmentRequest req, String actorId) {
        SalesOrder so = findById(soId);
        if (!so.canBeShipped()) {
            throw new InvalidSalesOrderStateException("create-shipment", so.getStatus());
        }

        String shipmentNumber = "SHIP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Instant shippedAt = Instant.now();

        int totalShipped = req.getItems().stream().mapToInt(CreateShipmentRequest.ShipmentItemRequest::getQuantity).sum();

        for (CreateShipmentRequest.ShipmentItemRequest item : req.getItems()) {
            SOLineItem lineItem = lineItemRepository.findByIdAndSalesOrderId(item.getLineId(), soId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Line item " + item.getLineId() + " does not belong to sales order " + soId));
            lineItem.setQuantityShipped(lineItem.getQuantityShipped() + item.getQuantity());
            if (lineItem.getQuantityShipped() >= lineItem.getQuantityOrdered()) {
                lineItem.setStatus("SHIPPED");
            }
            lineItemRepository.save(lineItem);
        }

        OrderShipment shipment = OrderShipment.builder()
                .salesOrder(so)
                .shipmentNumber(shipmentNumber)
                .carrierName(req.getCarrierName())
                .trackingNumber(req.getTrackingNumber())
                .shippingMethod(req.getShippingMethod())
                .shippedQuantity(totalShipped)
                .shipDate(LocalDate.now())
                .estimatedDeliveryDate(req.getEstimatedDeliveryDate())
                .status("SHIPPED")
                .shippedBy(actorId)
                .deliveryNotes(req.getNotes())
                .shippedAt(shippedAt)
                .build();
        shipment = shipmentRepository.save(shipment);

        so.setShippedQuantity(so.getShippedQuantity() + totalShipped);
        boolean allShipped = so.getShippedQuantity() >= so.getTotalQuantity();
        if (allShipped) {
            so.setStatus("SHIPPED");
            so.setFulfillmentStatus("SHIPPED");
        }
        so.setUpdatedBy(actorId);
        salesOrderRepository.save(so);

        log.info("Shipment created: soId={} shipmentId={} shipped={} by={}", soId, shipment.getId(), totalShipped, actorId);

        eventPublisher.publishShipmentCreated(new ShipmentCreatedEvent(
                so.getId(), so.getSoNumber(), so.getCustomerId(),
                shipment.getId(), shipmentNumber,
                req.getCarrierName(), req.getTrackingNumber(),
                req.getEstimatedDeliveryDate(), totalShipped, shippedAt, actorId));

        return toShipmentResponse(shipment, soId);
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_sales-order:deliver')")
    public ShipmentResponse registerDelivery(String soId, String shipmentId, RegisterDeliveryRequest req, String actorId) {
        SalesOrder so = findById(soId);
        OrderShipment shipment = shipmentRepository.findByIdAndSalesOrderId(shipmentId, soId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Shipment " + shipmentId + " does not belong to sales order " + soId));

        Instant deliveredAt = Instant.now();
        shipment.setStatus("DELIVERED");
        shipment.setActualDeliveryDate(req.getDeliveryDate());
        shipment.setSignedBy(req.getSignedBy());
        shipment.setDeliveryNotes(req.getDeliveryNotes());
        shipment.setDeliveredAt(deliveredAt);
        shipment = shipmentRepository.save(shipment);

        boolean allDelivered = so.getShipments().stream()
                .allMatch(s -> "DELIVERED".equals(s.getStatus()) || s.getId().equals(shipmentId));
        if (allDelivered) {
            so.setStatus("DELIVERED");
            so.setFulfillmentStatus("DELIVERED");
            so.setUpdatedBy(actorId);
            salesOrderRepository.save(so);
        }

        log.info("Delivery registered: soId={} shipmentId={} by={}", soId, shipmentId, actorId);

        eventPublisher.publishDeliveryCompleted(new DeliveryCompletedEvent(
                so.getId(), so.getSoNumber(), so.getCustomerId(),
                shipmentId, req.getDeliveryDate(), req.getSignedBy(), deliveredAt));

        return toShipmentResponse(shipment, soId);
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_sales-order:write')")
    public SalesOrderResponse cancelSalesOrder(String soId, CancelSalesOrderRequest req, String actorId) {
        SalesOrder so = findById(soId);
        if (!so.canBeCancelled()) {
            throw new InvalidSalesOrderStateException("cancel", so.getStatus());
        }

        so.cancel(req.getReason(), actorId);
        so = salesOrderRepository.save(so);

        log.info("SalesOrder cancelled: soId={} reason={} by={}", soId, req.getReason(), actorId);

        eventPublisher.publishSalesOrderCancelled(new SalesOrderCancelledEvent(
                so.getId(), so.getSoNumber(), so.getCustomerId(),
                req.getReason(), so.getCancelledAt(), actorId));

        return toResponse(so);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_sales-order:read')")
    public List<SalesOrderSummaryResponse> getOrdersByCustomer(String customerId) {
        return salesOrderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_sales-order:read')")
    public List<SalesOrderSummaryResponse> getPendingDelivery() {
        return salesOrderRepository.findPendingDelivery()
                .stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_sales-order:read')")
    public List<ShipmentResponse> getShipments(String soId) {
        findById(soId);
        return shipmentRepository.findBySalesOrderId(soId)
                .stream().map(s -> toShipmentResponse(s, soId)).toList();
    }

    public SalesOrder findById(String soId) {
        return salesOrderRepository.findById(soId)
                .orElseThrow(() -> new SalesOrderNotFoundException(soId));
    }

    private List<SOLineItem> buildLineItems(SalesOrder so, List<CreateSalesOrderRequest.CreateLineItemRequest> items) {
        List<SOLineItem> lineItems = new ArrayList<>();
        for (CreateSalesOrderRequest.CreateLineItemRequest item : items) {
            SOLineItem lineItem = SOLineItem.builder()
                    .salesOrder(so)
                    .productId(item.getProductId())
                    .quantityOrdered(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .discountPercentage(item.getDiscount() != null ? item.getDiscount() : BigDecimal.ZERO)
                    .notes(item.getNotes())
                    .status("PENDING")
                    .build();
            lineItem.calculateLineTotal();
            lineItems.add(lineItem);
        }
        return lineItems;
    }

    private SalesOrderResponse toResponse(SalesOrder so) {
        List<SOLineItemResponse> lineItemResponses = so.getLineItems() == null ? List.of() :
                so.getLineItems().stream().map(this::toLineItemResponse).toList();

        List<ShipmentResponse> shipmentResponses = so.getShipments() == null ? List.of() :
                so.getShipments().stream().map(s -> toShipmentResponse(s, so.getId())).toList();

        return SalesOrderResponse.builder()
                .soId(so.getId())
                .soNumber(so.getSoNumber())
                .customerId(so.getCustomerId())
                .customerName(so.getCustomerName())
                .status(so.getStatus())
                .fulfillmentStatus(so.getFulfillmentStatus())
                .paymentStatus(so.getPaymentStatus())
                .orderDate(so.getOrderDate())
                .dueDate(so.getDueDate())
                .pickingWarehouseId(so.getPickingWarehouseId())
                .shippingAddress(so.getShippingAddress())
                .shippingMethod(so.getShippingMethod())
                .paymentTerms(so.getPaymentTerms())
                .totalQuantity(so.getTotalQuantity())
                .pickedQuantity(so.getPickedQuantity())
                .shippedQuantity(so.getShippedQuantity())
                .totalLineAmount(so.getTotalLineAmount())
                .discountAmount(so.getDiscountAmount())
                .taxAmount(so.getTaxAmount())
                .totalAmount(so.getTotalAmount())
                .paidAmount(so.getPaidAmount())
                .confirmationDate(so.getConfirmationDate())
                .cancelledAt(so.getCancelledAt())
                .cancellationReason(so.getCancellationReason())
                .notes(so.getNotes())
                .items(lineItemResponses)
                .shipments(shipmentResponses)
                .createdBy(so.getCreatedBy())
                .createdAt(so.getCreatedAt())
                .updatedAt(so.getUpdatedAt())
                .build();
    }

    private SalesOrderSummaryResponse toSummary(SalesOrder so) {
        return SalesOrderSummaryResponse.builder()
                .soId(so.getId())
                .soNumber(so.getSoNumber())
                .customerId(so.getCustomerId())
                .customerName(so.getCustomerName())
                .status(so.getStatus())
                .fulfillmentStatus(so.getFulfillmentStatus())
                .paymentStatus(so.getPaymentStatus())
                .orderDate(so.getOrderDate())
                .dueDate(so.getDueDate())
                .totalAmount(so.getTotalAmount())
                .createdAt(so.getCreatedAt())
                .build();
    }

    private SOLineItemResponse toLineItemResponse(SOLineItem li) {
        return SOLineItemResponse.builder()
                .lineId(li.getId())
                .productId(li.getProductId())
                .productName(li.getProductName())
                .quantity(li.getQuantityOrdered())
                .pickedQuantity(li.getQuantityPicked())
                .shippedQuantity(li.getQuantityShipped())
                .unitPrice(li.getUnitPrice())
                .discountPercentage(li.getDiscountPercentage())
                .lineTotal(li.getLineTotal())
                .status(li.getStatus())
                .binLocation(li.getBinLocation())
                .notes(li.getNotes())
                .createdAt(li.getCreatedAt())
                .updatedAt(li.getUpdatedAt())
                .build();
    }

    private ShipmentResponse toShipmentResponse(OrderShipment s, String soId) {
        return ShipmentResponse.builder()
                .shipmentId(s.getId())
                .soId(soId)
                .shipmentNumber(s.getShipmentNumber())
                .carrierName(s.getCarrierName())
                .trackingNumber(s.getTrackingNumber())
                .shippingMethod(s.getShippingMethod())
                .shippedQuantity(s.getShippedQuantity())
                .shipDate(s.getShipDate())
                .estimatedDeliveryDate(s.getEstimatedDeliveryDate())
                .actualDeliveryDate(s.getActualDeliveryDate())
                .status(s.getStatus())
                .signedBy(s.getSignedBy())
                .deliveryNotes(s.getDeliveryNotes())
                .shippedAt(s.getShippedAt())
                .deliveredAt(s.getDeliveredAt())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
