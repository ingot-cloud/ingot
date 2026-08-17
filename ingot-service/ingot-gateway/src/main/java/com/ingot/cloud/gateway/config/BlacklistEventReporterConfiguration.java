package com.ingot.cloud.gateway.config;

import com.ingot.cloud.gateway.security.BlacklistEventReporter;
import com.ingot.framework.security.recording.config.SecurityEventProperties;
import com.ingot.framework.security.recording.transport.feign.SecurityEventReportPublisher;
import com.ingot.framework.security.recording.transport.feign.config.SecurityEventReportPublisherAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>在 {@link SecurityEventReportPublisher} 就绪后注册 {@link BlacklistEventReporter}。</p>
 *
 * <p>须晚于 {@link SecurityEventReportPublisherAutoConfiguration}，避免 {@code @ConditionalOnBean}
 * 在组件扫描阶段误判 ReportPublisher 不存在。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "ingot.security.event", name = "enabled", havingValue = "true")
public class BlacklistEventReporterConfiguration {

    @Bean
    public BlacklistEventReporter blacklistEventReporter(
            ObjectProvider<SecurityEventReportPublisher> reportPublisher,
            SecurityEventProperties recordingProperties) {
        SecurityEventReportPublisher publisher = reportPublisher.getIfAvailable();
        if (publisher == null) {
            return null;
        }
        return new BlacklistEventReporter(publisher, recordingProperties);
    }
}
