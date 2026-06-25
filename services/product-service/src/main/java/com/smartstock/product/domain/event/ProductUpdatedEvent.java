package com.smartstock.product.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter @Setter @NoArgsConstructor @SuperBuilder
public class ProductUpdatedEvent extends DomainEvent {

    private String productId;
    private Map<String, Object> changes;
    private Map<String, Object> previousValues;
    private String updatedBy;

    public ProductUpdatedEvent(String productId, Map<String, Object> changes,
                                Map<String, Object> previousValues, String updatedBy) {
        super(productId, "Product", "product-service");
        this.productId = productId;
        this.changes = changes;
        this.previousValues = previousValues;
        this.updatedBy = updatedBy;
    }
}
