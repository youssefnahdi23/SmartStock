package com.smartstock.customer.service;

import com.smartstock.customer.api.dto.request.CreateContactRequest;
import com.smartstock.customer.api.dto.response.ContactResponse;
import com.smartstock.customer.domain.model.Customer;
import com.smartstock.customer.domain.model.CustomerContact;
import com.smartstock.customer.domain.repository.CustomerContactRepository;
import com.smartstock.customer.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerContactService {

    private final CustomerContactRepository contactRepository;
    private final CustomerService customerService;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_customer:read')")
    public List<ContactResponse> listContacts(String customerId) {
        customerService.findById(customerId);
        return contactRepository.findByCustomerIdAndIsActiveTrue(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_customer:write')")
    public ContactResponse addContact(String customerId, CreateContactRequest req, String actorId) {
        Customer customer = customerService.findById(customerId);

        CustomerContact contact = CustomerContact.builder()
                .customer(customer)
                .contactName(req.getContactName())
                .contactTitle(req.getContactTitle())
                .emailAddress(req.getEmailAddress())
                .phoneNumber(req.getPhoneNumber())
                .mobileNumber(req.getMobileNumber())
                .contactType(req.getContactType() != null ? req.getContactType() : "GENERAL")
                .isPrimary(Boolean.TRUE.equals(req.getIsPrimary()))
                .preferredContactMethod(req.getPreferredContactMethod())
                .build();

        contact = contactRepository.save(contact);
        log.info("Contact added: customerId={} contactId={} by={}", customerId, contact.getId(), actorId);
        return toResponse(contact);
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_customer:write')")
    public void deactivateContact(String customerId, String contactId, String actorId) {
        customerService.findById(customerId);
        CustomerContact contact = contactRepository.findByIdAndCustomerId(contactId, customerId)
                .orElseThrow(() -> new BusinessException("CONTACT_NOT_FOUND",
                        "Contact not found: " + contactId, HttpStatus.NOT_FOUND));
        contact.setIsActive(false);
        contactRepository.save(contact);
        log.info("Contact deactivated: customerId={} contactId={} by={}", customerId, contactId, actorId);
    }

    private ContactResponse toResponse(CustomerContact c) {
        return ContactResponse.builder()
                .id(c.getId())
                .customerId(c.getCustomer().getId())
                .contactName(c.getContactName())
                .contactTitle(c.getContactTitle())
                .emailAddress(c.getEmailAddress())
                .phoneNumber(c.getPhoneNumber())
                .mobileNumber(c.getMobileNumber())
                .contactType(c.getContactType())
                .isPrimary(c.getIsPrimary())
                .isActive(c.getIsActive())
                .preferredContactMethod(c.getPreferredContactMethod())
                .lastContactedAt(c.getLastContactedAt())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
