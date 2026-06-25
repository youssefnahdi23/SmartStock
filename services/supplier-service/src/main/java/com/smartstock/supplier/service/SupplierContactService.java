package com.smartstock.supplier.service;

import com.smartstock.supplier.api.dto.request.CreateContactRequest;
import com.smartstock.supplier.api.dto.response.ContactResponse;
import com.smartstock.supplier.domain.model.Supplier;
import com.smartstock.supplier.domain.model.SupplierContact;
import com.smartstock.supplier.domain.repository.SupplierContactRepository;
import com.smartstock.supplier.exception.BusinessException;
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
public class SupplierContactService {

    private final SupplierContactRepository contactRepository;
    private final SupplierService supplierService;

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_supplier:write')")
    public ContactResponse addContact(String supplierId, CreateContactRequest req, String actorId) {
        Supplier supplier = supplierService.findById(supplierId);

        if (Boolean.TRUE.equals(req.getIsPrimary())) {
            contactRepository.findBySupplierIdAndIsPrimaryTrue(supplierId)
                    .ifPresent(existing -> {
                        existing.setIsPrimary(false);
                        contactRepository.save(existing);
                    });
        }

        SupplierContact contact = SupplierContact.builder()
                .supplier(supplier)
                .contactName(req.getContactName())
                .contactTitle(req.getContactTitle())
                .emailAddress(req.getEmailAddress())
                .phoneNumber(req.getPhoneNumber())
                .mobileNumber(req.getMobileNumber())
                .addressLine1(req.getAddressLine1())
                .addressLine2(req.getAddressLine2())
                .city(req.getCity())
                .stateProvince(req.getStateProvince())
                .postalCode(req.getPostalCode())
                .contactType(req.getContactType() != null ? req.getContactType() : "GENERAL")
                .isPrimary(Boolean.TRUE.equals(req.getIsPrimary()))
                .build();

        contact = contactRepository.save(contact);
        log.info("Contact added: supplierId={} contactId={} by={}", supplierId, contact.getId(), actorId);
        return toResponse(contact, supplierId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_supplier:read')")
    public List<ContactResponse> listContacts(String supplierId) {
        supplierService.findById(supplierId);
        return contactRepository.findBySupplierIdAndIsActiveTrue(supplierId).stream()
                .map(c -> toResponse(c, supplierId))
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_supplier:write')")
    public void deactivateContact(String supplierId, String contactId, String actorId) {
        SupplierContact contact = contactRepository.findById(contactId)
                .filter(c -> c.getSupplier() != null && supplierId.equals(c.getSupplier().getId()))
                .orElseThrow(() -> new BusinessException("CONTACT_NOT_FOUND",
                        "Contact not found: " + contactId, HttpStatus.NOT_FOUND));
        contact.setIsActive(false);
        contactRepository.save(contact);
        log.info("Contact deactivated: supplierId={} contactId={} by={}", supplierId, contactId, actorId);
    }

    private ContactResponse toResponse(SupplierContact c, String supplierId) {
        return ContactResponse.builder()
                .id(c.getId())
                .supplierId(supplierId)
                .contactName(c.getContactName())
                .contactTitle(c.getContactTitle())
                .emailAddress(c.getEmailAddress())
                .phoneNumber(c.getPhoneNumber())
                .mobileNumber(c.getMobileNumber())
                .addressLine1(c.getAddressLine1())
                .addressLine2(c.getAddressLine2())
                .city(c.getCity())
                .stateProvince(c.getStateProvince())
                .postalCode(c.getPostalCode())
                .contactType(c.getContactType())
                .isPrimary(c.getIsPrimary())
                .isActive(c.getIsActive())
                .lastContactedAt(c.getLastContactedAt())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
