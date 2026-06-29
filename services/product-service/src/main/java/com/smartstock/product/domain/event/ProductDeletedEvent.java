package com.smartstock.product.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter @Setter @NoArgsConstructor @SuperBuilder
public class ProductDeletedEvent extends DomainEvent {

    private String productId;
    private String sku;
    private String name;
    private String reason;
    private String deactivatedBy;

    public ProductDeletedEvent(String productId, String sku, String name,
                                String reason, String deactivatedBy) {
        super(productId, "Product", "product-service");
        this.productId = productId;
        this.sku = sku;
        this.name = name;
        this.reason = reason;
        this.deactivatedBy = deactivatedBy;
    }
}
