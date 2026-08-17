package com.ingot.framework.security.recording.config;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.recording.actuate.SecurityEventRecordingEndpoint;
import com.ingot.framework.security.recording.model.RecordingTarget;
import com.ingot.framework.security.recording.runtime.*;
import com.ingot.framework.security.recording.spool.FileSpoolAutoConfiguration;
import com.ingot.framework.security.recording.spi.RecordQueue;
import com.ingot.framework.security.recording.spi.SecurityEventPublisher;
import com.ingot.framework.security.recording.spi.SecurityEventStore;
import com.ingot.framework.security.recording.spi.SecurityEventTransport;
import com.ingot.framework.security.recording.support.SecurityEventRecordValidator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * <p>安全事件 recording 框架自动配置：Publisher、dispatcher、兼容映射与观测。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration
@AutoConfigureAfter(FileSpoolAutoConfiguration.class)
@ConditionalOnProperty(prefix = "ingot.security.event", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(SecurityEventProperties.class)
public class SecurityEventRecordingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RecordingMetrics recordingMetrics() {
        return new RecordingMetrics();
    }

    @Bean
    @ConditionalOnMissingBean
    public EffectiveRecordingConfig effectiveRecordingConfig(SecurityEventProperties properties) {
        return RecordingConfigResolver.resolve(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public PriorityClassifier priorityClassifier(SecurityEventProperties properties) {
        return new DefaultPriorityClassifier(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityEventRecordValidator securityEventRecordValidator(ObjectProvider<ObjectMapper> objectMapper) {
        return new SecurityEventRecordValidator(objectMapper.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean(SecurityEventPublisher.class)
    public SecurityEventPublisher securityEventPublisher(
            SecurityEventRecordingDispatcher dispatcher,
            SecurityEventRecordValidator validator,
            SecurityEventProperties properties) {
        TransactionalSecurityEventPublisher.CategoryGate gate = properties::isCategoryEnabled;
        return new TransactionalSecurityEventPublisher(dispatcher, validator, gate);
    }

    @Bean
    @ConditionalOnMissingBean
    public MemoryRecordQueue memoryRecordQueue(
            EffectiveRecordingConfig config,
            SecurityEventProperties properties) {
        int capacity = Math.max(config.memory().getQueueCapacity(), 1);
        return new MemoryRecordQueue(capacity, properties.getSourceModule());
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public SecurityEventRecordingDispatcher securityEventRecordingDispatcher(
            EffectiveRecordingConfig config,
            SecurityEventProperties properties,
            PriorityClassifier priorityClassifier,
            MemoryRecordQueue memoryQueue,
            RecordQueue<com.ingot.framework.security.recording.model.SecurityEventRecord> durableRecordQueue,
            RecordingMetrics metrics,
            List<SecurityEventStore> stores,
            ObjectProvider<SecurityEventTransport> transportProvider) {
        SecurityEventStore primaryStore = null;
        SecurityEventStore shadowLocalStore = null;
        SecurityEventTransport transport = transportProvider.getIfAvailable();

        if (config.primaryTarget() == RecordingTarget.LOCAL) {
            primaryStore = StoreSelector.selectPrimary(stores, properties.getPrimaryStore());
        } else if (config.shadowTargets().contains(RecordingTarget.LOCAL) && !stores.isEmpty()) {
            shadowLocalStore = StoreSelector.selectPrimary(stores, properties.getPrimaryStore());
        }

        return new SecurityEventRecordingDispatcher(
                config,
                properties,
                priorityClassifier,
                memoryQueue,
                durableRecordQueue,
                metrics,
                primaryStore,
                transport,
                shadowLocalStore);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    @ConditionalOnAvailableEndpoint
    @ConditionalOnBean(SecurityEventRecordingDispatcher.class)
    @ConditionalOnMissingBean
    public SecurityEventRecordingEndpoint securityEventRecordingEndpoint(
            EffectiveRecordingConfig config,
            RecordingMetrics metrics,
            MemoryRecordQueue memoryQueue,
            SecurityEventRecordingDispatcher dispatcher) {
        return new SecurityEventRecordingEndpoint(config, metrics, memoryQueue, dispatcher);
    }
}
