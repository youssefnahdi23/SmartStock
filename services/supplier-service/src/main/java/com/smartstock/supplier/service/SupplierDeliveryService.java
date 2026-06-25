package com.smartstock.supplier.service;

import com.smartstock.supplier.api.dto.request.ConfirmDeliveryRequest;
import com.smartstock.supplier.api.dto.request.RegisterDeliveryRequest;
import com.smartstock.supplier.api.dto.response.DeliveryResponse;
import com.smartstock.supplier.api.dto.response.PagedResponse;
import com.smartstock.supplier.api.dto.response.SupplierPerformanceResponse;
import com.smartstock.supplier.domain.event.SupplierDeliveryRegisteredEvent;
import com.smartstock.supplier.domain.event.SupplierPerformanceUpdatedEvent;
import com.smartstock.supplier.domain.model.Supplier;
import com.smartstock.supplier.domain.model.SupplierDelivery;
import com.smartstock.supplier.domain.model.SupplierMetrics;
import com.smartstock.supplier.domain.repository.SupplierDeliveryRepository;
import com.smartstock.supplier.domain.repository.SupplierMetricsRepository;
import com.smartstock.supplier.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierDeliveryService {

    private final SupplierDeliveryRepository deliveryRepository;
    private final SupplierMetricsRepository metricsRepository;
    private final SupplierService supplierService;
    private final SupplierEventPublisher eventPublisher;

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_supplier:write')")
    public DeliveryResponse registerDelivery(String supplierId, RegisterDeliveryRequest req, String actorId) {
        supplierService.findById(supplierId);

        if (deliveryRepository.existsByDeliveryNumber(req.getDeliveryNumber())) {
            throw new BusinessException("DELIVERY_NUMBER_EXISTS",
                    "Delivery number already exists: " + req.getDeliveryNumber(), HttpStatus.BAD_REQUEST);
        }

        SupplierDelivery delivery = SupplierDelivery.builder()
                .supplierId(supplierId)
                .purchaseOrderId(req.getPurchaseOrderId())
                .deliveryNumber(req.getDeliveryNumber())
                .orderDate(req.getOrderDate())
                .promisedDeliveryDate(req.getPromisedDeliveryDate())
                .quantityOrdered(req.getQuantityOrdered())
                .totalValue(req.getTotalValue())
                .carrierName(req.getCarrierName())
                .trackingNumber(req.getTrackingNumber())
                .notes(req.getNotes())
                .deliveryStatus("PENDING")
                .build();

        delivery = deliveryRepository.save(delivery);
        log.info("Delivery registered: supplierId={} deliveryId={} by={}", supplierId, delivery.getId(), actorId);

        Supplier supplier = supplierService.findById(supplierId);
        eventPublisher.publishDeliveryRegistered(new SupplierDeliveryRegisteredEvent(
                delivery.getId(), supplierId, delivery.getDeliveryNumber(),
                delivery.getPurchaseOrderId(), supplier.getSupplierCode(),
                delivery.getOrderDate(), delivery.getPromisedDeliveryDate(),
                delivery.getQuantityOrdered(), actorId));

        return toResponse(delivery);
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_supplier:write')")
    public DeliveryResponse confirmDelivery(String supplierId, String deliveryId, ConfirmDeliveryRequest req, String actorId) {
        SupplierDelivery delivery = deliveryRepository.findById(deliveryId)
                .filter(d -> supplierId.equals(d.getSupplierId()))
                .orElseThrow(() -> new BusinessException("DELIVERY_NOT_FOUND",
                        "Delivery not found: " + deliveryId, HttpStatus.NOT_FOUND));

        delivery.confirmDelivery(
                req.getActualDeliveryDate(),
                req.getQuantityReceived(),
                req.getQuantityRejected(),
                actorId);
        delivery.setQualityRating(req.getQualityRating());
        delivery.setQualityIssuesFound(req.getQualityIssuesFound() != null ? req.getQualityIssuesFound() : 0);
        delivery.setQualityInspectionStatus(req.getQualityInspectionStatus() != null ? req.getQualityInspectionStatus() : "PASSED");

        delivery = deliveryRepository.save(delivery);
        log.info("Delivery confirmed: supplierId={} deliveryId={} onTime={} by={}",
                supplierId, deliveryId, delivery.getOnTime(), actorId);

        recalculateMetrics(supplierId, delivery.getOrderDate(), actorId);
        return toResponse(delivery);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_supplier:read')")
    public PagedResponse<DeliveryResponse> listDeliveries(
            String supplierId, String status, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        supplierService.findById(supplierId);
        Page<SupplierDelivery> page = deliveryRepository.findBySupplierWithFilters(supplierId, status, fromDate, toDate, pageable);
        List<DeliveryResponse> items = page.getContent().stream().map(this::toResponse).toList();
        return PagedResponse.<DeliveryResponse>builder()
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

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_supplier:report')")
    public SupplierPerformanceResponse getPerformance(String supplierId, LocalDate fromDate, LocalDate toDate) {
        Supplier supplier = supplierService.findById(supplierId);

        LocalDate from = fromDate != null ? fromDate : LocalDate.now().minusDays(90);
        LocalDate to = toDate != null ? toDate : LocalDate.now();

        List<SupplierDelivery> deliveries = deliveryRepository.findDeliveredInPeriod(supplierId, from, to);

        long total = deliveries.size();
        long onTime = deliveries.stream().filter(d -> Boolean.TRUE.equals(d.getOnTime())).count();
        long late = total - onTime;

        BigDecimal onTimeRate = total > 0
                ? BigDecimal.valueOf(onTime * 100.0 / total).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        int totalQualityIssues = deliveries.stream().mapToInt(d -> d.getQualityIssuesFound() != null ? d.getQualityIssuesFound() : 0).sum();
        int totalQtyReceived = deliveries.stream().mapToInt(d -> d.getQuantityReceived() != null ? d.getQuantityReceived() : 0).sum();
        int totalQtyRejected = deliveries.stream().mapToInt(d -> d.getQuantityRejected() != null ? d.getQuantityRejected() : 0).sum();

        BigDecimal qualityPassRate = totalQtyReceived > 0
                ? BigDecimal.valueOf((totalQtyReceived - totalQtyRejected) * 100.0 / totalQtyReceived).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal totalValue = deliveries.stream()
                .map(d -> d.getTotalValue() != null ? d.getTotalValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return SupplierPerformanceResponse.builder()
                .supplierId(supplierId)
                .supplierCode(supplier.getSupplierCode())
                .supplierName(supplier.getSupplierName())
                .fromDate(from)
                .toDate(to)
                .totalOrders(total)
                .onTimeDeliveries(onTime)
                .lateDeliveries(late)
                .onTimeDeliveryRate(onTimeRate)
                .qualityPassRate(qualityPassRate)
                .qualityIssuesCount(totalQualityIssues)
                .totalValueReceived(totalValue)
                .build();
    }

    private void recalculateMetrics(String supplierId, LocalDate metricDate, String actorId) {
        LocalDate monthStart = metricDate.withDayOfMonth(1);
        LocalDate monthEnd = metricDate.withDayOfMonth(metricDate.lengthOfMonth());

        List<SupplierDelivery> deliveries = deliveryRepository.findDeliveredInPeriod(supplierId, monthStart, monthEnd);
        if (deliveries.isEmpty()) return;

        long total = deliveries.size();
        long onTimeCount = deliveries.stream().filter(d -> Boolean.TRUE.equals(d.getOnTime())).count();
        BigDecimal onTimeRate = BigDecimal.valueOf(onTimeCount * 100.0 / total).setScale(2, RoundingMode.HALF_UP);

        int totalQtyReceived = deliveries.stream().mapToInt(d -> d.getQuantityReceived() != null ? d.getQuantityReceived() : 0).sum();
        int totalQtyRejected = deliveries.stream().mapToInt(d -> d.getQuantityRejected() != null ? d.getQuantityRejected() : 0).sum();
        BigDecimal qualityPassRate = totalQtyReceived > 0
                ? BigDecimal.valueOf((totalQtyReceived - totalQtyRejected) * 100.0 / totalQtyReceived).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal totalValue = deliveries.stream()
                .map(d -> d.getTotalValue() != null ? d.getTotalValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int qualityIssues = deliveries.stream().mapToInt(d -> d.getQualityIssuesFound() != null ? d.getQualityIssuesFound() : 0).sum();

        BigDecimal overallScore = onTimeRate.add(qualityPassRate)
                .divide(BigDecimal.valueOf(20), 2, RoundingMode.HALF_UP);

        SupplierMetrics metrics = metricsRepository.findBySupplierIdAndMetricDate(supplierId, metricDate)
                .orElse(SupplierMetrics.builder().supplierId(supplierId).metricDate(metricDate).build());

        metrics.setTotalOrders((int) total);
        metrics.setTotalUnitsReceived(totalQtyReceived);
        metrics.setOnTimeDeliveries((int) onTimeCount);
        metrics.setOnTimeDeliveryRate(onTimeRate);
        metrics.setQualityPassRate(qualityPassRate);
        metrics.setQualityIssuesCount(qualityIssues);
        metrics.setTotalValueReceived(totalValue);
        metrics.setOverallPerformanceScore(overallScore);
        metricsRepository.save(metrics);

        Supplier supplier = supplierService.findById(supplierId);
        eventPublisher.publishPerformanceUpdated(new SupplierPerformanceUpdatedEvent(
                supplierId, supplier.getSupplierCode(), metricDate, onTimeRate, qualityPassRate, overallScore));
    }

    private DeliveryResponse toResponse(SupplierDelivery d) {
        return DeliveryResponse.builder()
                .id(d.getId())
                .supplierId(d.getSupplierId())
                .purchaseOrderId(d.getPurchaseOrderId())
                .deliveryNumber(d.getDeliveryNumber())
                .orderDate(d.getOrderDate())
                .promisedDeliveryDate(d.getPromisedDeliveryDate())
                .actualDeliveryDate(d.getActualDeliveryDate())
                .quantityOrdered(d.getQuantityOrdered())
                .quantityReceived(d.getQuantityReceived())
                .quantityRejected(d.getQuantityRejected())
                .deliveryStatus(d.getDeliveryStatus())
                .onTime(d.getOnTime())
                .onTimeDaysVariance(d.getOnTimeDaysVariance())
                .qualityInspectionStatus(d.getQualityInspectionStatus())
                .qualityIssuesFound(d.getQualityIssuesFound())
                .qualityRating(d.getQualityRating())
                .carrierName(d.getCarrierName())
                .trackingNumber(d.getTrackingNumber())
                .totalValue(d.getTotalValue())
                .notes(d.getNotes())
                .receivedBy(d.getReceivedBy())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
