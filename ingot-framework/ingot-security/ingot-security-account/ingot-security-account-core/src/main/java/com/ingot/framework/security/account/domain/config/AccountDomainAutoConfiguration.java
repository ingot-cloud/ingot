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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * <p>账号域自动配置：扫描用例，并在缺少真实 Port 时注册 NoOp。</p>
 *
 * <p>仅在 account-adapter 在类路径时启用，避免 BFF / Auth 等仅依赖 account-core
 * 读取锁定信号的进程误装配需要事务管理器的用例。</p>
 *
 * <p>{@code mode=local}（缺省）装配 {@link LocalAccountLockoutPolicyLoader}；
 * {@code mode=remote} 由 adapter 提供更高优先级的分层缓存实现。</p>
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
     * 账号锁定策略本地加载器，仅 {@code mode=local}（或缺省）时注册。
     *
     * @param properties 账号域配置
     * @return 即时映射 Nacos 的 loader
     */
    @Bean
    @ConditionalOnMissingBean(AccountLockoutPolicyLoader.class)
    @ConditionalOnProperty(name = "ingot.security.account.mode",
            havingValue = PolicySourceMode.VALUE_LOCAL, matchIfMissing = true)
    public AccountLockoutPolicyLoader localAccountLockoutPolicyLoader(AccountDomainProperties properties) {
        log.info("[AccountDomain] ingot.security.account.mode=local, use LocalAccountLockoutPolicyLoader");
        return new LocalAccountLockoutPolicyLoader(properties);
    }

    /**
     * 用户账号 Port 的 NoOp 回退。
     *
     * @return 空实现
     */
    @Bean
    @ConditionalOnMissingBean(UserAccountPort.class)
    public UserAccountPort noOpUserAccountPort() {
        return new NoOpUserAccountPort();
    }

    /**
     * 用户凭证 Port 的 NoOp 回退。
     *
     * @return 空实现
     */
    @Bean
    @ConditionalOnMissingBean(UserCredentialPort.class)
    public UserCredentialPort noOpUserCredentialPort() {
        return new NoOpUserCredentialPort();
    }

    /**
     * 锁定状态 Port 的 NoOp 回退。
     *
     * @return 空实现
     */
    @Bean
    @ConditionalOnMissingBean(LockStatePort.class)
    public LockStatePort noOpLockStatePort() {
        return new NoOpLockStatePort();
    }
}
