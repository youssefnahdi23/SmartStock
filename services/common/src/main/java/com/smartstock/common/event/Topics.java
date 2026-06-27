package com.smartstock.common.event;

/**
 * Canonical Kafka topic names — the single source of truth shared by producers and
 * consumers (debt C-4). Wiring a listener to a string literal that no producer emits
 * silently breaks event-driven flows; referencing these constants on both sides makes
 * such a mismatch a compile-time concern and lets contract tests assert the pairing.
 *
 * <p>One topic per producing bounded context. The name is the producer's stream; any
 * consumer interested in that context subscribes to the same constant.
 */
public final class Topics {

    private Topics() {
    }

    /** Emitted by inventory-service. */
    public static final String INVENTORY_EVENTS = "inventory.events";

    /** Emitted by product-service. */
    public static final String PRODUCT_EVENTS = "product.events";

    /** Emitted by warehouse-service. */
    public static final String WAREHOUSE_EVENTS = "warehouse.events";

    /** Emitted by supplier-service. */
    public static final String SUPPLIER_EVENTS = "supplier.events";

    /** Emitted by customer-service. */
    public static final String CUSTOMER_EVENTS = "customer.events";

    /** Emitted by identity-service. */
    public static final String IDENTITY_EVENTS = "identity.events";

    /** Emitted by purchase-order-service. */
    public static final String PURCHASE_ORDER_EVENTS = "purchase-order.events";

    /** Emitted by sales-order-service. */
    public static final String SALES_ORDER_EVENTS = "sales-order.events";
}
