package com.ingot.framework.dict.client.internal;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ingot.framework.dict.client.DictService;
import com.ingot.framework.dict.client.config.DictClientProperties;
import com.ingot.framework.dict.client.model.DictItem;
import com.ingot.framework.dict.client.model.DictQuery;
import com.ingot.framework.dict.client.model.DictScope;

/**
 * L1 进程内 Caffeine 缓存装饰器。装饰真实的 {@link DictService} 实现，
 * 命中时直接返回；未命中时调用 delegate 取数并写回。
 * <p>
 * <b>不缓存空列表</b>：避免字典从「无项」变为「有项」后，L1 长期命中
 * 此前的 {@code []} 而不再回源（与 {@link RedisDictService} 策略一致）。
 * </p>
 * <p>
 * {@link #evict} 与 {@link #evictAll} 会沿装饰器链继续传播给 delegate，
 * 以确保链路下游（例如 L2 Redis）也被同步清理。
 * </p>
 *
 * @author jy
 * @since 2026/4/25
 */
public class CaffeineDictService implements DictService {

    private final DictService delegate;
    private final Cache<CacheKey, List<DictItem>> cache;
    private final boolean enabled;

    public CaffeineDictService(DictService delegate, DictClientProperties properties) {
        this.delegate = delegate;
        this.enabled = properties.isCacheEnabled();
        this.cache = Caffeine.newBuilder()
                .maximumSize(properties.getCacheMaximumSize())
                .expireAfterWrite(properties.getCacheTtl())
                .build();
    }

    @Override
    public List<DictItem> items(String dictCode, DictQuery query) {
        if (!enabled) {
            return delegate.items(dictCode, query);
        }
        CacheKey key = CacheKey.of(dictCode, query);
        List<DictItem> hit = cache.getIfPresent(key);
        if (hit != null && !hit.isEmpty()) {
            return hit;
        }
        if (hit != null) {
            cache.invalidate(key);
        }
        List<DictItem> fresh = delegate.items(dictCode, query);
        if (fresh != null && !fresh.isEmpty()) {
            cache.put(key, fresh);
        }
        return fresh != null ? fresh : List.of();
    }

    @Override
    public Map<String, List<DictItem>> batchItems(List<String> dictCodes, DictQuery query) {
        if (dictCodes == null || dictCodes.isEmpty()) {
            return Map.of();
        }
        if (!enabled) {
            return delegate.batchItems(dictCodes, query);
        }
        Map<String, List<DictItem>> result = new LinkedHashMap<>();
        List<String> missing = new java.util.ArrayList<>();
        for (String code : dictCodes) {
            CacheKey key = CacheKey.of(code, query);
            List<DictItem> hit = cache.getIfPresent(key);
            if (hit != null && !hit.isEmpty()) {
                result.put(code, hit);
            } else {
                if (hit != null) {
                    cache.invalidate(key);
                }
                missing.add(code);
            }
        }
        if (!missing.isEmpty()) {
            Map<String, List<DictItem>> fetched = delegate.batchItems(missing, query);
            fetched.forEach((code, list) -> {
                if (list != null && !list.isEmpty()) {
                    cache.put(CacheKey.of(code, query), list);
                }
                result.put(code, list != null ? list : List.of());
            });
        }
        Map<String, List<DictItem>> ordered = new LinkedHashMap<>(dictCodes.size());
        for (String code : dictCodes) {
            ordered.put(code, result.getOrDefault(code, List.of()));
        }
        return ordered;
    }

    @Override
    public void evict(String dictCode) {
        if (enabled && dictCode != null) {
            cache.asMap().keySet().removeIf(key -> dictCode.equals(key.code));
        }
        // 沿装饰器链向下传播
        delegate.evict(dictCode);
    }

    @Override
    public void evictAll() {
        if (enabled) {
            cache.invalidateAll();
        }
        delegate.evictAll();
    }

    private record CacheKey(String code, DictScope scope, Long tenantId, Long appId, boolean includeDisabled) {
        static CacheKey of(String code, DictQuery query) {
            DictScope scope = query == null || query.getScope() == null ? DictScope.PLATFORM : query.getScope();
            Long tenantId = query == null ? null : query.getTenantId();
            Long appId = query == null ? null : query.getAppId();
            boolean includeDisabled = query != null && query.isIncludeDisabled();
            return new CacheKey(code, scope, tenantId, appId, includeDisabled);
        }
    }

    /**
     * 透传 batchItems 的默认实现：当 delegate 没有实现批量接口时，使用 items 单次调用。
     */
    public static Map<String, List<DictItem>> batchByItems(DictService delegate,
                                                           List<String> dictCodes,
                                                           DictQuery query) {
        Map<String, List<DictItem>> map = new HashMap<>();
        for (String code : dictCodes) {
            map.put(code, delegate.items(code, query));
        }
        return map;
    }
}
