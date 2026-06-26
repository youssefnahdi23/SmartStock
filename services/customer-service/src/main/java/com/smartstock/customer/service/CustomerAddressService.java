package com.smartstock.customer.service;

import com.smartstock.customer.api.dto.request.CreateAddressRequest;
import com.smartstock.customer.api.dto.response.AddressResponse;
import com.smartstock.customer.domain.model.Customer;
import com.smartstock.customer.domain.model.CustomerAddress;
import com.smartstock.customer.domain.repository.CustomerAddressRepository;
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
public class CustomerAddressService {

    private final CustomerAddressRepository addressRepository;
    private final CustomerService customerService;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_customer:read')")
    public List<AddressResponse> listAddresses(String customerId) {
        customerService.findById(customerId);
        return addressRepository.findByCustomerIdAndIsActiveTrue(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_customer:write')")
    public AddressResponse addAddress(String customerId, CreateAddressRequest req, String actorId) {
        Customer customer = customerService.findById(customerId);

        CustomerAddress address = CustomerAddress.builder()
                .customer(customer)
                .label(req.getLabel())
                .addressLine1(req.getAddressLine1())
                .addressLine2(req.getAddressLine2())
                .city(req.getCity())
                .stateProvince(req.getStateProvince())
                .postalCode(req.getPostalCode())
                .countryCode(req.getCountryCode() != null ? req.getCountryCode() : "US")
                .addressType(req.getAddressType() != null ? req.getAddressType() : "SHIPPING")
                .isDefault(Boolean.TRUE.equals(req.getIsDefault()))
                .build();

        address = addressRepository.save(address);
        log.info("Address added: customerId={} addressId={} by={}", customerId, address.getId(), actorId);
        return toResponse(address);
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_customer:write')")
    public void deactivateAddress(String customerId, String addressId, String actorId) {
        customerService.findById(customerId);
        CustomerAddress address = addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new BusinessException("ADDRESS_NOT_FOUND",
                        "Address not found: " + addressId, HttpStatus.NOT_FOUND));
        address.setIsActive(false);
        addressRepository.save(address);
        log.info("Address deactivated: customerId={} addressId={} by={}", customerId, addressId, actorId);
    }

    private AddressResponse toResponse(CustomerAddress a) {
        return AddressResponse.builder()
                .id(a.getId())
                .customerId(a.getCustomer().getId())
                .label(a.getLabel())
                .addressLine1(a.getAddressLine1())
                .addressLine2(a.getAddressLine2())
                .city(a.getCity())
                .stateProvince(a.getStateProvince())
                .postalCode(a.getPostalCode())
                .countryCode(a.getCountryCode())
                .addressType(a.getAddressType())
                .isDefault(a.getIsDefault())
                .isActive(a.getIsActive())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
