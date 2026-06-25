package com.smartstock.product.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter @Setter @NoArgsConstructor @SuperBuilder
public class BarcodeGeneratedEvent extends DomainEvent {

    private String productId;
    private String sku;
    private String barcodeValue;
    private String barcodeFormat;
    private String generatedBy;

    public BarcodeGeneratedEvent(String productId, String sku, String barcodeValue,
                                  String barcodeFormat, String generatedBy) {
        super(productId, "Product", "product-service");
        this.productId = productId;
        this.sku = sku;
        this.barcodeValue = barcodeValue;
        this.barcodeFormat = barcodeFormat;
        this.generatedBy = generatedBy;
    }
}
