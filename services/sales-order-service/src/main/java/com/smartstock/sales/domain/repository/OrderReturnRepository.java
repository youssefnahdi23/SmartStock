package com.smartstock.sales.domain.repository;

import com.smartstock.sales.domain.model.OrderReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderReturnRepository extends JpaRepository<OrderReturn, String> {

    List<OrderReturn> findBySalesOrderId(String salesOrderId);

    Optional<OrderReturn> findByIdAndSalesOrderId(String id, String salesOrderId);
}
