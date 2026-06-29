package com.smartstock.common.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Registers a Prometheus gauge {@code smartstock_outbox_queue_depth} that reports
 * the number of PENDING records in the outbox table.
 *
 * Registered automatically by {@link OutboxAutoConfiguration} for every service
 * that has {@code smartstock.outbox.enabled=true}. The gauge allows the
 * {@code OutboxRelayLag} Prometheus alert to fire when relay throughput falls
 * behind event production.
 */
public class OutboxQueueDepthBinder implements MeterBinder {

    private final OutboxRepository repository;

    public OutboxQueueDepthBinder(OutboxRepository repository) {
        this.repository = repository;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("smartstock.outbox.queue.depth", repository, OutboxRepository::countPending)
                .description("Number of PENDING outbox records waiting to be published to Kafka")
                .register(registry);
    }
}
