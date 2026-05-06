package com.ingot.framework.dict.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.pms.api.rpc.RemotePmsDictService;
import com.ingot.framework.dict.client.DictService;
import com.ingot.framework.dict.client.internal.DictCacheCoordinator;
import com.ingot.framework.dict.client.internal.DictServiceFactory;
import com.ingot.framework.dict.client.internal.RedisDictService;
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
 * 字典客户端自动配置。
 * <p>
 * 装配顺序：
 * <ol>
 *     <li>L0 delegate：PMS 进程内由 {@code LocalDictConfig} 提供 {@code dictDelegate}；
 *         其它微服务由本类基于 {@link RemotePmsDictService} 注册 {@link RemoteDictService}。</li>
 *     <li>L2 Redis：当 Redis 类路径存在且 {@code ingot.dict.client.redis-enabled=true} 时，
 *         注册 {@link RedisDictService} 包裹 delegate。</li>
 *     <li>L1 Caffeine：最外层，{@code ingot.dict.client.cache-enabled=true} 时启用，作为 {@code @Primary} 暴露。</li>
 *     <li>跨节点失效：{@code InvalidationBus} 存在且 {@code invalidation-enabled=true} 时注册 {@link DictCacheCoordinator}。</li>
 * </ol>
 * <p>
 * 本类<strong>必须</strong>排在 {@link EventBusAutoConfiguration} 之后：{@code dictCacheCoordinator}
 * bean 带有 {@code @ConditionalOnBean(InvalidationBus.class)}；若按 classpath 字母序早于
 * {@code eventbus} 包处理，会导致总线尚未注册、条件不成立，协调器 bean 被<strong>永久跳过</strong>，
 * 典型现象为「PMS 写库后本地缓存正常，其它微服务（如 AUTH）L1 永不失效」。
 * </p>
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

    /**
     * 远端 delegate（仅在没有本地 delegate 时启用，典型场景：非 PMS 微服务）
     */
    @Bean(name = DICT_DELEGATE_SERVICE_NAME)
    @ConditionalOnClass(Feign.class)
    @ConditionalOnBean(RemotePmsDictService.class)
    @ConditionalOnMissingBean(name = DICT_DELEGATE_SERVICE_NAME)
    public DictService dictDelegate(RemotePmsDictService remotePmsDictService) {
        log.info("[DictClient] register remote delegate (RemoteDictService)");
        return new RemoteDictService(remotePmsDictService);
    }

    /**
     * L2 Redis 共享缓存层。delegate 必须存在；Redis 不可用时跳过。
     */
    @Bean
    @ConditionalOnBean({StringRedisTemplate.class})
    @ConditionalOnProperty(value = "ingot.dict.client.redis-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(RedisDictService.class)
    public RedisDictService dictRedisLayer(@Qualifier("dictDelegate") DictService delegate,
                                           StringRedisTemplate redisTemplate,
                                           ObjectProvider<ObjectMapper> objectMapperProvider,
                                           DictClientProperties properties) {
        ObjectMapper mapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        RedisDictService layer = DictServiceFactory.composeRedisLayer(delegate, properties, redisTemplate, mapper);
        if (layer != null) {
            log.info("[DictClient] L2 Redis layer enabled, ttl={}, keyPrefix={}",
                    properties.getRedisTtl(), properties.getRedisKeyPrefix());
        }
        return layer;
    }

    /**
     * 对外暴露的 {@link DictService}：在 delegate 之上叠加 L2、L1。
     */
    @Bean
    @Primary
    @ConditionalOnBean(name = DICT_DELEGATE_SERVICE_NAME)
    public DictService dictService(@Qualifier(DICT_DELEGATE_SERVICE_NAME) DictService delegate,
                                   ObjectProvider<RedisDictService> redisLayerProvider,
                                   DictClientProperties properties) {
        DictService inner = redisLayerProvider.getIfAvailable();
        if (inner == null) {
            inner = delegate;
        }
        DictService composed = DictServiceFactory.composeCaffeineLayer(inner, properties);
        log.info("[DictClient] DictService composed (mode={}, l1={}, l2={})",
                properties.getMode(), properties.isCacheEnabled(), properties.isRedisEnabled());
        return composed;
    }

    /**
     * 失效广播协调器：订阅 {@link com.ingot.framework.dict.client.event.DictInvalidationEvent}，
     * 回调时调用根 {@link DictService}（L1 入口），由装饰器链向下逐层清缓存。
     */
    @Bean
    @ConditionalOnBean(InvalidationBus.class)
    @ConditionalOnProperty(value = "ingot.dict.client.invalidation-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(DictCacheCoordinator.class)
    public DictCacheCoordinator dictCacheCoordinator(InvalidationBus bus,
                                                     @Qualifier("dictService") DictService dictService) {
        return new DictCacheCoordinator(bus, dictService);
    }
}
