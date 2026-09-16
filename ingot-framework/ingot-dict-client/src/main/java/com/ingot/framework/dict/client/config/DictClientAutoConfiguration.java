package com.ingot.framework.dict.client.config;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.iam.api.rpc.RemoteIamDictService;
import com.ingot.framework.cache.config.LayeredCacheBuilder;
import com.ingot.framework.cache.config.LayeredCacheSettings;
import com.ingot.framework.cache.registry.LayeredCacheRegistry;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.dict.client.DictService;
import com.ingot.framework.dict.client.internal.DictCacheCoordinator;
import com.ingot.framework.dict.client.internal.DictCacheKey;
import com.ingot.framework.dict.client.internal.LayeredDictService;
import com.ingot.framework.dict.client.model.DictItem;
import com.ingot.framework.dict.client.remote.RemoteDictService;
import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.config.EventBusAutoConfiguration;
import feign.Feign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * <p>字典客户端自动配置：按部署形态选择本地或 RPC delegate，再叠加 L1/L2。</p>
 *
 * <p>装配顺序：</p>
 * <ol>
 *     <li>L0 delegate：IAM 进程内由 {@code LocalDictConfig} 提供 {@code dictDelegate}；
 *         其它微服务由本类基于 {@link RemoteIamDictService} 注册 {@link RemoteDictService}。</li>
 *     <li>分层缓存：{@code mode=NONE} 时直接暴露 delegate；否则用 {@link LayeredCacheBuilder}
 *         按既有 {@code cache-*} / {@code redis-*} 键叠加 L1/L2，不启用 LKG 与地板。</li>
 *     <li>跨节点失效：{@code InvalidationBus} 存在且 {@code invalidation-enabled=true} 时注册
 *         {@link DictCacheCoordinator}。</li>
 * </ol>
 *
 * <p>本类必须排在 {@link EventBusAutoConfiguration} 之后，否则协调器会因总线尚未注册而被永久跳过。</p>
 *
 * @author jy
 * @since 2026/4/25
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(EventBusAutoConfiguration.class)
@EnableConfigurationProperties(DictClientProperties.class)
public class DictClientAutoConfiguration {

    public static final String DICT_DELEGATE_SERVICE_NAME = "dictDelegate";

    public static final String CACHE_BEAN_NAME = "dictItemCache";

    private static final String CACHE_NAME = "dict";
    private static final TypeReference<List<DictItem>> ITEM_LIST_TYPE = new TypeReference<>() {
    };

    @Bean(name = DICT_DELEGATE_SERVICE_NAME)
    @ConditionalOnClass(Feign.class)
    @ConditionalOnBean(RemoteIamDictService.class)
    @ConditionalOnMissingBean(name = DICT_DELEGATE_SERVICE_NAME)
    public DictService dictDelegate(RemoteIamDictService remoteIamDictService) {
        log.info("[DictClient] register remote delegate (RemoteDictService)");
        return new RemoteDictService(remoteIamDictService);
    }

    @Bean(CACHE_BEAN_NAME)
    @ConditionalOnBean(name = DICT_DELEGATE_SERVICE_NAME)
    @ConditionalOnMissingBean(name = CACHE_BEAN_NAME)
    public LayeredCache<DictCacheKey, List<DictItem>> dictItemCache(
            @Qualifier(DICT_DELEGATE_SERVICE_NAME) DictService delegate,
            DictClientProperties properties,
            ObjectProvider<StringRedisTemplate> redisProvider,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            ObjectProvider<LayeredCacheRegistry> registryProvider) {
        LayeredCacheSettings settings = LayeredCacheSettings.builder()
                .l1Enabled(properties.isCacheEnabled())
                .l1Ttl(properties.getCacheTtl())
                .l1MaximumSize(properties.getCacheMaximumSize())
                .l2Enabled(properties.isRedisEnabled())
                .l2Ttl(properties.getRedisTtl())
                .resilienceEnabled(false)
                .build();

        StringRedisTemplate redisTemplate = redisProvider.getIfAvailable();
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable();
        String prefix = properties.getRedisKeyPrefix();

        log.info("[DictClient] layered cache assembled (mode={}, l1={}, l2={})",
                properties.getMode(), properties.isCacheEnabled(), properties.isRedisEnabled());
        return LayeredCacheBuilder.<DictCacheKey, List<DictItem>>named(CACHE_NAME)
                .loader(key -> delegate.items(key.code(), key.toQuery()))
                .settings(settings)
                .cacheable(v -> v != null && !v.isEmpty())
                .emptyValue(List::of)
                .l2(redisTemplate, objectMapper, ITEM_LIST_TYPE,
                        key -> key.redisKey(prefix), DictCacheKey.redisAllPattern(prefix))
                .registry(registryProvider.getIfAvailable())
                .build();
    }

    @Bean
    @Primary
    @ConditionalOnBean(name = DICT_DELEGATE_SERVICE_NAME)
    public DictService dictService(@Qualifier(DICT_DELEGATE_SERVICE_NAME) DictService delegate,
                                   ObjectProvider<LayeredCache<DictCacheKey, List<DictItem>>> cacheProvider,
                                   DictClientProperties properties) {
        if (properties.getMode() == DictClientProperties.Mode.NONE) {
            log.info("[DictClient] mode=NONE, expose delegate without cache");
            return delegate;
        }
        LayeredCache<DictCacheKey, List<DictItem>> cache = cacheProvider.getIfAvailable();
        if (cache == null) {
            return delegate;
        }
        return new LayeredDictService(cache, properties.getRedisKeyPrefix());
    }

    @Bean
    @ConditionalOnBean(InvalidationBus.class)
    @ConditionalOnProperty(value = "ingot.dict.client.invalidation-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(DictCacheCoordinator.class)
    public DictCacheCoordinator dictCacheCoordinator(InvalidationBus bus,
                                                     @Qualifier("dictService") DictService dictService) {
        return new DictCacheCoordinator(bus, dictService);
    }
}
