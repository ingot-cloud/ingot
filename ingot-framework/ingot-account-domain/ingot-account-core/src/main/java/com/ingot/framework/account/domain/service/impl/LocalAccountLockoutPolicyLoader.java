package com.ingot.framework.account.domain.service.impl;

import com.ingot.framework.account.domain.config.AccountDomainProperties;
import com.ingot.framework.account.domain.model.LockoutPolicy;
import com.ingot.framework.account.domain.service.AccountLockoutPolicyLoader;
import lombok.RequiredArgsConstructor;

/**
 * 本地（Nacos）账号锁定策略加载器。
 *
 * <p>{@code mode=local} 时生效，每次调用即时从 {@link AccountDomainProperties} 映射，
 * 不做进程内缓存，从而保证 Nacos 配置经 {@code ConfigurationPropertiesRebinder} 重绑定后，
 * 下次读取即可感知最新值（免冷启动刷新）。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class LocalAccountLockoutPolicyLoader implements AccountLockoutPolicyLoader {

    private final AccountDomainProperties properties;

    @Override
    public LockoutPolicy getLockoutPolicy() {
        AccountDomainProperties.LockoutPolicy source = properties.getLockout();
        return LockoutPolicy.builder()
                .enabled(source.isEnabled())
                .maxAttempts(source.getMaxAttempts())
                .lockDurationMinutes(source.getLockDurationMinutes())
                .attemptWindowMinutes(source.getAttemptWindowMinutes())
                .hintAfterAttempts(source.getHintAfterAttempts())
                .build();
    }
}
