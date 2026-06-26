package com.smartstock.customer.service;

import com.smartstock.customer.api.dto.request.CreateCustomerRequest;
import com.smartstock.customer.api.dto.request.SuspendCustomerRequest;
import com.smartstock.customer.api.dto.request.UpdateCustomerRequest;
import com.smartstock.customer.api.dto.response.CustomerResponse;
import com.smartstock.customer.api.dto.response.CustomerSummaryResponse;
import com.smartstock.customer.api.dto.response.PagedResponse;
import com.smartstock.customer.domain.event.*;
import com.smartstock.customer.domain.model.Customer;
import com.smartstock.customer.domain.repository.CustomerRepository;
import com.smartstock.customer.domain.event.CustomerCreatedEvent.AddressData;
import com.smartstock.customer.exception.CustomerCodeExistsException;
import com.smartstock.customer.exception.CustomerNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerEventPublisher eventPublisher;

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_customer:create')")
    public CustomerResponse createCustomer(CreateCustomerRequest req, String actorId) {
        Customer customer = Customer.builder()
                .customerCode(req.getCustomerCode())
                .customerName(req.getCustomerName())
                .customerType(req.getCustomerType() != null ? req.getCustomerType() : "RETAIL")
                .companyName(req.getCompanyName())
                .industry(req.getIndustry())
                .businessRegistrationNumber(req.getBusinessRegistrationNumber())
                .taxId(req.getTaxId())
                .websiteUrl(req.getWebsiteUrl())
                .emailAddress(req.getEmailAddress())
                .phoneNumber(req.getPhoneNumber())
                .paymentTerms(req.getPaymentTerms())
                .preferredCurrency(req.getPreferredCurrency() != null ? req.getPreferredCurrency() : "USD")
                .creditLimit(req.getCreditLimit())
                .segment(req.getSegment() != null ? req.getSegment() : "STANDARD")
                .riskRating(req.getRiskRating() != null ? req.getRiskRating() : "LOW")
                .accountManagerId(req.getAccountManagerId())
                .notes(req.getNotes())
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();

        try {
            customer = customerRepository.save(customer);
        } catch (DataIntegrityViolationException ex) {
            throw new CustomerCodeExistsException(req.getCustomerCode());
        }
        log.info("Customer created: code={} by={}", customer.getCustomerCode(), actorId);

        eventPublisher.publishCustomerCreated(new CustomerCreatedEvent(
                customer.getId(), customer.getCustomerCode(), customer.getCustomerName(),
                customer.getCustomerType(), customer.getSegment(), customer.getEmailAddress(),
                customer.getPhoneNumber(), null, actorId));

        return toResponse(customer);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_customer:read')")
    public CustomerResponse getCustomer(String customerId) {
        return toResponse(findById(customerId));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_customer:read')")
    public PagedResponse<CustomerSummaryResponse> listCustomers(
            String type, String segment, String status, String search, Pageable pageable) {

        Page<Customer> page = customerRepository.findWithFilters(type, segment, status, search, pageable);
        List<CustomerSummaryResponse> items = page.getContent().stream().map(this::toSummary).toList();

        return PagedResponse.<CustomerSummaryResponse>builder()
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
    @PreAuthorize("hasAuthority('PERMISSION_customer:write')")
    public CustomerResponse updateCustomer(String customerId, UpdateCustomerRequest req, String actorId) {
        Customer customer = findById(customerId);
        String previousSegment = customer.getSegment();

        if (req.getCustomerName() != null) customer.setCustomerName(req.getCustomerName());
        if (req.getCustomerType() != null) customer.setCustomerType(req.getCustomerType());
        if (req.getCompanyName() != null) customer.setCompanyName(req.getCompanyName());
        if (req.getIndustry() != null) customer.setIndustry(req.getIndustry());
        if (req.getTaxId() != null) customer.setTaxId(req.getTaxId());
        if (req.getWebsiteUrl() != null) customer.setWebsiteUrl(req.getWebsiteUrl());
        if (req.getEmailAddress() != null) customer.setEmailAddress(req.getEmailAddress());
        if (req.getPhoneNumber() != null) customer.setPhoneNumber(req.getPhoneNumber());
        if (req.getPaymentTerms() != null) customer.setPaymentTerms(req.getPaymentTerms());
        if (req.getPreferredCurrency() != null) customer.setPreferredCurrency(req.getPreferredCurrency());
        if (req.getCreditLimit() != null) customer.setCreditLimit(req.getCreditLimit());
        if (req.getSegment() != null) customer.setSegment(req.getSegment());
        if (req.getRiskRating() != null) customer.setRiskRating(req.getRiskRating());
        if (req.getAccountManagerId() != null) customer.setAccountManagerId(req.getAccountManagerId());
        if (req.getNotes() != null) customer.setNotes(req.getNotes());
        customer.setUpdatedBy(actorId);

        customer = customerRepository.save(customer);
        log.info("Customer updated: id={} by={}", customerId, actorId);

        eventPublisher.publishCustomerUpdated(new CustomerUpdatedEvent(
                customer.getId(), customer.getCustomerCode(), customer.getCustomerName(), actorId));

        if (req.getSegment() != null && !req.getSegment().equals(previousSegment)) {
            eventPublisher.publishCustomerSegmentChanged(new CustomerSegmentChangedEvent(
                    customer.getId(), customer.getCustomerCode(), customer.getCustomerName(),
                    previousSegment, req.getSegment(), actorId));
        }

        return toResponse(customer);
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_customer:write')")
    public CustomerResponse suspendCustomer(String customerId, SuspendCustomerRequest req, String actorId) {
        Customer customer = findById(customerId);
        customer.suspend(req.getReason(), req.getResumeDate());
        customer.setUpdatedBy(actorId);
        customer = customerRepository.save(customer);
        log.info("Customer suspended: id={} reason={} by={}", customerId, req.getReason(), actorId);

        eventPublisher.publishCustomerSuspended(new CustomerSuspendedEvent(
                customer.getId(), customer.getCustomerCode(), customer.getCustomerName(),
                req.getReason(), req.getResumeDate(), actorId));

        return toResponse(customer);
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_customer:write')")
    public CustomerResponse resumeCustomer(String customerId, String actorId) {
        Customer customer = findById(customerId);
        if (!customer.isSuspended()) {
            return toResponse(customer);
        }
        customer.activate();
        customer.setUpdatedBy(actorId);
        customer = customerRepository.save(customer);
        log.info("Customer resumed: id={} by={}", customerId, actorId);

        eventPublisher.publishCustomerResumed(new CustomerResumedEvent(
                customer.getId(), customer.getCustomerCode(), customer.getCustomerName(), actorId));

        return toResponse(customer);
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_customer:write')")
    public CustomerResponse deactivateCustomer(String customerId, String actorId) {
        Customer customer = findById(customerId);
        customer.deactivate();
        customer.setUpdatedBy(actorId);
        customer = customerRepository.save(customer);
        log.info("Customer deactivated: id={} by={}", customerId, actorId);
        return toResponse(customer);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_customer:read')")
    public PagedResponse<CustomerSummaryResponse> listBySegment(String segment, Pageable pageable) {
        Page<Customer> page = customerRepository.findBySegment(segment, pageable);
        List<CustomerSummaryResponse> items = page.getContent().stream().map(this::toSummary).toList();
        return PagedResponse.<CustomerSummaryResponse>builder()
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

    public Customer findById(String customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    private CustomerResponse toResponse(Customer c) {
        return CustomerResponse.builder()
                .id(c.getId())
                .customerCode(c.getCustomerCode())
                .customerName(c.getCustomerName())
                .customerType(c.getCustomerType())
                .status(c.resolveStatus())
                .segment(c.getSegment())
                .companyName(c.getCompanyName())
                .industry(c.getIndustry())
                .businessRegistrationNumber(c.getBusinessRegistrationNumber())
                .taxId(c.getTaxId())
                .websiteUrl(c.getWebsiteUrl())
                .emailAddress(c.getEmailAddress())
                .phoneNumber(c.getPhoneNumber())
                .paymentTerms(c.getPaymentTerms())
                .preferredCurrency(c.getPreferredCurrency())
                .creditLimit(c.getCreditLimit())
                .currentCreditBalance(c.getCurrentCreditBalance())
                .creditAvailable(c.getCreditAvailable())
                .totalOrders(c.getTotalOrders())
                .totalSpent(c.getTotalSpent())
                .averageOrderValue(c.getAverageOrderValue())
                .lifetimeValue(c.getLifetimeValue())
                .customerRating(c.getCustomerRating())
                .firstOrderDate(c.getFirstOrderDate())
                .lastOrderDate(c.getLastOrderDate())
                .riskRating(c.getRiskRating())
                .accountManagerId(c.getAccountManagerId())
                .isVerified(c.getIsVerified())
                .suspensionReason(c.getSuspensionReason())
                .suspendedAt(c.getSuspendedAt())
                .resumeDate(c.getResumeDate())
                .notes(c.getNotes())
                .createdBy(c.getCreatedBy())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private CustomerSummaryResponse toSummary(Customer c) {
        return CustomerSummaryResponse.builder()
                .id(c.getId())
                .customerCode(c.getCustomerCode())
                .customerName(c.getCustomerName())
                .customerType(c.getCustomerType())
                .status(c.resolveStatus())
                .segment(c.getSegment())
                .emailAddress(c.getEmailAddress())
                .phoneNumber(c.getPhoneNumber())
                .creditLimit(c.getCreditLimit())
                .creditAvailable(c.getCreditAvailable())
                .totalOrders(c.getTotalOrders())
                .totalSpent(c.getTotalSpent())
                .customerRating(c.getCustomerRating())
                .lastOrderDate(c.getLastOrderDate())
                .riskRating(c.getRiskRating())
                .isVerified(c.getIsVerified())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
