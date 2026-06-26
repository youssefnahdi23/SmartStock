package com.smartstock.sales.domain.repository;

import com.smartstock.sales.domain.model.OrderShipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderShipmentRepository extends JpaRepository<OrderShipment, String> {

    List<OrderShipment> findBySalesOrderId(String salesOrderId);

    Optional<OrderShipment> findByIdAndSalesOrderId(String id, String salesOrderId);
}
