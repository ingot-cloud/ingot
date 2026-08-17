package com.ingot.framework.security.account.domain.config;

import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import com.ingot.framework.security.account.domain.port.outbound.noop.NoOpAccountLockSignalPort;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * <p>无 Redis 信号实现时的 NoOp 回退（fail-open）。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration
@AutoConfigureAfter(AccountLockSignalAutoConfiguration.class)
@ConditionalOnMissingBean(AccountLockSignalPort.class)
public class AccountLockSignalNoOpAutoConfiguration {

    @Bean
    public AccountLockSignalPort noOpAccountLockSignalPort() {
        return new NoOpAccountLockSignalPort();
    }
}
