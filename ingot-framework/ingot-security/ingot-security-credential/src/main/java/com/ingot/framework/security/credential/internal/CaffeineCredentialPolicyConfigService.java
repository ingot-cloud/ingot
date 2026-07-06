package com.ingot.framework.security.credential.internal;

import java.util.List;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ingot.cloud.security.api.model.vo.CredentialPolicyConfigVO;
import com.ingot.framework.security.credential.config.CredentialCacheProperties;
import com.ingot.framework.security.credential.service.CredentialPolicyConfigService;

/**
 * L1 进程内 Caffeine 缓存装饰器。
 * <p>
 * 单 key（{@link #CACHE_KEY}）设计：策略配置全表实体一次取，配合写后失效广播保证一致性。
 * 不缓存空列表：避免「无配置 -> 有配置」翻转期间长期命中 {@code []}。
 * </p>
 *
 * @author jy
 * @since 2026/5/16
 */
public class CaffeineCredentialPolicyConfigService implements CredentialPolicyConfigService {

    static final String CACHE_KEY = "all";

    private final CredentialPolicyConfigService delegate;
    private final Cache<String, List<CredentialPolicyConfigVO>> cache;
    private final boolean enabled;

    public CaffeineCredentialPolicyConfigService(CredentialPolicyConfigService delegate,
                                                 CredentialCacheProperties properties) {
        this.delegate = delegate;
        this.enabled = properties.isL1Enabled();
        this.cache = Caffeine.newBuilder()
                .maximumSize(properties.getL1MaximumSize())
                .expireAfterWrite(properties.getL1Ttl())
                .build();
    }

    @Override
    public List<CredentialPolicyConfigVO> getAll() {
        if (!enabled) {
            return delegate.getAll();
        }
        List<CredentialPolicyConfigVO> hit = cache.getIfPresent(CACHE_KEY);
        if (hit != null && !hit.isEmpty()) {
            return hit;
        }
        if (hit != null) {
            cache.invalidate(CACHE_KEY);
        }
        List<CredentialPolicyConfigVO> fresh = delegate.getAll();
        if (fresh != null && !fresh.isEmpty()) {
            cache.put(CACHE_KEY, fresh);
        }
        return fresh != null ? fresh : List.of();
    }

    @Override
    public void evictAll() {
        if (enabled) {
            cache.invalidateAll();
        }
        delegate.evictAll();
    }
}
