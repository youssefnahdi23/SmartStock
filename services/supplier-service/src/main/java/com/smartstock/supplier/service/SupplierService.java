package com.smartstock.supplier.service;

import com.smartstock.supplier.api.dto.request.CreateSupplierRequest;
import com.smartstock.supplier.api.dto.request.SuspendSupplierRequest;
import com.smartstock.supplier.api.dto.request.UpdateSupplierRequest;
import com.smartstock.supplier.api.dto.response.PagedResponse;
import com.smartstock.supplier.api.dto.response.SupplierResponse;
import com.smartstock.supplier.api.dto.response.SupplierSummaryResponse;
import com.smartstock.supplier.domain.event.*;
import com.smartstock.supplier.domain.model.Supplier;
import com.smartstock.supplier.domain.repository.SupplierRepository;
import com.smartstock.supplier.exception.SupplierCodeExistsException;
import com.smartstock.supplier.exception.SupplierNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierEventPublisher eventPublisher;

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_supplier:create')")
    public SupplierResponse createSupplier(CreateSupplierRequest req, String actorId) {
        if (supplierRepository.existsBySupplierCode(req.getSupplierCode())) {
            throw new SupplierCodeExistsException(req.getSupplierCode());
        }

        String certs = req.getCertifications() != null ? String.join(",", req.getCertifications()) : null;

        Supplier supplier = Supplier.builder()
                .supplierCode(req.getSupplierCode())
                .supplierName(req.getSupplierName())
                .supplierType(req.getSupplierType() != null ? req.getSupplierType() : "VENDOR")
                .businessRegistrationNumber(req.getBusinessRegistrationNumber())
                .taxId(req.getTaxId())
                .websiteUrl(req.getWebsiteUrl())
                .emailAddress(req.getEmailAddress())
                .phoneNumber(req.getPhoneNumber())
                .paymentTerms(req.getPaymentTerms())
                .currencyCode(req.getCurrencyCode() != null ? req.getCurrencyCode() : "USD")
                .countryCode(req.getCountryCode() != null ? req.getCountryCode() : "US")
                .headquarterAddress(req.getHeadquarterAddress())
                .city(req.getCity())
                .stateProvince(req.getStateProvince())
                .postalCode(req.getPostalCode())
                .creditLimit(req.getCreditLimit())
                .averageLeadTimeDays(req.getAverageLeadTimeDays() != null ? req.getAverageLeadTimeDays() : 7)
                .minimumOrderQuantity(req.getMinimumOrderQuantity() != null ? req.getMinimumOrderQuantity() : 1)
                .minimumOrderValue(req.getMinimumOrderValue())
                .riskRating(req.getRiskRating() != null ? req.getRiskRating() : "MEDIUM")
                .certifications(certs)
                .notes(req.getNotes())
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();

        supplier = supplierRepository.save(supplier);
        log.info("Supplier created: code={} by={}", supplier.getSupplierCode(), actorId);

        eventPublisher.publishSupplierCreated(new SupplierCreatedEvent(
                supplier.getId(), supplier.getSupplierCode(), supplier.getSupplierName(),
                supplier.getSupplierType(), actorId));

        return toResponse(supplier);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_supplier:read')")
    public SupplierResponse getSupplier(String supplierId) {
        return toResponse(findById(supplierId));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_supplier:read')")
    public PagedResponse<SupplierSummaryResponse> listSuppliers(
            String type, String status, String search, Double minRating, Pageable pageable) {

        Page<Supplier> page = supplierRepository.findWithFilters(type, status, search, minRating, pageable);
        List<SupplierSummaryResponse> items = page.getContent().stream().map(this::toSummary).toList();

        return PagedResponse.<SupplierSummaryResponse>builder()
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
    @PreAuthorize("hasAuthority('PERMISSION_supplier:write')")
    public SupplierResponse updateSupplier(String supplierId, UpdateSupplierRequest req, String actorId) {
        Supplier supplier = findById(supplierId);

        if (req.getSupplierName() != null) supplier.setSupplierName(req.getSupplierName());
        if (req.getWebsiteUrl() != null) supplier.setWebsiteUrl(req.getWebsiteUrl());
        if (req.getEmailAddress() != null) supplier.setEmailAddress(req.getEmailAddress());
        if (req.getPhoneNumber() != null) supplier.setPhoneNumber(req.getPhoneNumber());
        if (req.getPaymentTerms() != null) supplier.setPaymentTerms(req.getPaymentTerms());
        if (req.getCurrencyCode() != null) supplier.setCurrencyCode(req.getCurrencyCode());
        if (req.getHeadquarterAddress() != null) supplier.setHeadquarterAddress(req.getHeadquarterAddress());
        if (req.getCity() != null) supplier.setCity(req.getCity());
        if (req.getStateProvince() != null) supplier.setStateProvince(req.getStateProvince());
        if (req.getPostalCode() != null) supplier.setPostalCode(req.getPostalCode());
        if (req.getCreditLimit() != null) supplier.setCreditLimit(req.getCreditLimit());
        if (req.getAverageLeadTimeDays() != null) supplier.setAverageLeadTimeDays(req.getAverageLeadTimeDays());
        if (req.getMinimumOrderQuantity() != null) supplier.setMinimumOrderQuantity(req.getMinimumOrderQuantity());
        if (req.getMinimumOrderValue() != null) supplier.setMinimumOrderValue(req.getMinimumOrderValue());
        if (req.getRiskRating() != null) supplier.setRiskRating(req.getRiskRating());
        if (req.getNotes() != null) supplier.setNotes(req.getNotes());
        if (req.getCertifications() != null) supplier.setCertifications(String.join(",", req.getCertifications()));
        supplier.setUpdatedBy(actorId);

        supplier = supplierRepository.save(supplier);
        log.info("Supplier updated: id={} by={}", supplierId, actorId);

        eventPublisher.publishSupplierUpdated(new SupplierUpdatedEvent(
                supplier.getId(), supplier.getSupplierCode(), supplier.getSupplierName(), actorId));

        return toResponse(supplier);
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_supplier:write')")
    public SupplierResponse suspendSupplier(String supplierId, SuspendSupplierRequest req, String actorId) {
        Supplier supplier = findById(supplierId);
        supplier.suspend(req.getReason(), req.getResumeDate());
        supplier.setUpdatedBy(actorId);
        supplier = supplierRepository.save(supplier);
        log.info("Supplier suspended: id={} reason={} by={}", supplierId, req.getReason(), actorId);

        eventPublisher.publishSupplierSuspended(new SupplierSuspendedEvent(
                supplier.getId(), supplier.getSupplierCode(), supplier.getSupplierName(),
                req.getReason(), req.getResumeDate(), actorId));

        return toResponse(supplier);
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_supplier:write')")
    public SupplierResponse resumeSupplier(String supplierId, String actorId) {
        Supplier supplier = findById(supplierId);
        supplier.activate();
        supplier.setUpdatedBy(actorId);
        supplier = supplierRepository.save(supplier);
        log.info("Supplier resumed: id={} by={}", supplierId, actorId);

        eventPublisher.publishSupplierResumed(new SupplierResumedEvent(
                supplier.getId(), supplier.getSupplierCode(), supplier.getSupplierName(), actorId));

        return toResponse(supplier);
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_supplier:write')")
    public SupplierResponse deactivateSupplier(String supplierId, String actorId) {
        Supplier supplier = findById(supplierId);
        supplier.setIsActive(false);
        supplier.setUpdatedBy(actorId);
        supplier = supplierRepository.save(supplier);
        log.info("Supplier deactivated: id={} by={}", supplierId, actorId);
        return toResponse(supplier);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_supplier:read')")
    public PagedResponse<SupplierSummaryResponse> getTopRated(Pageable pageable) {
        Page<Supplier> page = supplierRepository.findTopRated(pageable);
        List<SupplierSummaryResponse> items = page.getContent().stream().map(this::toSummary).toList();
        return PagedResponse.<SupplierSummaryResponse>builder()
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

    public Supplier findById(String supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new SupplierNotFoundException(supplierId));
    }

    private String resolveStatus(Supplier s) {
        if (Boolean.TRUE.equals(s.getIsActive())) return "ACTIVE";
        if (s.getSuspensionReason() != null) return "SUSPENDED";
        return "INACTIVE";
    }

    private SupplierResponse toResponse(Supplier s) {
        return SupplierResponse.builder()
                .id(s.getId())
                .supplierCode(s.getSupplierCode())
                .supplierName(s.getSupplierName())
                .supplierType(s.getSupplierType())
                .status(resolveStatus(s))
                .businessRegistrationNumber(s.getBusinessRegistrationNumber())
                .taxId(s.getTaxId())
                .websiteUrl(s.getWebsiteUrl())
                .emailAddress(s.getEmailAddress())
                .phoneNumber(s.getPhoneNumber())
                .paymentTerms(s.getPaymentTerms())
                .currencyCode(s.getCurrencyCode())
                .countryCode(s.getCountryCode())
                .headquarterAddress(s.getHeadquarterAddress())
                .city(s.getCity())
                .stateProvince(s.getStateProvince())
                .postalCode(s.getPostalCode())
                .creditLimit(s.getCreditLimit())
                .averageLeadTimeDays(s.getAverageLeadTimeDays())
                .minimumOrderQuantity(s.getMinimumOrderQuantity())
                .minimumOrderValue(s.getMinimumOrderValue())
                .isVerified(s.getIsVerified())
                .riskRating(s.getRiskRating())
                .rating(s.getRating())
                .totalOrders(s.getTotalOrders())
                .totalSpent(s.getTotalSpent())
                .certifications(s.getCertifications())
                .notes(s.getNotes())
                .suspensionReason(s.getSuspensionReason())
                .suspendedAt(s.getSuspendedAt())
                .createdBy(s.getCreatedBy())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private SupplierSummaryResponse toSummary(Supplier s) {
        return SupplierSummaryResponse.builder()
                .id(s.getId())
                .supplierCode(s.getSupplierCode())
                .supplierName(s.getSupplierName())
                .supplierType(s.getSupplierType())
                .status(resolveStatus(s))
                .emailAddress(s.getEmailAddress())
                .phoneNumber(s.getPhoneNumber())
                .countryCode(s.getCountryCode())
                .city(s.getCity())
                .rating(s.getRating())
                .totalOrders(s.getTotalOrders())
                .totalSpent(s.getTotalSpent())
                .isVerified(s.getIsVerified())
                .riskRating(s.getRiskRating())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
