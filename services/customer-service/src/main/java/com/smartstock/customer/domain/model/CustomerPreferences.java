package com.smartstock.customer.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "customer_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    @Column(name = "receive_email_communications")
    @Builder.Default
    private Boolean receiveEmailCommunications = true;

    @Column(name = "receive_sms_communications")
    @Builder.Default
    private Boolean receiveSMSCommunications = false;

    @Column(name = "receive_order_notifications")
    @Builder.Default
    private Boolean receiveOrderNotifications = true;

    @Column(name = "receive_promotional_offers")
    @Builder.Default
    private Boolean receivePromotionalOffers = true;

    @Column(name = "preferred_contact_method", length = 50)
    @Builder.Default
    private String preferredContactMethod = "EMAIL";

    @Column(name = "preferred_communication_time", length = 50)
    private String preferredCommunicationTime;

    @Column(name = "newsletter_subscribed")
    @Builder.Default
    private Boolean newsletterSubscribed = true;

    @Column(name = "receive_phone_communications")
    @Builder.Default
    private Boolean receivePhoneCommunications = true;

    @Column(name = "receive_product_updates")
    @Builder.Default
    private Boolean receiveProductUpdates = true;

    @Column(name = "receive_shipping_updates")
    @Builder.Default
    private Boolean receiveShippingUpdates = true;

    @Column(name = "do_not_contact_until")
    private Instant doNotContactUntil;

    @Column(name = "communication_frequency", length = 50)
    @Builder.Default
    private String communicationFrequency = "AS_NEEDED";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
