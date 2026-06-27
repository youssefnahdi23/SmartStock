package com.smartstock.common.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the Topics registry as the single source of truth for all Kafka topic names (C-4).
 *
 * <p>Asserts:
 * <ul>
 *   <li>Every constant follows the {@code {bounded-context}.events} naming convention.
 *   <li>All wire names are unique — two constants must not share a topic.
 *   <li>Known canonical values are exactly as specified, catching accidental renames.
 * </ul>
 */
@DisplayName("Topics registry — single source of truth (C-4)")
class TopicsRegistryTest {

    @Test
    @DisplayName("all topic constants follow the {context}.events naming convention")
    void allTopicsFollowNamingConvention() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Field f : Topics.class.getDeclaredFields()) {
            if (Modifier.isPublic(f.getModifiers()) && Modifier.isStatic(f.getModifiers())
                    && f.getType() == String.class) {
                String value = (String) f.get(null);
                if (!value.endsWith(".events")) {
                    violations.add(f.getName() + " = \"" + value + "\" (missing .events suffix)");
                }
            }
        }
        assertThat(violations)
                .as("All Topics constants must end with '.events'")
                .isEmpty();
    }

    @Test
    @DisplayName("all topic wire names are unique — no two constants share a topic")
    void allTopicNamesAreUnique() throws Exception {
        Set<String> seen = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (Field f : Topics.class.getDeclaredFields()) {
            if (Modifier.isPublic(f.getModifiers()) && Modifier.isStatic(f.getModifiers())
                    && f.getType() == String.class) {
                String value = (String) f.get(null);
                if (!seen.add(value)) {
                    duplicates.add(f.getName() + " = \"" + value + "\"");
                }
            }
        }
        assertThat(duplicates)
                .as("Each Topics constant must have a unique wire name")
                .isEmpty();
    }

    @Test
    @DisplayName("canonical wire names match the documented event catalog")
    void canonicalWireNames() {
        assertThat(Topics.INVENTORY_EVENTS).isEqualTo("inventory.events");
        assertThat(Topics.PRODUCT_EVENTS).isEqualTo("product.events");
        assertThat(Topics.WAREHOUSE_EVENTS).isEqualTo("warehouse.events");
        assertThat(Topics.SUPPLIER_EVENTS).isEqualTo("supplier.events");
        assertThat(Topics.CUSTOMER_EVENTS).isEqualTo("customer.events");
        assertThat(Topics.IDENTITY_EVENTS).isEqualTo("identity.events");
        assertThat(Topics.PURCHASE_ORDER_EVENTS).isEqualTo("purchase-order.events");
        assertThat(Topics.SALES_ORDER_EVENTS).isEqualTo("sales-order.events");
    }

    @Test
    @DisplayName("CUSTOMER_EVENTS is normalised — legacy 'events.customer' wire name is gone")
    void customerEventsIsNormalised() {
        assertThat(Topics.CUSTOMER_EVENTS)
                .as("The legacy inverted name 'events.customer' must not come back")
                .doesNotContain("events.customer")
                .startsWith("customer");
    }

    @Test
    @DisplayName("registry contains exactly the 8 documented bounded contexts")
    void registrySize() throws Exception {
        long count = 0;
        for (Field f : Topics.class.getDeclaredFields()) {
            if (Modifier.isPublic(f.getModifiers()) && Modifier.isStatic(f.getModifiers())
                    && f.getType() == String.class) {
                count++;
            }
        }
        assertThat(count)
                .as("Topics must declare exactly 8 constants — one per producing bounded context")
                .isEqualTo(8);
    }
}
