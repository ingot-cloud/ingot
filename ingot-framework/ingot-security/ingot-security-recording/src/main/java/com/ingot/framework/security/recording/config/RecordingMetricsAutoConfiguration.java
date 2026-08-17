package com.ingot.framework.security.recording.config;

import com.ingot.framework.security.recording.runtime.RecordingMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * <p>将 {@link RecordingMetrics} 桥接为 Micrometer 指标。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration(after = MetricsAutoConfiguration.class)
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(RecordingMetrics.class)
public class RecordingMetricsAutoConfiguration {

    @Bean
    public MeterBinder securityEventRecordingMeterBinder(RecordingMetrics metrics) {
        return registry -> {
            registry.gauge("ingot.security.event.published", metrics, RecordingMetrics::getPublished);
            registry.gauge("ingot.security.event.accepted", metrics, RecordingMetrics::getAccepted);
            registry.gauge("ingot.security.event.persisted", metrics, RecordingMetrics::getPersisted);
            registry.gauge("ingot.security.event.dropped", metrics, RecordingMetrics::getDropped);
            registry.gauge("ingot.security.event.failed", metrics, RecordingMetrics::getFailed);
            registry.gauge("ingot.security.event.replayed", metrics, RecordingMetrics::getReplayed);
            registry.gauge("ingot.security.event.duplicate", metrics, RecordingMetrics::getDuplicate);
            registry.gauge("ingot.security.event.shadow_failures", metrics, RecordingMetrics::getShadowFailures);
        };
    }
}
