package com.ingot.framework.security.access.service.impl;

import java.util.List;

import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.security.access.model.LoginFailurePolicy;
import com.ingot.framework.security.access.service.LoginFailurePolicyLoader;
import lombok.RequiredArgsConstructor;

/**
 * <p>登录失败策略的缓存入口，把 {@link LoginFailurePolicyLoader} 接到分层缓存链上。</p>
 *
 * <p>单 key 全量快照：{@link #CACHE_KEY} 本身无业务语义。{@link #evictAll()} 清 L1 与 L2，
 * 使失效广播真正生效（修复 Coordinator 空转）。LKG 不受影响。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class CachedLoginFailurePolicyLoader implements LoginFailurePolicyLoader {

    /**
     * 全量策略的缓存键；单 key 场景，取值本身无语义。
     */
    public static final String CACHE_KEY = "all";

    private final LayeredCache<String, List<LoginFailurePolicy>> cache;

    @Override
    public List<LoginFailurePolicy> loadAll() {
        List<LoginFailurePolicy> data = cache.get(CACHE_KEY);
        return data != null ? data : List.of();
    }

    @Override
    public void evictAll() {
        cache.evictAll();
    }
}
