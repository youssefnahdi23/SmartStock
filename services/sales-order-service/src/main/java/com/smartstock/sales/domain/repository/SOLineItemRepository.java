package com.smartstock.sales.domain.repository;

import com.smartstock.sales.domain.model.SOLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SOLineItemRepository extends JpaRepository<SOLineItem, String> {

    List<SOLineItem> findBySalesOrderId(String salesOrderId);

    Optional<SOLineItem> findByIdAndSalesOrderId(String id, String salesOrderId);
}
