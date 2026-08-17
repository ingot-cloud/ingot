package com.ingot.framework.security.account.adapter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.account.adapter.port.CompositeSecurityEventPort;
import com.ingot.framework.security.account.adapter.support.AccountSecurityEventRecordMapper;
import com.ingot.framework.security.account.domain.port.outbound.SecurityEventPort;
import com.ingot.framework.security.recording.config.SecurityEventProperties;
import com.ingot.framework.security.recording.config.SecurityEventRecordingAutoConfiguration;
import com.ingot.framework.security.recording.config.SecurityEventRecordingDisabledAutoConfiguration;
import com.ingot.framework.security.recording.spi.SecurityEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * <p>在 {@link SecurityEventPublisher} 与 {@link AccountSecurityEventRecordMapper} 就绪后注册
 * {@link CompositeSecurityEventPort}。</p>
 *
 * <p>须晚于 {@link AccountAdapterAutoConfiguration} 与 recording 自动配置，避免
 * {@code @ConditionalOnBean} 在组件扫描阶段误判依赖尚未注册。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration
@AutoConfigureAfter({
        AccountAdapterAutoConfiguration.class,
        SecurityEventRecordingAutoConfiguration.class,
        SecurityEventRecordingDisabledAutoConfiguration.class
})
@ConditionalOnBean(SecurityEventPublisher.class)
public class AccountSecurityEventPortAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AccountSecurityEventRecordMapper accountSecurityEventRecordMapper(
            SecurityEventProperties properties,
            ObjectMapper objectMapper) {
        return new AccountSecurityEventRecordMapper(properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(SecurityEventPort.class)
    public SecurityEventPort compositeSecurityEventPort(
            SecurityEventPublisher publisher,
            AccountSecurityEventRecordMapper recordMapper,
            SecurityEventProperties properties) {
        return new CompositeSecurityEventPort(publisher, recordMapper, properties);
    }
}
