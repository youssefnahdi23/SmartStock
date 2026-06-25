package com.smartstock.warehouse.api.controller;

import com.smartstock.warehouse.api.dto.request.*;
import com.smartstock.warehouse.api.dto.response.*;
import com.smartstock.warehouse.security.SecurityUserDetails;
import com.smartstock.warehouse.service.WarehouseService;
import com.smartstock.warehouse.service.ZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/warehouses")
@RequiredArgsConstructor
@Tag(name = "Warehouses", description = "Warehouse management — warehouses, zones, shelves, bins")
@SecurityRequirement(name = "Bearer Authentication")
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final ZoneService zoneService;

    // ─── Warehouses ────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new warehouse")
    public ResponseEntity<Map<String, Object>> createWarehouse(
            @Valid @RequestBody CreateWarehouseRequest req,
            @AuthenticationPrincipal SecurityUserDetails principal) {
        WarehouseResponse response = warehouseService.createWarehouse(req, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", response));
    }

    @GetMapping("/{warehouseId}")
    @Operation(summary = "Get warehouse details")
    public ResponseEntity<Map<String, Object>> getWarehouse(
            @PathVariable String warehouseId) {
        WarehouseResponse response = warehouseService.getWarehouse(warehouseId);
        return ResponseEntity.ok(Map.of("data", response));
    }

    @GetMapping
    @Operation(summary = "List warehouses with filters and pagination")
    public ResponseEntity<PagedResponse<WarehouseResponse>> listWarehouses(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Search by name or code") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by type") @RequestParam(required = false) String type,
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(warehouseService.listWarehouses(page, size, search, type, active));
    }

    @PutMapping("/{warehouseId}")
    @Operation(summary = "Update warehouse information")
    public ResponseEntity<Map<String, Object>> updateWarehouse(
            @PathVariable String warehouseId,
            @Valid @RequestBody UpdateWarehouseRequest req,
            @AuthenticationPrincipal SecurityUserDetails principal) {
        WarehouseResponse response = warehouseService.updateWarehouse(warehouseId, req, principal.getUserId());
        return ResponseEntity.ok(Map.of("data", response));
    }

    @PostMapping("/{warehouseId}/deactivate")
    @Operation(summary = "Deactivate a warehouse")
    public ResponseEntity<Map<String, Object>> deactivateWarehouse(
            @PathVariable String warehouseId,
            @AuthenticationPrincipal SecurityUserDetails principal) {
        WarehouseResponse response = warehouseService.deactivateWarehouse(warehouseId, principal.getUserId());
        return ResponseEntity.ok(Map.of("data", response));
    }

    @PostMapping("/{warehouseId}/reactivate")
    @Operation(summary = "Reactivate a warehouse")
    public ResponseEntity<Map<String, Object>> reactivateWarehouse(
            @PathVariable String warehouseId,
            @AuthenticationPrincipal SecurityUserDetails principal) {
        WarehouseResponse response = warehouseService.reactivateWarehouse(warehouseId, principal.getUserId());
        return ResponseEntity.ok(Map.of("data", response));
    }

    @GetMapping("/{warehouseId}/capacity-report")
    @Operation(summary = "Get warehouse capacity report")
    public ResponseEntity<Map<String, Object>> getCapacityReport(
            @PathVariable String warehouseId) {
        CapacityReportResponse response = warehouseService.getCapacityReport(warehouseId);
        return ResponseEntity.ok(Map.of("data", response));
    }

    // ─── Zones ────────────────────────────────────────────────

    @PostMapping("/{warehouseId}/zones")
    @Operation(summary = "Create a zone in a warehouse")
    public ResponseEntity<Map<String, Object>> createZone(
            @PathVariable String warehouseId,
            @Valid @RequestBody CreateZoneRequest req,
            @AuthenticationPrincipal SecurityUserDetails principal) {
        ZoneResponse response = zoneService.createZone(warehouseId, req, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", response));
    }

    @GetMapping("/{warehouseId}/zones")
    @Operation(summary = "List zones in a warehouse")
    public ResponseEntity<PagedResponse<ZoneResponse>> listZones(
            @PathVariable String warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(zoneService.listZones(warehouseId, page, size));
    }

    @GetMapping("/{warehouseId}/zones/{zoneId}")
    @Operation(summary = "Get zone details")
    public ResponseEntity<Map<String, Object>> getZone(
            @PathVariable String warehouseId,
            @PathVariable String zoneId) {
        ZoneResponse response = zoneService.getZone(warehouseId, zoneId);
        return ResponseEntity.ok(Map.of("data", response));
    }

    // ─── Shelves ───────────────────────────────────────────────

    @PostMapping("/{warehouseId}/zones/{zoneId}/shelves")
    @Operation(summary = "Create a shelf in a zone")
    public ResponseEntity<Map<String, Object>> createShelf(
            @PathVariable String warehouseId,
            @PathVariable String zoneId,
            @Valid @RequestBody CreateShelfRequest req,
            @AuthenticationPrincipal SecurityUserDetails principal) {
        ShelfResponse response = zoneService.createShelf(warehouseId, zoneId, req, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", response));
    }

    // ─── Bins ──────────────────────────────────────────────────

    @PostMapping("/{warehouseId}/zones/{zoneId}/shelves/{shelfId}/bins")
    @Operation(summary = "Create a bin in a shelf")
    public ResponseEntity<Map<String, Object>> createBin(
            @PathVariable String warehouseId,
            @PathVariable String zoneId,
            @PathVariable String shelfId,
            @Valid @RequestBody CreateBinRequest req,
            @AuthenticationPrincipal SecurityUserDetails principal) {
        BinResponse response = zoneService.createBin(warehouseId, zoneId, shelfId, req, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", response));
    }

    @GetMapping("/{warehouseId}/bins/available")
    @Operation(summary = "Find available bins in a warehouse")
    public ResponseEntity<Map<String, Object>> findAvailableBins(
            @PathVariable String warehouseId,
            @RequestParam(required = false) String zoneId,
            @RequestParam(defaultValue = "1") int quantity) {
        List<AvailableBinResponse> bins = zoneService.findAvailableBins(warehouseId, zoneId, quantity);
        return ResponseEntity.ok(Map.of("data", bins));
    }
}
