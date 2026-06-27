package com.smartstock.supplier.api.controller;

import com.smartstock.supplier.api.dto.request.*;
import com.smartstock.supplier.api.dto.response.*;
import com.smartstock.common.security.SecurityUserDetails;
import com.smartstock.supplier.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/suppliers")
@RequiredArgsConstructor
@Tag(name = "Suppliers", description = "Supplier profiles, contacts, contracts, deliveries, and performance")
public class SupplierController {

    private final SupplierService supplierService;
    private final SupplierContactService contactService;
    private final SupplierContractService contractService;
    private final SupplierDeliveryService deliveryService;

    // ─── Supplier CRUD ───────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new supplier")
    public ResponseEntity<Map<String, Object>> createSupplier(
            @Valid @RequestBody CreateSupplierRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        SupplierResponse data = supplierService.createSupplier(request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(envelope(data));
    }

    @GetMapping("/{supplierId}")
    @Operation(summary = "Get supplier details")
    public ResponseEntity<Map<String, Object>> getSupplier(@PathVariable String supplierId) {
        SupplierResponse data = supplierService.getSupplier(supplierId);
        return ResponseEntity.ok(envelope(data));
    }

    @GetMapping
    @Operation(summary = "List suppliers with optional filters")
    public ResponseEntity<PagedResponse<SupplierSummaryResponse>> listSuppliers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Double minRating) {
        PagedResponse<SupplierSummaryResponse> result = supplierService.listSuppliers(
                type, status, search, minRating,
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{supplierId}")
    @Operation(summary = "Update supplier details")
    public ResponseEntity<Map<String, Object>> updateSupplier(
            @PathVariable String supplierId,
            @Valid @RequestBody UpdateSupplierRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        SupplierResponse data = supplierService.updateSupplier(supplierId, request, user.getUserId());
        return ResponseEntity.ok(envelope(data));
    }

    @DeleteMapping("/{supplierId}")
    @Operation(summary = "Deactivate supplier")
    public ResponseEntity<Map<String, Object>> deactivateSupplier(
            @PathVariable String supplierId,
            @AuthenticationPrincipal SecurityUserDetails user) {
        SupplierResponse data = supplierService.deactivateSupplier(supplierId, user.getUserId());
        return ResponseEntity.ok(envelope(data));
    }

    // ─── Suspend / Resume ────────────────────────────────────────────────────────

    @PostMapping("/{supplierId}/suspend")
    @Operation(summary = "Suspend a supplier")
    public ResponseEntity<Map<String, Object>> suspendSupplier(
            @PathVariable String supplierId,
            @Valid @RequestBody SuspendSupplierRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        SupplierResponse data = supplierService.suspendSupplier(supplierId, request, user.getUserId());
        return ResponseEntity.ok(envelope(data));
    }

    @PostMapping("/{supplierId}/resume")
    @Operation(summary = "Resume a suspended supplier")
    public ResponseEntity<Map<String, Object>> resumeSupplier(
            @PathVariable String supplierId,
            @AuthenticationPrincipal SecurityUserDetails user) {
        SupplierResponse data = supplierService.resumeSupplier(supplierId, user.getUserId());
        return ResponseEntity.ok(envelope(data));
    }

    // ─── Top-Rated ───────────────────────────────────────────────────────────────

    @GetMapping("/by-rating")
    @Operation(summary = "Get top-rated active suppliers")
    public ResponseEntity<PagedResponse<SupplierSummaryResponse>> getTopRated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<SupplierSummaryResponse> result = supplierService.getTopRated(
                PageRequest.of(page, Math.min(size, 100)));
        return ResponseEntity.ok(result);
    }

    // ─── Performance ─────────────────────────────────────────────────────────────

    @GetMapping("/{supplierId}/performance")
    @Operation(summary = "Get supplier performance report")
    public ResponseEntity<Map<String, Object>> getPerformance(
            @PathVariable String supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        SupplierPerformanceResponse data = deliveryService.getPerformance(supplierId, fromDate, toDate);
        return ResponseEntity.ok(envelope(data));
    }

    // ─── Contacts ────────────────────────────────────────────────────────────────

    @GetMapping("/{supplierId}/contacts")
    @Operation(summary = "List contacts for a supplier")
    public ResponseEntity<Map<String, Object>> listContacts(@PathVariable String supplierId) {
        List<ContactResponse> data = contactService.listContacts(supplierId);
        return ResponseEntity.ok(Map.of("data", data, "meta", metaMap()));
    }

    @PostMapping("/{supplierId}/contacts")
    @Operation(summary = "Add a contact to a supplier")
    public ResponseEntity<Map<String, Object>> addContact(
            @PathVariable String supplierId,
            @Valid @RequestBody CreateContactRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        ContactResponse data = contactService.addContact(supplierId, request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(envelope(data));
    }

    @DeleteMapping("/{supplierId}/contacts/{contactId}")
    @Operation(summary = "Deactivate a supplier contact")
    public ResponseEntity<Void> deactivateContact(
            @PathVariable String supplierId,
            @PathVariable String contactId,
            @AuthenticationPrincipal SecurityUserDetails user) {
        contactService.deactivateContact(supplierId, contactId, user.getUserId());
        return ResponseEntity.noContent().build();
    }

    // ─── Contracts ───────────────────────────────────────────────────────────────

    @GetMapping("/{supplierId}/contracts")
    @Operation(summary = "List contracts for a supplier")
    public ResponseEntity<PagedResponse<ContractResponse>> listContracts(
            @PathVariable String supplierId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ContractResponse> result = contractService.listContracts(
                supplierId, PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{supplierId}/contracts")
    @Operation(summary = "Create a contract for a supplier")
    public ResponseEntity<Map<String, Object>> createContract(
            @PathVariable String supplierId,
            @Valid @RequestBody CreateContractRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        ContractResponse data = contractService.createContract(supplierId, request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(envelope(data));
    }

    // ─── Deliveries ──────────────────────────────────────────────────────────────

    @GetMapping("/{supplierId}/deliveries")
    @Operation(summary = "List delivery records for a supplier")
    public ResponseEntity<PagedResponse<DeliveryResponse>> listDeliveries(
            @PathVariable String supplierId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        PagedResponse<DeliveryResponse> result = deliveryService.listDeliveries(
                supplierId, status, fromDate, toDate,
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "orderDate")));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{supplierId}/deliveries")
    @Operation(summary = "Register a delivery from a supplier")
    public ResponseEntity<Map<String, Object>> registerDelivery(
            @PathVariable String supplierId,
            @Valid @RequestBody RegisterDeliveryRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        DeliveryResponse data = deliveryService.registerDelivery(supplierId, request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(envelope(data));
    }

    @PostMapping("/{supplierId}/deliveries/{deliveryId}/confirm")
    @Operation(summary = "Confirm receipt and quality inspection of a delivery")
    public ResponseEntity<Map<String, Object>> confirmDelivery(
            @PathVariable String supplierId,
            @PathVariable String deliveryId,
            @Valid @RequestBody ConfirmDeliveryRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        DeliveryResponse data = deliveryService.confirmDelivery(supplierId, deliveryId, request, user.getUserId());
        return ResponseEntity.ok(envelope(data));
    }

    // ─── Products ────────────────────────────────────────────────────────────────

    @GetMapping("/{supplierId}/products")
    @PreAuthorize("hasAuthority('PERMISSION_supplier:read')")
    @Operation(summary = "List products associated with a supplier")
    public ResponseEntity<Map<String, Object>> getSupplierProducts(@PathVariable String supplierId) {
        List<SupplierProductResponse> data = supplierService.getSupplierProducts(supplierId);
        return ResponseEntity.ok(Map.of("data", data, "meta", metaMap()));
    }

    // ─── Orders (stub — delegates to Purchase Order Service) ─────────────────────

    @GetMapping("/{supplierId}/orders")
    @PreAuthorize("hasAuthority('PERMISSION_supplier:read')")
    @Operation(summary = "List purchase orders for a supplier (delegates to Purchase Order Service)")
    public ResponseEntity<Map<String, Object>> getSupplierOrders(@PathVariable String supplierId) {
        // Purchase orders live in the Purchase Order Service (not yet implemented).
        // This stub returns an empty list until cross-service delegation is wired up.
        return ResponseEntity.ok(Map.of("data", List.of(), "meta", metaMap()));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private Map<String, Object> envelope(Object data) {
        return Map.of("data", data, "meta", metaMap());
    }

    private Map<String, Object> metaMap() {
        return Map.of("timestamp", Instant.now().toString());
    }
}
