package com.ingot.framework.security.account.adapter.policy;

import java.util.List;
import java.util.Objects;

import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.model.LockoutPolicy;
import com.ingot.framework.security.account.domain.service.AccountLockoutPolicyLoader;
import lombok.RequiredArgsConstructor;

/**
 * <p>账号锁定策略的缓存入口，把 {@link AccountLockoutPolicyLoader} 接到分层缓存链上。</p>
 *
 * <p>单 key 全量快照。按 {@code userType} 取行；缺行时回落快照第一行（地板单元素场景）。
 * {@link #evictAll()} 清 L1 与 L2，LKG 不受影响。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class CachedAccountLockoutPolicyLoader implements AccountLockoutPolicyLoader {

    /**
     * 全量策略的缓存键；单 key 场景，取值本身无语义。
     */
    public static final String CACHE_KEY = "all";

    private final LayeredCache<String, List<LockoutPolicy>> cache;

    @Override
    public LockoutPolicy getLockoutPolicy(UserTypeEnum userType) {
        List<LockoutPolicy> data = cache.get(CACHE_KEY);
        if (data == null || data.isEmpty()) {
            throw new AccountLockoutPolicyRemoteUnavailableException(
                    "Account lockout policy snapshot is empty");
        }
        return data.stream()
                .filter(item -> userType == null || Objects.equals(item.userType(), userType))
                .findFirst()
                .orElse(data.get(0));
    }

    @Override
    public void evictAll() {
        cache.evictAll();
    }
}
