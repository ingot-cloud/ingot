package com.ingot.framework.security.account.domain.config;

import com.ingot.framework.commons.model.security.PolicySourceMode;
import com.ingot.framework.security.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.security.account.domain.port.outbound.UserAccountPort;
import com.ingot.framework.security.account.domain.port.outbound.UserCredentialPort;
import com.ingot.framework.security.account.domain.port.outbound.noop.NoOpLockStatePort;
import com.ingot.framework.security.account.domain.port.outbound.noop.NoOpUserAccountPort;
import com.ingot.framework.security.account.domain.port.outbound.noop.NoOpUserCredentialPort;
import com.ingot.framework.security.account.domain.service.AccountLockoutPolicyLoader;
import com.ingot.framework.security.account.domain.service.AccountUseCaseModule;
import com.ingot.framework.security.account.domain.service.impl.LocalAccountLockoutPolicyLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * 账号域自动配置
 * <p>
 * UseCase 实现通过 {@code @Service} 自动注入，
 * Port 接口的 NoOp 实现在没有具体实现时作为默认值生效。
 * </p>
 * <p>
 * 仅在 account-adapter 在类路径时启用，避免 BFF / Auth 等仅依赖 account-core
 * 读取锁定信号的进程误装配 {@code UnlockAccountUseCaseService} 等需要事务管理器的用例。
 * </p>
 *
 * @author jymot
 * @since 2026-02-13
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(name = "com.ingot.framework.security.account.adapter.port.DefaultLockStatePortAdapter")
@ComponentScan(basePackageClasses = AccountUseCaseModule.class)
@EnableConfigurationProperties(AccountDomainProperties.class)
public class AccountDomainAutoConfiguration {

    /**
     * 账号锁定策略加载器（seam）。
     * <p>本期仅提供 {@code local} 实现；{@code mode=remote} 但无远程实现（后续 change 提供）时，
     * 回退 {@code local} 并告警，保证可用性。远程实现由后续 change 以更高优先级 Bean 覆盖本默认。</p>
     */
    @Bean
    @ConditionalOnMissingBean(AccountLockoutPolicyLoader.class)
    public AccountLockoutPolicyLoader localAccountLockoutPolicyLoader(AccountDomainProperties properties) {
        if (properties.getMode() == PolicySourceMode.REMOTE) {
            log.warn("[AccountDomain] ingot.security.account.mode=remote 但当前未提供远程实现，回退 local。"
                    + "远程弹性阶梯由后续 change 接入。");
        }
        return new LocalAccountLockoutPolicyLoader(properties);
    }

    @Bean
    @ConditionalOnMissingBean(UserAccountPort.class)
    public UserAccountPort noOpUserAccountPort() {
        return new NoOpUserAccountPort();
    }

    @Bean
    @ConditionalOnMissingBean(UserCredentialPort.class)
    public UserCredentialPort noOpUserCredentialPort() {
        return new NoOpUserCredentialPort();
    }

    @Bean
    @ConditionalOnMissingBean(LockStatePort.class)
    public LockStatePort noOpLockStatePort() {
        return new NoOpLockStatePort();
    }
}
