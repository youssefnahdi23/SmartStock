package com.smartstock.supplier.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "supplier_risk_assessment",
        uniqueConstraints = @UniqueConstraint(name = "uq_risk_assessment", columnNames = {"supplier_id", "assessment_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierRiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "supplier_id", nullable = false, length = 36)
    private String supplierId;

    @Column(name = "assessment_date", nullable = false)
    private LocalDate assessmentDate;

    @Column(name = "financial_health_score", precision = 3, scale = 2)
    private BigDecimal financialHealthScore;

    @Column(name = "delivery_reliability_score", precision = 3, scale = 2)
    private BigDecimal deliveryReliabilityScore;

    @Column(name = "quality_consistency_score", precision = 3, scale = 2)
    private BigDecimal qualityConsistencyScore;

    @Column(name = "communication_responsiveness_score", precision = 3, scale = 2)
    private BigDecimal communicationResponsivenessScore;

    @Column(name = "compliance_score", precision = 3, scale = 2)
    private BigDecimal complianceScore;

    @Column(name = "overall_risk_score", precision = 3, scale = 2)
    private BigDecimal overallRiskScore;

    @Column(name = "risk_level", length = 50)
    private String riskLevel;

    @Column(name = "key_risks")
    private String keyRisks;

    @Column(name = "mitigation_actions")
    private String mitigationActions;

    @Column(name = "next_assessment_date")
    private LocalDate nextAssessmentDate;

    @Column(name = "assessed_by", nullable = false, length = 36)
    private String assessedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
