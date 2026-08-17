package com.ingot.framework.security.account.domain.config;

import com.ingot.framework.security.account.domain.port.outbound.SecurityEventPort;
import com.ingot.framework.security.account.domain.port.outbound.noop.NoOpSecurityEventPort;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * <p>recording / adapter 均未提供 {@link SecurityEventPort} 时的 NoOp 回退。</p>
 *
 * <p>须在 recording 与 {@code AccountSecurityEventPortAutoConfiguration} 之后评估，
 * 避免过早占位导致 {@code CompositeSecurityEventPort} 无法注册。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration
@AutoConfigureAfter(name = {
        "com.ingot.framework.security.recording.config.SecurityEventRecordingAutoConfiguration",
        "com.ingot.framework.security.recording.config.SecurityEventRecordingDisabledAutoConfiguration",
        "com.ingot.framework.security.account.adapter.config.AccountSecurityEventPortAutoConfiguration"
})
@ConditionalOnMissingBean(SecurityEventPort.class)
public class AccountSecurityEventPortNoOpAutoConfiguration {

    @Bean
    public SecurityEventPort noOpSecurityEventPort() {
        return new NoOpSecurityEventPort();
    }
}
