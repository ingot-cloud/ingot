package com.ingot.framework.security.account.adapter.policy;

import java.util.List;

import com.ingot.framework.cache.spi.CacheFloorSupplier;
import com.ingot.framework.security.account.domain.config.AccountDomainProperties;
import com.ingot.framework.security.account.domain.model.LockoutPolicy;
import com.ingot.framework.security.account.domain.service.impl.LocalAccountLockoutPolicyLoader;
import lombok.RequiredArgsConstructor;

/**
 * <p>Nacos 本地地板：把当前进程 {@link AccountDomainProperties#getLockout()} 编成非空单元素列表。</p>
 *
 * <p>仅在远端不可用且无 LKG 时被调用。单元素保证地板非空；Cached loader 在缺 userType 行时使用该元素。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote 每次调用都重新读取 Properties，从而反映 Nacos 动态刷新后的最新值。
 */
@RequiredArgsConstructor
public class LocalAccountLockoutFloorSupplier implements CacheFloorSupplier<String, List<LockoutPolicy>> {

    private final AccountDomainProperties properties;

    @Override
    public List<LockoutPolicy> get(String key) {
        return List.of(LocalAccountLockoutPolicyLoader.fromProperties(properties, null));
    }
}
