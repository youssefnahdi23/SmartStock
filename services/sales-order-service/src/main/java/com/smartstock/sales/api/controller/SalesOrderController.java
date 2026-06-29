package com.smartstock.sales.api.controller;

import com.smartstock.sales.api.dto.request.*;
import com.smartstock.sales.api.dto.response.*;
import com.smartstock.common.security.SecurityUserDetails;
import com.smartstock.sales.service.SalesOrderService;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sales-orders")
@RequiredArgsConstructor
@Tag(name = "Sales Orders", description = "Sales order lifecycle: creation, confirmation, picking, shipment, delivery, cancellation")
@SecurityRequirement(name = "bearerAuth")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    @PostMapping
    @Operation(summary = "Create a new sales order")
    public ResponseEntity<Map<String, Object>> createSalesOrder(
            @Valid @RequestBody CreateSalesOrderRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        SalesOrderResponse data = salesOrderService.createSalesOrder(request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(envelope(data));
    }

    @GetMapping("/{soId}")
    @Operation(summary = "Get sales order details")
    public ResponseEntity<Map<String, Object>> getSalesOrder(@PathVariable String soId) {
        SalesOrderResponse data = salesOrderService.getSalesOrder(soId);
        return ResponseEntity.ok(envelope(data));
    }

    @GetMapping
    @Operation(summary = "List sales orders with optional filters")
    public ResponseEntity<PagedResponse<SalesOrderSummaryResponse>> listSalesOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String fulfillmentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        PagedResponse<SalesOrderSummaryResponse> result = salesOrderService.listSalesOrders(
                status, customerId, fulfillmentStatus, fromDate, toDate,
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{soId}/confirm")
    @Operation(summary = "Confirm a sales order (triggers stock reservation)")
    public ResponseEntity<Map<String, Object>> confirmSalesOrder(
            @PathVariable String soId,
            @Valid @RequestBody ConfirmSalesOrderRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        SalesOrderResponse data = salesOrderService.confirmSalesOrder(soId, request, user.getUserId());
        return ResponseEntity.ok(envelope(data));
    }

    @PostMapping("/{soId}/pick")
    @Operation(summary = "Record picking of items for a sales order")
    public ResponseEntity<Map<String, Object>> pickSalesOrder(
            @PathVariable String soId,
            @Valid @RequestBody PickSalesOrderRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        PickingResponse data = salesOrderService.pickSalesOrder(soId, request, user.getUserId());
        return ResponseEntity.ok(envelope(data));
    }

    @PostMapping("/{soId}/shipments")
    @Operation(summary = "Create a shipment for a sales order")
    public ResponseEntity<Map<String, Object>> createShipment(
            @PathVariable String soId,
            @Valid @RequestBody CreateShipmentRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        ShipmentResponse data = salesOrderService.createShipment(soId, request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(envelope(data));
    }

    @GetMapping("/{soId}/shipments")
    @Operation(summary = "Get shipments for a sales order")
    public ResponseEntity<Map<String, Object>> getShipments(@PathVariable String soId) {
        List<ShipmentResponse> data = salesOrderService.getShipments(soId);
        return ResponseEntity.ok(envelope(data));
    }

    @PostMapping("/{soId}/delivery/{shipmentId}")
    @Operation(summary = "Register delivery for a shipment")
    public ResponseEntity<Map<String, Object>> registerDelivery(
            @PathVariable String soId,
            @PathVariable String shipmentId,
            @Valid @RequestBody RegisterDeliveryRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        ShipmentResponse data = salesOrderService.registerDelivery(soId, shipmentId, request, user.getUserId());
        return ResponseEntity.ok(envelope(data));
    }

    @PostMapping("/{soId}/cancel")
    @Operation(summary = "Cancel a sales order")
    public ResponseEntity<Map<String, Object>> cancelSalesOrder(
            @PathVariable String soId,
            @Valid @RequestBody CancelSalesOrderRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        SalesOrderResponse data = salesOrderService.cancelSalesOrder(soId, request, user.getUserId());
        return ResponseEntity.ok(envelope(data));
    }

    @GetMapping("/by-customer/{customerId}")
    @Operation(summary = "Get sales order history for a customer")
    public ResponseEntity<Map<String, Object>> getOrdersByCustomer(@PathVariable String customerId) {
        List<SalesOrderSummaryResponse> data = salesOrderService.getOrdersByCustomer(customerId);
        return ResponseEntity.ok(envelope(data));
    }

    @GetMapping("/pending-delivery")
    @Operation(summary = "Get all sales orders pending delivery")
    public ResponseEntity<Map<String, Object>> getPendingDelivery() {
        List<SalesOrderSummaryResponse> data = salesOrderService.getPendingDelivery();
        return ResponseEntity.ok(envelope(data));
    }

    private Map<String, Object> envelope(Object data) {
        return Map.of("data", data, "meta", Map.of("timestamp", Instant.now().toString()));
    }
}
