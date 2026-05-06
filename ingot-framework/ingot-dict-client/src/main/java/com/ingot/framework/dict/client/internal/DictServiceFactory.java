package com.ingot.framework.dict.client.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.dict.client.DictService;
import com.ingot.framework.dict.client.config.DictClientProperties;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 字典服务装饰器组合工厂。组合顺序自外向内：
 * <pre>
 * Caffeine (L1)  -->  Redis (L2)  -->  delegate (Local/Remote)
 * </pre>
 *
 * @author jy
 * @since 2026/4/27
 */
public final class DictServiceFactory {

    private DictServiceFactory() {
    }

    /**
     * 仅构造 L2 层（不带 L1）。返回 {@code null} 表示按配置不启用 L2。
     */
    public static RedisDictService composeRedisLayer(DictService delegate,
                                                     DictClientProperties properties,
                                                     StringRedisTemplate redisTemplate,
                                                     ObjectMapper objectMapper) {
        if (properties.getMode() == DictClientProperties.Mode.NONE) {
            return null;
        }
        if (!properties.isRedisEnabled() || redisTemplate == null || objectMapper == null) {
            return null;
        }
        return new RedisDictService(delegate, redisTemplate, objectMapper, properties);
    }

    /**
     * 在已有装饰链外再叠加 L1 Caffeine 缓存。
     *
     * @param inner      下层链路（可能是 {@link RedisDictService} 或原始 delegate）
     * @param properties 客户端配置
     * @return 最终对外暴露的 {@link DictService}
     */
    public static DictService composeCaffeineLayer(DictService inner, DictClientProperties properties) {
        if (properties.getMode() == DictClientProperties.Mode.NONE) {
            return inner;
        }
        if (!properties.isCacheEnabled()) {
            return inner;
        }
        return new CaffeineDictService(inner, properties);
    }
}
