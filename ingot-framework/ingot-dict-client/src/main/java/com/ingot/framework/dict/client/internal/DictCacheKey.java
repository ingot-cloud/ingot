package com.ingot.framework.dict.client.internal;

import com.ingot.framework.dict.client.model.DictQuery;
import com.ingot.framework.dict.client.model.DictScope;

/**
 * <p>字典分层缓存的复合键：同一 {@code dictCode} 在不同作用域 / 租户 / 应用 / 是否含禁用项下各占一条。</p>
 *
 * <p>L2 Redis key 格式与迁移前一致：
 * {@code <prefix>:dict:items:<code>:<scope>:<tenantId>:<appId>:<includeDisabled>}。</p>
 *
 * @param code             字典编码
 * @param scope            作用域，缺省 {@link DictScope#PLATFORM}
 * @param tenantId         租户 ID；平台级为 {@code null}
 * @param appId            应用 ID；非应用作用域为 {@code null}
 * @param includeDisabled  是否包含禁用项
 * @author jy
 * @since 1.0.0
 */
public record DictCacheKey(String code, DictScope scope, Long tenantId, Long appId, boolean includeDisabled) {

    static final String KEY_NAMESPACE = "dict:items";

    /**
     * 从查询条件构造缓存键；{@code query} 为空时按平台默认作用域。
     *
     * @param code  字典编码
     * @param query 查询条件，可为 {@code null}
     * @return 缓存键
     */
    public static DictCacheKey of(String code, DictQuery query) {
        DictScope scope = query == null || query.getScope() == null ? DictScope.PLATFORM : query.getScope();
        Long tenantId = query == null ? null : query.getTenantId();
        Long appId = query == null ? null : query.getAppId();
        boolean includeDisabled = query != null && query.isIncludeDisabled();
        return new DictCacheKey(code, scope, tenantId, appId, includeDisabled);
    }

    /**
     * 还原为查询条件，供加载器回源。
     *
     * @return 查询条件
     */
    public DictQuery toQuery() {
        return DictQuery.builder()
                .scope(scope)
                .tenantId(tenantId)
                .appId(appId)
                .includeDisabled(includeDisabled)
                .build();
    }

    /**
     * 拼出该键对应的 L2 Redis key。
     *
     * @param keyPrefix 配置项 {@code redis-key-prefix}
     * @return 完整 Redis key
     */
    public String redisKey(String keyPrefix) {
        return keyPrefix + ":" + KEY_NAMESPACE + ":" + code
                + ":" + scope.name()
                + ":" + (tenantId == null ? "_" : tenantId)
                + ":" + (appId == null ? "_" : appId)
                + ":" + (includeDisabled ? "1" : "0");
    }

    /**
     * 指定字典编码下全部 query 变体的 SCAN 模式。
     *
     * @param keyPrefix 配置项 {@code redis-key-prefix}
     * @param dictCode  字典编码
     * @return Redis SCAN 模式
     */
    public static String redisPattern(String keyPrefix, String dictCode) {
        return keyPrefix + ":" + KEY_NAMESPACE + ":" + dictCode + ":*";
    }

    /**
     * 本客户端全部字典 L2 key 的 SCAN 模式。
     *
     * @param keyPrefix 配置项 {@code redis-key-prefix}
     * @return Redis SCAN 模式
     */
    public static String redisAllPattern(String keyPrefix) {
        return keyPrefix + ":" + KEY_NAMESPACE + ":*";
    }
}
