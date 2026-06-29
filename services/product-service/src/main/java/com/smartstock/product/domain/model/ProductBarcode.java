package com.smartstock.product.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_barcodes", indexes = {
        @Index(name = "idx_barcodes_value",      columnList = "barcode_value", unique = true),
        @Index(name = "idx_barcodes_product_id", columnList = "product_id"),
        @Index(name = "idx_barcodes_type",       columnList = "barcode_type")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductBarcode {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "barcode_value", nullable = false, unique = true, length = 255)
    private String barcodeValue;

    @Column(name = "barcode_type", nullable = false, length = 50)
    private String barcodeType;

    @Column(name = "barcode_format", length = 50)
    @Builder.Default
    private String barcodeFormat = "EAN13";

    @Column(name = "qr_code_data", columnDefinition = "TEXT")
    private String qrCodeData;

    @Column(name = "qr_code_image_url", length = 500)
    private String qrCodeImageUrl;

    @Column(name = "barcode_image_url", length = 500)
    private String barcodeImageUrl;

    @Column(name = "is_primary")
    @Builder.Default
    private boolean primary = false;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "scanned_count")
    @Builder.Default
    private int scannedCount = 0;

    @Column(name = "last_scanned_at")
    private LocalDateTime lastScannedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", nullable = false, updatable = false, length = 36)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
