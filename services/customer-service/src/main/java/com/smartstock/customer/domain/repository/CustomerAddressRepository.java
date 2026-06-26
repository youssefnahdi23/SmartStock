package com.smartstock.customer.domain.repository;

import com.smartstock.customer.domain.model.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, String> {

    List<CustomerAddress> findByCustomerIdAndIsActiveTrue(String customerId);

    Optional<CustomerAddress> findByIdAndCustomerId(String id, String customerId);

    List<CustomerAddress> findByCustomerIdAndAddressTypeAndIsActiveTrue(String customerId, String addressType);
}
