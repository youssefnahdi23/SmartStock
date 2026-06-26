package com.smartstock.purchase.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "quality_issues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "line_item_id", length = 36)
    private String lineItemId;

    @Column(name = "issue_type", nullable = false, length = 100)
    private String issueType;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "description")
    private String description;

    @Column(name = "severity", length = 50)
    private String severity;

    @Column(name = "proposed_resolution", length = 100)
    private String proposedResolution;

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "resolution_notes")
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by", length = 36)
    private String resolvedBy;

    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
