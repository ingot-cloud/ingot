package com.ingot.framework.security.account.domain.service.impl;

import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.config.AccountDomainProperties;
import com.ingot.framework.security.account.domain.model.LockoutPolicy;
import com.ingot.framework.security.account.domain.service.AccountLockoutPolicyLoader;
import lombok.RequiredArgsConstructor;

/**
 * <p>本地（Nacos）账号锁定策略加载器。</p>
 *
 * <p>{@code mode=local} 时生效，每次调用即时从 {@link AccountDomainProperties} 映射，
 * 不做进程内缓存，从而保证 Nacos 经 {@code ConfigurationPropertiesRebinder} 重绑定后下次即可读到新值。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class LocalAccountLockoutPolicyLoader implements AccountLockoutPolicyLoader {

    private final AccountDomainProperties properties;

    @Override
    public LockoutPolicy getLockoutPolicy(UserTypeEnum userType) {
        return fromProperties(properties, userType);
    }

    /**
     * 把当前 Nacos 锁定配置映射为值对象。
     *
     * @param properties 账号域属性
     * @param userType   调用方用户类型，写入返回值便于快照对齐
     * @return 非空策略
     */
    public static LockoutPolicy fromProperties(AccountDomainProperties properties, UserTypeEnum userType) {
        AccountDomainProperties.LockoutPolicy source = properties.getLockout();
        return new LockoutPolicy(
                userType,
                source.isEnabled(),
                source.getMaxAttempts(),
                source.getLockDurationMinutes(),
                source.getAttemptWindowMinutes(),
                source.getHintAfterAttempts()
        );
    }
}
