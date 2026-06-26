package com.smartstock.purchase.domain.repository;

import com.smartstock.purchase.domain.model.DeliveryTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryTrackingRepository extends JpaRepository<DeliveryTracking, String> {

    List<DeliveryTracking> findByPurchaseOrderId(String purchaseOrderId);

    Optional<DeliveryTracking> findTopByPurchaseOrderIdOrderByCreatedAtDesc(String purchaseOrderId);
}
