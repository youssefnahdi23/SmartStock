package com.smartstock.purchase.api.controller;

import com.smartstock.purchase.api.dto.request.*;
import com.smartstock.purchase.api.dto.response.*;
import com.smartstock.common.security.SecurityUserDetails;
import com.smartstock.purchase.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "Purchase Orders", description = "Purchase order lifecycle: creation, approval, delivery tracking, quality issues")
@SecurityRequirement(name = "bearerAuth")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping
    @Operation(summary = "Create a new purchase order")
    public ResponseEntity<Map<String, Object>> createPurchaseOrder(
            @Valid @RequestBody CreatePurchaseOrderRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        PurchaseOrderResponse data = purchaseOrderService.createPurchaseOrder(request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(envelope(data));
    }

    @GetMapping("/{poId}")
    @Operation(summary = "Get purchase order details")
    public ResponseEntity<Map<String, Object>> getPurchaseOrder(@PathVariable String poId) {
        PurchaseOrderResponse data = purchaseOrderService.getPurchaseOrder(poId);
        return ResponseEntity.ok(envelope(data));
    }

    @GetMapping
    @Operation(summary = "List purchase orders with optional filters")
    public ResponseEntity<PagedResponse<PurchaseOrderSummaryResponse>> listPurchaseOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        PagedResponse<PurchaseOrderSummaryResponse> result = purchaseOrderService.listPurchaseOrders(
                status, supplierId, warehouseId, fromDate, toDate,
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{poId}/confirm")
    @Operation(summary = "Confirm a purchase order with supplier")
    public ResponseEntity<Map<String, Object>> confirmPurchaseOrder(
            @PathVariable String poId,
            @Valid @RequestBody ConfirmPurchaseOrderRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        PurchaseOrderResponse data = purchaseOrderService.confirmPurchaseOrder(poId, request, user.getUserId());
        return ResponseEntity.ok(envelope(data));
    }

    @PostMapping("/{poId}/delivery")
    @Operation(summary = "Register a delivery for a purchase order")
    public ResponseEntity<Map<String, Object>> registerDelivery(
            @PathVariable String poId,
            @Valid @RequestBody RegisterDeliveryRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        DeliveryResponse data = purchaseOrderService.registerDelivery(poId, request, user.getUserId());
        return ResponseEntity.ok(envelope(data));
    }

    @PostMapping("/{poId}/cancel")
    @Operation(summary = "Cancel a purchase order")
    public ResponseEntity<Map<String, Object>> cancelPurchaseOrder(
            @PathVariable String poId,
            @Valid @RequestBody CancelPurchaseOrderRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        PurchaseOrderResponse data = purchaseOrderService.cancelPurchaseOrder(poId, request, user.getUserId());
        return ResponseEntity.ok(envelope(data));
    }

    @PostMapping("/{poId}/quality-issue")
    @Operation(summary = "Record a quality issue on a delivered purchase order")
    public ResponseEntity<Map<String, Object>> recordQualityIssue(
            @PathVariable String poId,
            @Valid @RequestBody RecordQualityIssueRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        QualityIssueResponse data = purchaseOrderService.recordQualityIssue(poId, request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(envelope(data));
    }

    private Map<String, Object> envelope(Object data) {
        return Map.of("data", data, "meta", Map.of("timestamp", Instant.now().toString()));
    }
}
