package com.smartstock.customer.domain.repository;

import com.smartstock.customer.domain.model.CustomerContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerContactRepository extends JpaRepository<CustomerContact, String> {

    List<CustomerContact> findByCustomerIdAndIsActiveTrue(String customerId);

    Optional<CustomerContact> findByIdAndCustomerId(String id, String customerId);

    boolean existsByCustomerIdAndIsPrimaryTrue(String customerId);
}
