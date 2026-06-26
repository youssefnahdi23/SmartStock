package com.smartstock.purchase.service;

import com.smartstock.purchase.api.dto.request.*;
import com.smartstock.purchase.api.dto.response.*;
import com.smartstock.purchase.domain.event.*;
import com.smartstock.purchase.domain.model.DeliveryTracking;
import com.smartstock.purchase.domain.model.POLineItem;
import com.smartstock.purchase.domain.model.PurchaseOrder;
import com.smartstock.purchase.domain.model.QualityIssue;
import com.smartstock.purchase.domain.repository.DeliveryTrackingRepository;
import com.smartstock.purchase.domain.repository.POLineItemRepository;
import com.smartstock.purchase.domain.repository.PurchaseOrderRepository;
import com.smartstock.purchase.domain.repository.QualityIssueRepository;
import com.smartstock.purchase.exception.DuplicatePONumberException;
import com.smartstock.purchase.exception.InvalidPurchaseOrderStateException;
import com.smartstock.purchase.exception.PurchaseOrderNotFoundException;
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
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final POLineItemRepository lineItemRepository;
    private final DeliveryTrackingRepository deliveryTrackingRepository;
    private final QualityIssueRepository qualityIssueRepository;
    private final PurchaseOrderEventPublisher eventPublisher;

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_purchase-order:create')")
    public PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderRequest req, String actorId) {
        if (purchaseOrderRepository.existsByPoNumber(req.getPoNumber())) {
            throw new DuplicatePONumberException(req.getPoNumber());
        }

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(req.getPoNumber())
                .supplierId(req.getSupplierId())
                .orderDate(req.getOrderDate())
                .dueDate(req.getDueDate())
                .expectedDeliveryDate(req.getExpectedDeliveryDate())
                .deliveryWarehouseId(req.getDeliveryWarehouseId())
                .shippingMethod(req.getShippingMethod())
                .paymentTerms(req.getPaymentTerms())
                .notes(req.getNotes())
                .status("CREATED")
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();

        po = purchaseOrderRepository.save(po);

        List<POLineItem> lineItems = buildLineItems(po, req.getItems());
        lineItemRepository.saveAll(lineItems);
        po.setLineItems(lineItems);
        po.recalculateTotals();
        po = purchaseOrderRepository.save(po);

        log.info("PurchaseOrder created: poNumber={} by={}", po.getPoNumber(), actorId);

        eventPublisher.publishPurchaseOrderCreated(new PurchaseOrderCreatedEvent(
                po.getId(), po.getPoNumber(), po.getSupplierId(), po.getSupplierName(),
                po.getDeliveryWarehouseId(), po.getExpectedDeliveryDate(),
                po.getTotalAmount(), po.getTotalQuantity(), actorId));

        return toResponse(po);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_purchase-order:read')")
    public PurchaseOrderResponse getPurchaseOrder(String poId) {
        return toResponse(findById(poId));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_purchase-order:read')")
    public PagedResponse<PurchaseOrderSummaryResponse> listPurchaseOrders(
            String status, String supplierId, String warehouseId,
            LocalDate fromDate, LocalDate toDate, Pageable pageable) {

        Page<PurchaseOrder> page = purchaseOrderRepository.findWithFilters(
                status, supplierId, warehouseId, fromDate, toDate, pageable);

        List<PurchaseOrderSummaryResponse> items = page.getContent().stream().map(this::toSummary).toList();
        return PagedResponse.<PurchaseOrderSummaryResponse>builder()
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
    @PreAuthorize("hasAuthority('PERMISSION_purchase-order:confirm')")
    public PurchaseOrderResponse confirmPurchaseOrder(String poId, ConfirmPurchaseOrderRequest req, String actorId) {
        PurchaseOrder po = findById(poId);
        if (!po.canBeConfirmed()) {
            throw new InvalidPurchaseOrderStateException("confirm", po.getStatus());
        }

        String confirmationNumber = "CONF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        po.confirm(confirmationNumber, actorId);
        if (req.getNotes() != null) po.setNotes(req.getNotes());
        po = purchaseOrderRepository.save(po);

        log.info("PurchaseOrder confirmed: poId={} confirmationNumber={} by={}", poId, confirmationNumber, actorId);

        eventPublisher.publishPurchaseOrderConfirmed(new PurchaseOrderConfirmedEvent(
                po.getId(), po.getPoNumber(), po.getSupplierId(),
                po.getDeliveryWarehouseId(), po.getExpectedDeliveryDate(),
                po.getConfirmationNumber(), po.getConfirmationDate(), actorId));

        return toResponse(po);
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_purchase-order:receive')")
    public DeliveryResponse registerDelivery(String poId, RegisterDeliveryRequest req, String actorId) {
        PurchaseOrder po = findById(poId);
        if (!po.canReceiveDelivery()) {
            throw new InvalidPurchaseOrderStateException("register-delivery", po.getStatus());
        }

        int totalReceived = 0;
        int totalDamage = 0;
        List<DeliveryResponse.DeliveryItemResult> deliveryItems = new ArrayList<>();

        for (RegisterDeliveryRequest.DeliveryItemRequest item : req.getItems()) {
            POLineItem lineItem = lineItemRepository.findByIdAndPurchaseOrderId(item.getLineId(), poId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Line item " + item.getLineId() + " does not belong to purchase order " + poId));
            int received = item.getReceivedQuantity() != null ? item.getReceivedQuantity() : 0;
            int damage = item.getDamageCount() != null ? item.getDamageCount() : 0;
            lineItem.setQuantityReceived(lineItem.getQuantityReceived() + received);
            if (lineItem.getQuantityReceived() >= lineItem.getQuantityOrdered()) {
                lineItem.setStatus("RECEIVED");
            } else if (lineItem.getQuantityReceived() > 0) {
                lineItem.setStatus("PARTIALLY_RECEIVED");
            }
            lineItemRepository.save(lineItem);
            totalReceived += received;
            totalDamage += damage;
            deliveryItems.add(DeliveryResponse.DeliveryItemResult.builder()
                    .lineId(item.getLineId())
                    .productId(lineItem.getProductId())
                    .receivedQuantity(received)
                    .damageCount(damage)
                    .build());
        }

        po.setDeliveredQuantity(po.getDeliveredQuantity() + totalReceived);
        boolean allReceived = po.getDeliveredQuantity() >= po.getTotalQuantity();
        po.setDeliveryStatus(allReceived ? "FULLY_RECEIVED" : "PARTIALLY_RECEIVED");
        if (allReceived) {
            po.setStatus("RECEIVED");
        } else {
            po.setStatus("SHIPPED");
        }
        po.setUpdatedBy(actorId);
        po = purchaseOrderRepository.save(po);

        Instant receivedAt = Instant.now();
        DeliveryTracking delivery = DeliveryTracking.builder()
                .purchaseOrderId(poId)
                .deliveryDate(req.getDeliveryDate())
                .carrierName(req.getCarrierName())
                .trackingNumber(req.getTrackingNumber())
                .totalReceivedQuantity(totalReceived)
                .damageCount(totalDamage)
                .status("DELIVERED")
                .deliveryNotes(req.getDeliveryNotes())
                .receivedBy(actorId)
                .receivedAt(receivedAt)
                .build();
        delivery = deliveryTrackingRepository.save(delivery);

        log.info("Delivery registered: poId={} deliveryId={} received={} by={}", poId, delivery.getId(), totalReceived, actorId);

        eventPublisher.publishDeliveryRegistered(new DeliveryRegisteredEvent(
                po.getId(), po.getPoNumber(), po.getSupplierId(),
                delivery.getId(), po.getDeliveryWarehouseId(),
                req.getDeliveryDate(), totalReceived, totalDamage,
                po.getStatus(), receivedAt, actorId));

        return DeliveryResponse.builder()
                .deliveryId(delivery.getId())
                .poId(poId)
                .status(po.getStatus())
                .deliveryDate(req.getDeliveryDate())
                .totalReceivedQuantity(totalReceived)
                .damageCount(totalDamage)
                .receivedAt(receivedAt)
                .items(deliveryItems)
                .build();
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_purchase-order:write')")
    public PurchaseOrderResponse cancelPurchaseOrder(String poId, CancelPurchaseOrderRequest req, String actorId) {
        PurchaseOrder po = findById(poId);
        if (!po.canBeCancelled()) {
            throw new InvalidPurchaseOrderStateException("cancel", po.getStatus());
        }

        po.cancel(req.getReason(), actorId);
        po = purchaseOrderRepository.save(po);

        log.info("PurchaseOrder cancelled: poId={} reason={} by={}", poId, req.getReason(), actorId);

        eventPublisher.publishPurchaseOrderCancelled(new PurchaseOrderCancelledEvent(
                po.getId(), po.getPoNumber(), po.getSupplierId(),
                req.getReason(), po.getCancelledAt(), actorId));

        return toResponse(po);
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_purchase-order:quality')")
    public QualityIssueResponse recordQualityIssue(String poId, RecordQualityIssueRequest req, String actorId) {
        PurchaseOrder po = findById(poId);
        if ("CANCELLED".equals(po.getStatus())) {
            throw new InvalidPurchaseOrderStateException("record-quality-issue", po.getStatus());
        }

        QualityIssue issue = QualityIssue.builder()
                .purchaseOrder(po)
                .lineItemId(req.getLineId())
                .issueType(req.getIssueType())
                .quantity(req.getQuantity())
                .description(req.getDescription())
                .severity(req.getSeverity())
                .proposedResolution(req.getProposedResolution())
                .status("OPEN")
                .createdBy(actorId)
                .build();
        issue = qualityIssueRepository.save(issue);

        log.info("Quality issue recorded: poId={} issueId={} type={} by={}", poId, issue.getId(), req.getIssueType(), actorId);

        eventPublisher.publishQualityIssueReported(new QualityIssueReportedEvent(
                po.getId(), po.getPoNumber(), po.getSupplierId(),
                issue.getId(), req.getLineId(),
                req.getIssueType(), req.getQuantity(), req.getSeverity(),
                req.getProposedResolution(), actorId));

        return toQualityIssueResponse(issue, poId);
    }

    public PurchaseOrder findById(String poId) {
        return purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException(poId));
    }

    private List<POLineItem> buildLineItems(PurchaseOrder po, List<CreatePurchaseOrderRequest.CreateLineItemRequest> items) {
        List<POLineItem> lineItems = new ArrayList<>();
        for (CreatePurchaseOrderRequest.CreateLineItemRequest item : items) {
            POLineItem lineItem = POLineItem.builder()
                    .purchaseOrder(po)
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

    private PurchaseOrderResponse toResponse(PurchaseOrder po) {
        List<POLineItemResponse> lineItemResponses = po.getLineItems() == null ? List.of() :
                po.getLineItems().stream().map(this::toLineItemResponse).toList();

        return PurchaseOrderResponse.builder()
                .poId(po.getId())
                .poNumber(po.getPoNumber())
                .supplierId(po.getSupplierId())
                .supplierName(po.getSupplierName())
                .status(po.getStatus())
                .orderDate(po.getOrderDate())
                .dueDate(po.getDueDate())
                .expectedDeliveryDate(po.getExpectedDeliveryDate())
                .deliveryWarehouseId(po.getDeliveryWarehouseId())
                .shippingMethod(po.getShippingMethod())
                .paymentTerms(po.getPaymentTerms())
                .totalQuantity(po.getTotalQuantity())
                .deliveredQuantity(po.getDeliveredQuantity())
                .totalLineAmount(po.getTotalLineAmount())
                .discountAmount(po.getDiscountAmount())
                .taxAmount(po.getTaxAmount())
                .totalAmount(po.getTotalAmount())
                .paidAmount(po.getPaidAmount())
                .deliveryStatus(po.getDeliveryStatus())
                .paymentStatus(po.getPaymentStatus())
                .confirmationDate(po.getConfirmationDate())
                .confirmationNumber(po.getConfirmationNumber())
                .cancelledAt(po.getCancelledAt())
                .cancellationReason(po.getCancellationReason())
                .notes(po.getNotes())
                .items(lineItemResponses)
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private PurchaseOrderSummaryResponse toSummary(PurchaseOrder po) {
        return PurchaseOrderSummaryResponse.builder()
                .poId(po.getId())
                .poNumber(po.getPoNumber())
                .supplierId(po.getSupplierId())
                .supplierName(po.getSupplierName())
                .status(po.getStatus())
                .orderDate(po.getOrderDate())
                .dueDate(po.getDueDate())
                .totalAmount(po.getTotalAmount())
                .deliveryStatus(po.getDeliveryStatus())
                .paymentStatus(po.getPaymentStatus())
                .createdAt(po.getCreatedAt())
                .build();
    }

    private POLineItemResponse toLineItemResponse(POLineItem li) {
        return POLineItemResponse.builder()
                .lineId(li.getId())
                .productId(li.getProductId())
                .productName(li.getProductName())
                .quantity(li.getQuantityOrdered())
                .receivedQuantity(li.getQuantityReceived())
                .unitPrice(li.getUnitPrice())
                .discountPercentage(li.getDiscountPercentage())
                .lineAmount(li.getLineTotal())
                .status(li.getStatus())
                .notes(li.getNotes())
                .createdAt(li.getCreatedAt())
                .updatedAt(li.getUpdatedAt())
                .build();
    }

    private QualityIssueResponse toQualityIssueResponse(QualityIssue qi, String poId) {
        return QualityIssueResponse.builder()
                .issueId(qi.getId())
                .poId(poId)
                .lineId(qi.getLineItemId())
                .issueType(qi.getIssueType())
                .quantity(qi.getQuantity())
                .description(qi.getDescription())
                .severity(qi.getSeverity())
                .proposedResolution(qi.getProposedResolution())
                .status(qi.getStatus())
                .resolutionNotes(qi.getResolutionNotes())
                .resolvedAt(qi.getResolvedAt())
                .createdBy(qi.getCreatedBy())
                .createdAt(qi.getCreatedAt())
                .build();
    }
}
