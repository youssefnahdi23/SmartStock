package com.smartstock.common.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Applies common tags (service, environment) to every meter emitted by this JVM.
 * Tags are applied globally so Grafana dashboards can filter by service or environment
 * without custom PromQL label surgery.
 */
@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
public class ObservabilityAutoConfiguration {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTagsCustomizer(
            @Value("${spring.application.name:unknown}") String serviceName,
            @Value("${ENVIRONMENT:development}") String environment) {

        return registry -> registry.config()
                .commonTags(Tags.of(
                        Tag.of("service", serviceName),
                        Tag.of("environment", environment)
                ));
    }
}
