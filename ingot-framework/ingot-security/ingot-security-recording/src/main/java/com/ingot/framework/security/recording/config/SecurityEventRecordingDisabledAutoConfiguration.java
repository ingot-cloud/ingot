package com.ingot.framework.security.recording.config;

import com.ingot.framework.security.recording.runtime.DisabledSecurityEventPublisher;
import com.ingot.framework.security.recording.spi.SecurityEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * <p>{@code ingot.security.event.enabled=false} 时的 Publisher 占位实现。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "ingot.security.event", name = "enabled", havingValue = "false")
@ConditionalOnMissingBean(SecurityEventPublisher.class)
public class SecurityEventRecordingDisabledAutoConfiguration {

    @Bean
    public SecurityEventPublisher disabledSecurityEventPublisher() {
        return new DisabledSecurityEventPublisher();
    }
}
