package com.smartstock.customer.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "customer_contacts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerContact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "contact_name", nullable = false, length = 255)
    private String contactName;

    @Column(name = "contact_title", length = 100)
    private String contactTitle;

    @Column(name = "email_address", length = 255)
    private String emailAddress;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "mobile_number", length = 20)
    private String mobileNumber;

    @Column(name = "contact_type", length = 50)
    @Builder.Default
    private String contactType = "GENERAL";

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "preferred_contact_method", length = 50)
    private String preferredContactMethod;

    @Column(name = "last_contacted_at")
    private Instant lastContactedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
