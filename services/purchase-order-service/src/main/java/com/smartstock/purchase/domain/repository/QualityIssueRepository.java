package com.smartstock.purchase.domain.repository;

import com.smartstock.purchase.domain.model.QualityIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QualityIssueRepository extends JpaRepository<QualityIssue, String> {

    List<QualityIssue> findByPurchaseOrderId(String purchaseOrderId);
}
