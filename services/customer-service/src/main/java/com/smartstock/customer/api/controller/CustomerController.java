package com.smartstock.customer.api.controller;

import com.smartstock.customer.api.dto.request.*;
import com.smartstock.customer.api.dto.response.*;
import com.smartstock.customer.security.SecurityUserDetails;
import com.smartstock.customer.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer profiles, contacts, addresses, segmentation, and credit management")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerContactService contactService;
    private final CustomerAddressService addressService;

    // ─── Customer CRUD ────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new customer")
    public ResponseEntity<Map<String, Object>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        CustomerResponse data = customerService.createCustomer(request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(envelope(data));
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Get customer details")
    public ResponseEntity<Map<String, Object>> getCustomer(@PathVariable String customerId) {
        CustomerResponse data = customerService.getCustomer(customerId);
        return ResponseEntity.ok(envelope(data));
    }

    @GetMapping
    @Operation(summary = "List customers with optional filters")
    public ResponseEntity<PagedResponse<CustomerSummaryResponse>> listCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String segment,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        PagedResponse<CustomerSummaryResponse> result = customerService.listCustomers(
                type, segment, status, search,
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{customerId}")
    @Operation(summary = "Update customer details")
    public ResponseEntity<Map<String, Object>> updateCustomer(
            @PathVariable String customerId,
            @Valid @RequestBody UpdateCustomerRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        CustomerResponse data = customerService.updateCustomer(customerId, request, user.getUserId());
        return ResponseEntity.ok(envelope(data));
    }

    @DeleteMapping("/{customerId}")
    @Operation(summary = "Deactivate a customer")
    public ResponseEntity<Map<String, Object>> deactivateCustomer(
            @PathVariable String customerId,
            @AuthenticationPrincipal SecurityUserDetails user) {
        CustomerResponse data = customerService.deactivateCustomer(customerId, user.getUserId());
        return ResponseEntity.ok(envelope(data));
    }

    // ─── Suspend / Resume ─────────────────────────────────────────────────────────

    @PostMapping("/{customerId}/suspend")
    @Operation(summary = "Suspend a customer")
    public ResponseEntity<Map<String, Object>> suspendCustomer(
            @PathVariable String customerId,
            @Valid @RequestBody SuspendCustomerRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        CustomerResponse data = customerService.suspendCustomer(customerId, request, user.getUserId());
        return ResponseEntity.ok(envelope(data));
    }

    @PostMapping("/{customerId}/resume")
    @Operation(summary = "Resume a suspended customer")
    public ResponseEntity<Map<String, Object>> resumeCustomer(
            @PathVariable String customerId,
            @AuthenticationPrincipal SecurityUserDetails user) {
        CustomerResponse data = customerService.resumeCustomer(customerId, user.getUserId());
        return ResponseEntity.ok(envelope(data));
    }

    // ─── By Segment ───────────────────────────────────────────────────────────────

    @GetMapping("/by-segment")
    @Operation(summary = "List customers by segment")
    public ResponseEntity<PagedResponse<CustomerSummaryResponse>> getBySegment(
            @RequestParam String segment,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<CustomerSummaryResponse> result = customerService.listBySegment(
                segment, PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(result);
    }

    // ─── Contacts ─────────────────────────────────────────────────────────────────

    @GetMapping("/{customerId}/contacts")
    @Operation(summary = "List contacts for a customer")
    public ResponseEntity<Map<String, Object>> listContacts(@PathVariable String customerId) {
        List<ContactResponse> data = contactService.listContacts(customerId);
        return ResponseEntity.ok(Map.of("data", data, "meta", metaMap()));
    }

    @PostMapping("/{customerId}/contacts")
    @Operation(summary = "Add a contact to a customer")
    public ResponseEntity<Map<String, Object>> addContact(
            @PathVariable String customerId,
            @Valid @RequestBody CreateContactRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        ContactResponse data = contactService.addContact(customerId, request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(envelope(data));
    }

    @DeleteMapping("/{customerId}/contacts/{contactId}")
    @Operation(summary = "Deactivate a customer contact")
    public ResponseEntity<Void> deactivateContact(
            @PathVariable String customerId,
            @PathVariable String contactId,
            @AuthenticationPrincipal SecurityUserDetails user) {
        contactService.deactivateContact(customerId, contactId, user.getUserId());
        return ResponseEntity.noContent().build();
    }

    // ─── Addresses ────────────────────────────────────────────────────────────────

    @GetMapping("/{customerId}/addresses")
    @Operation(summary = "List addresses for a customer")
    public ResponseEntity<Map<String, Object>> listAddresses(@PathVariable String customerId) {
        List<AddressResponse> data = addressService.listAddresses(customerId);
        return ResponseEntity.ok(Map.of("data", data, "meta", metaMap()));
    }

    @PostMapping("/{customerId}/addresses")
    @Operation(summary = "Add an address to a customer")
    public ResponseEntity<Map<String, Object>> addAddress(
            @PathVariable String customerId,
            @Valid @RequestBody CreateAddressRequest request,
            @AuthenticationPrincipal SecurityUserDetails user) {
        AddressResponse data = addressService.addAddress(customerId, request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(envelope(data));
    }

    @DeleteMapping("/{customerId}/addresses/{addressId}")
    @Operation(summary = "Deactivate a customer address")
    public ResponseEntity<Void> deactivateAddress(
            @PathVariable String customerId,
            @PathVariable String addressId,
            @AuthenticationPrincipal SecurityUserDetails user) {
        addressService.deactivateAddress(customerId, addressId, user.getUserId());
        return ResponseEntity.noContent().build();
    }

    // ─── Purchase History (reference stub) ────────────────────────────────────────

    @GetMapping("/{customerId}/orders")
    @Operation(summary = "Get customer order history (reference — delegates to Sales Order Service)")
    public ResponseEntity<Map<String, Object>> getCustomerOrders(@PathVariable String customerId) {
        customerService.findById(customerId);
        return ResponseEntity.ok(Map.of("data", List.of(), "meta", metaMap()));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────────

    private Map<String, Object> envelope(Object data) {
        return Map.of("data", data, "meta", metaMap());
    }

    private Map<String, Object> metaMap() {
        return Map.of("timestamp", Instant.now().toString());
    }
}
