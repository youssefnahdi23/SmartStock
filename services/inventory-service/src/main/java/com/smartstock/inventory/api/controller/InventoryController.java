package com.smartstock.inventory.api.controller;

import com.smartstock.inventory.api.dto.request.*;
import com.smartstock.inventory.api.dto.response.*;
import com.smartstock.inventory.security.SecurityUserDetails;
import com.smartstock.inventory.service.*;
import io.swagger.v3.oas.annotations.Operation;
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
import java.time.ZoneOffset;
import java.util.Map;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Stock movements, levels, counts, and reservations")
public class InventoryController {

    private final InventoryService inventoryService;
    private final TransferService transferService;
    private final AdjustmentService adjustmentService;
    private final CountService countService;
    private final ReservationService reservationService;

    // ─── Stock In ────────────────────────────────────────────────────────────

    @PostMapping("/stock-in")
    @Operation(summary = "Receive stock into a warehouse")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Map<String, Object>> stockIn(
            @Valid @RequestBody StockInRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        StockTransactionResponse data = inventoryService.receiveStock(request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "data", data,
                "meta", Map.of("timestamp", Instant.now().toString())));
    }

    // ─── Stock Out ───────────────────────────────────────────────────────────

    @PostMapping("/stock-out")
    @Operation(summary = "Dispatch stock from a warehouse")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Map<String, Object>> stockOut(
            @Valid @RequestBody StockOutRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        StockTransactionResponse data = inventoryService.dispatchStock(request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "data", data,
                "meta", Map.of("timestamp", Instant.now().toString())));
    }

    // ─── Transfer ────────────────────────────────────────────────────────────

    @PostMapping("/transfers")
    @Operation(summary = "Transfer stock between warehouses")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Map<String, Object>> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        TransferResponse data = transferService.transfer(request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "data", data,
                "meta", Map.of("timestamp", Instant.now().toString())));
    }

    // ─── Adjustment ──────────────────────────────────────────────────────────

    @PostMapping("/adjustments")
    @Operation(summary = "Adjust stock level for a product in a warehouse")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Map<String, Object>> adjust(
            @Valid @RequestBody AdjustmentRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        AdjustmentResponse data = adjustmentService.adjust(request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "data", data,
                "meta", Map.of("timestamp", Instant.now().toString())));
    }

    // ─── Stock Level Queries ─────────────────────────────────────────────────

    @GetMapping("/stock/{productId}/{warehouseId}")
    @Operation(summary = "Get current stock level for a product in a warehouse")
    public ResponseEntity<Map<String, Object>> getStockLevel(
            @PathVariable String productId,
            @PathVariable String warehouseId) {
        StockLevelResponse data = inventoryService.getStockLevel(productId, warehouseId);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "meta", Map.of("timestamp", Instant.now().toString())));
    }

    @GetMapping("/products/{productId}/stock")
    @Operation(summary = "Get stock levels for a product across all warehouses")
    public ResponseEntity<Map<String, Object>> getProductStock(
            @PathVariable String productId) {
        ProductStockResponse data = inventoryService.getProductStock(productId);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "meta", Map.of("timestamp", Instant.now().toString())));
    }

    @GetMapping("/warehouses/{warehouseId}/stock")
    @Operation(summary = "Get all stock levels in a warehouse (paged)")
    public ResponseEntity<PagedResponse<WarehouseStockItemResponse>> getWarehouseStock(
            @PathVariable String warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean lowStockOnly,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "productId,asc") String sort) {

        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction dir = sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1])
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        PagedResponse<WarehouseStockItemResponse> result = inventoryService.getWarehouseStock(
                warehouseId, lowStockOnly, search,
                PageRequest.of(page, Math.min(size, 100), Sort.by(dir, sortField)));
        return ResponseEntity.ok(result);
    }

    // ─── Transaction History ─────────────────────────────────────────────────

    @GetMapping("/transactions")
    @Operation(summary = "Get stock movement history with optional filters")
    public ResponseEntity<PagedResponse<TransactionResponse>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        Instant from = fromDate != null ? fromDate.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant to = toDate != null ? toDate.atTime(23, 59, 59).toInstant(ZoneOffset.UTC) : null;

        PagedResponse<TransactionResponse> result = inventoryService.getTransactions(
                productId, warehouseId, transactionType, userId, from, to,
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "timestamp")));
        return ResponseEntity.ok(result);
    }

    // ─── Physical Counts ─────────────────────────────────────────────────────

    @PostMapping("/counts")
    @Operation(summary = "Begin a physical inventory count")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Map<String, Object>> beginCount(
            @Valid @RequestBody BeginCountRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        CountResponse data = countService.beginCount(request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "data", data,
                "meta", Map.of("timestamp", Instant.now().toString())));
    }

    @PostMapping("/counts/{countId}/items")
    @Operation(summary = "Record a counted item during a physical count")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Map<String, Object>> recordCountItem(
            @PathVariable String countId,
            @Valid @RequestBody RecordCountItemRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        CountItemResponse data = countService.recordItem(countId, request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "data", data,
                "meta", Map.of("timestamp", Instant.now().toString())));
    }

    @PostMapping("/counts/{countId}/complete")
    @Operation(summary = "Complete a physical count, optionally auto-adjusting variances")
    public ResponseEntity<Map<String, Object>> completeCount(
            @PathVariable String countId,
            @RequestBody(required = false) CompleteCountRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        if (request == null) request = new CompleteCountRequest();
        CompleteCountResponse data = countService.completeCount(countId, request, user.getUserId());
        return ResponseEntity.ok(Map.of(
                "data", data,
                "meta", Map.of("timestamp", Instant.now().toString())));
    }

    // ─── Reservations ────────────────────────────────────────────────────────

    @PostMapping("/reservations")
    @Operation(summary = "Reserve stock for a sales order or other purpose")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Map<String, Object>> reserve(
            @Valid @RequestBody ReservationRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        ReservationResponse data = reservationService.reserve(request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "data", data,
                "meta", Map.of("timestamp", Instant.now().toString())));
    }
}
