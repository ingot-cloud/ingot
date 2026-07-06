package com.ingot.framework.security.credential.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.credential.config.CredentialCacheProperties;
import com.ingot.framework.security.credential.service.CredentialPolicyConfigService;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 凭证策略配置服务装饰器组合工厂。组合顺序自外向内：
 * <pre>
 * Caffeine (L1)  -->  Redis (L2)  -->  delegate (Local Mapper / Remote Feign)
 * </pre>
 *
 * @author jy
 * @since 2026/5/16
 */
public final class CredentialPolicyConfigServiceFactory {

    private CredentialPolicyConfigServiceFactory() {
    }

    /**
     * 仅构造 L2 层（不带 L1）。返回 {@code null} 表示按配置不启用 L2。
     */
    public static RedisCredentialPolicyConfigService composeRedisLayer(CredentialPolicyConfigService delegate,
                                                                       CredentialCacheProperties properties,
                                                                       StringRedisTemplate redisTemplate,
                                                                       ObjectMapper objectMapper) {
        if (!properties.isL2Enabled() || redisTemplate == null || objectMapper == null) {
            return null;
        }
        return new RedisCredentialPolicyConfigService(delegate, redisTemplate, objectMapper, properties);
    }

    /**
     * 在已有装饰链外再叠加 L1 Caffeine 缓存。
     *
     * @param inner      下层链路（可能是 {@link RedisCredentialPolicyConfigService} 或原始 delegate）
     * @param properties 配置项
     * @return 最终对外暴露的 {@link CredentialPolicyConfigService}
     */
    public static CredentialPolicyConfigService composeCaffeineLayer(CredentialPolicyConfigService inner,
                                                                     CredentialCacheProperties properties) {
        if (!properties.isL1Enabled()) {
            return inner;
        }
        return new CaffeineCredentialPolicyConfigService(inner, properties);
    }
}
