package com.ingot.framework.security.recording.store.log.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.recording.config.SecurityEventRecordingAutoConfiguration;
import com.ingot.framework.security.recording.spi.SecurityEventStore;
import com.ingot.framework.security.recording.store.log.LogSecurityEventStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * <p>结构化文件日志 Store 自动配置。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration
@AutoConfigureBefore(SecurityEventRecordingAutoConfiguration.class)
@EnableConfigurationProperties(LogStoreProperties.class)
@ConditionalOnProperty(prefix = "ingot.security.event.log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityEventLogStoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "logSecurityEventStore")
    public SecurityEventStore logSecurityEventStore(LogStoreProperties properties, ObjectMapper objectMapper)
            throws java.io.IOException {
        return new LogSecurityEventStore(properties, objectMapper);
    }
}
