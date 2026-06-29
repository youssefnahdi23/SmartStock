package com.smartstock.sales.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class SalesOrderCreatedEvent extends DomainEvent {

    private String soNumber;
    private String customerId;
    private String customerName;
    private LocalDate orderDate;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private Integer totalQuantity;
    private List<OrderItemPayload> items;
    private String createdBy;

    public SalesOrderCreatedEvent(String soId, String soNumber, String customerId, String customerName,
                                   LocalDate orderDate, LocalDate dueDate,
                                   BigDecimal totalAmount, Integer totalQuantity,
                                   List<OrderItemPayload> items, String createdBy) {
        super(soId, "SalesOrder", "sales-order-service");
        this.soNumber = soNumber;
        this.customerId = customerId;
        this.customerName = customerName;
        this.orderDate = orderDate;
        this.dueDate = dueDate;
        this.totalAmount = totalAmount;
        this.totalQuantity = totalQuantity;
        this.items = items;
        this.createdBy = createdBy;
    }

    public record OrderItemPayload(String productId, int quantity, BigDecimal unitPrice) {}
}
