package com.smartstock.customer.unit;

import com.smartstock.common.consumer.IdempotencyService;
import com.smartstock.common.event.Topics;
import com.smartstock.customer.domain.model.Customer;
import com.smartstock.customer.domain.repository.CustomerRepository;
import com.smartstock.customer.event.SalesOrderEventPayload;
import com.smartstock.customer.service.OrderEventListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.KafkaListener;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventListener — sales-order completion wiring (C-4)")
class OrderEventListenerTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private OrderEventListener listener;

    private SalesOrderEventPayload completed(String customerId, BigDecimal amount) {
        return new SalesOrderEventPayload("evt-1", "DeliveryCompletedEvent", customerId, amount);
    }

    @Test
    @DisplayName("records spend on DeliveryCompletedEvent for a known customer")
    void recordsSpendOnCompletion() {
        Customer customer = mock(Customer.class);
        when(idempotencyService.claim(anyString(), anyString())).thenReturn(true);
        when(customerRepository.findById("cust-1")).thenReturn(Optional.of(customer));

        listener.onSalesOrderEvent(completed("cust-1", new BigDecimal("250.00")));

        verify(customer).recordOrder(new BigDecimal("250.00"));
        verify(customerRepository).save(customer);
    }

    @Test
    @DisplayName("redelivered event is skipped — no double-count (idempotency, H-3)")
    void redeliveryIsIdempotent() {
        when(idempotencyService.claim(anyString(), anyString())).thenReturn(false);

        listener.onSalesOrderEvent(completed("cust-1", new BigDecimal("250.00")));

        verify(customerRepository, never()).findById(any());
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("ignores non-completion event types (no double-count on create/confirm)")
    void ignoresOtherEventTypes() {
        listener.onSalesOrderEvent(
                new SalesOrderEventPayload("evt-2", "SalesOrderCreatedEvent", "cust-1", new BigDecimal("999")));

        verifyNoInteractions(customerRepository);
    }

    @Test
    @DisplayName("ignores completion events missing customerId or totalAmount")
    void ignoresIncompletePayloads() {
        listener.onSalesOrderEvent(completed(null, new BigDecimal("10")));
        listener.onSalesOrderEvent(completed("cust-1", null));

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("unknown customer does not persist anything")
    void unknownCustomerNoOp() {
        when(idempotencyService.claim(anyString(), anyString())).thenReturn(true);
        when(customerRepository.findById("ghost")).thenReturn(Optional.empty());

        listener.onSalesOrderEvent(completed("ghost", new BigDecimal("10")));

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("CONTRACT: listener subscribes to the canonical sales-order topic")
    void subscribesToCanonicalTopic() throws NoSuchMethodException {
        Method m = OrderEventListener.class.getMethod("onSalesOrderEvent", SalesOrderEventPayload.class);
        KafkaListener annotation = m.getAnnotation(KafkaListener.class);

        assertThat(annotation).isNotNull();
        // The SpEL expression must resolve to the shared registry constant the producer uses.
        assertThat(annotation.topics()).hasSize(1);
        assertThat(annotation.topics()[0]).contains("Topics).SALES_ORDER_EVENTS");
        assertThat(Topics.SALES_ORDER_EVENTS).isEqualTo("sales-order.events");
    }
}
