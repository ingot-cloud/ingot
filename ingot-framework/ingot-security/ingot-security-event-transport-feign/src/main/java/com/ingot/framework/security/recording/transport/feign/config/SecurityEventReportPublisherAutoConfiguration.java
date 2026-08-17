package com.ingot.framework.security.recording.transport.feign.config;

import com.ingot.framework.security.recording.config.SecurityEventRecordingAutoConfiguration;
import com.ingot.framework.security.recording.spi.SecurityEventPublisher;
import com.ingot.framework.security.recording.transport.feign.SecurityEventReportPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * <p>在 {@link SecurityEventPublisher} 就绪后注册 {@link SecurityEventReportPublisher}。</p>
 *
 * <p>须晚于 {@link SecurityEventRecordingAutoConfiguration}，避免 {@code @ConditionalOnBean} 在
 * Feign Transport 同批自动配置阶段误判 Publisher 不存在。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration
@AutoConfigureAfter(SecurityEventRecordingAutoConfiguration.class)
@ConditionalOnBean(SecurityEventPublisher.class)
public class SecurityEventReportPublisherAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SecurityEventReportPublisher securityEventReportPublisher(SecurityEventPublisher publisher) {
        return new SecurityEventReportPublisher(publisher);
    }
}
