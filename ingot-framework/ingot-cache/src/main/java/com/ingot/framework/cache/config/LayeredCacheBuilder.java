package com.ingot.framework.cache.config;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.cache.coordinator.CacheRefreshPublisher;
import com.ingot.framework.cache.internal.CaffeineCacheLayer;
import com.ingot.framework.cache.internal.LastKnownGoodStore;
import com.ingot.framework.cache.internal.LoaderCacheLayer;
import com.ingot.framework.cache.internal.RedisCacheLayer;
import com.ingot.framework.cache.internal.RefreshNotifyingCacheLayer;
import com.ingot.framework.cache.internal.ResilientCacheLayer;
import com.ingot.framework.cache.registry.LayeredCacheDescriptor;
import com.ingot.framework.cache.registry.LayeredCacheRegistry;
import com.ingot.framework.cache.source.CacheSourceHolder;
import com.ingot.framework.cache.spi.CacheFloorSupplier;
import com.ingot.framework.cache.spi.CacheValueLoader;
import com.ingot.framework.cache.spi.LayeredCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * <p>分层缓存装饰器链的流式装配器，按固定顺序组合各层并处理可选层缺省。</p>
 *
 * <p>组合顺序自外向内固定为 {@code 刷新通知 → L1 → L2 → Resilient → loader}。
 * Resilient 位于最内侧是关键约定：降级值因此受上层 TTL 约束，远端恢复后能自动回到新鲜数据。
 * 刷新通知位于最外侧同样是约定：监听器回调中的回读必须命中 L1，否则会把一次加载放大成两次。</p>
 *
 * <p>任一可选层缺省（未配置、开关关闭、Redis 不可用）时直接跳过，链路结构保持完整，
 * 不会因少一层而导致装配失败。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * LayeredCache<String, List<DictItem>> cache = LayeredCacheBuilder
 *         .<String, List<DictItem>>named("dict")
 *         .loader(remoteLoader)
 *         .settings(settings)
 *         .cacheable(v -> v != null && !v.isEmpty())
 *         .emptyValue(List::of)
 *         .l2MultiKey(redisTemplate, objectMapper, new TypeReference<>() {}, "in:dict:items:")
 *         .registry(registry)
 *         .build();
 * }</pre>
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 * @author jy
 * @since 1.0.0
 * @see LayeredCacheSettings
 */
@Slf4j
public final class LayeredCacheBuilder<K, V> {

    private final String name;
    private CacheValueLoader<K, V> loader;
    private LayeredCacheSettings settings = LayeredCacheSettings.defaults();
    private Predicate<V> cacheable = java.util.Objects::nonNull;
    private Supplier<V> emptyValue;
    private CacheSourceHolder sourceHolder;
    private LastKnownGoodStore<K, V> lkgStore;
    private CacheFloorSupplier<K, V> floorSupplier;
    private StringRedisTemplate redisTemplate;
    private ObjectMapper objectMapper;
    private TypeReference<V> valueType;
    private Function<K, String> l2KeyResolver;
    private String l2EvictAllPattern;
    private CacheRefreshPublisher<V> refreshPublisher;
    private LayeredCacheRegistry registry;

    private LayeredCacheBuilder(String name) {
        this.name = name;
    }

    /**
     * 开始装配一个具名缓存。
     *
     * @param name 缓存实例名，用于日志与 Actuator 展示
     * @param <K>  缓存键类型
     * @param <V>  缓存值类型
     * @return 装配器
     */
    public static <K, V> LayeredCacheBuilder<K, V> named(String name) {
        return new LayeredCacheBuilder<>(name);
    }

    /**
     * 设置最内层真实数据加载器（必填）。
     */
    public LayeredCacheBuilder<K, V> loader(CacheValueLoader<K, V> loader) {
        this.loader = loader;
        return this;
    }

    /**
     * 设置各层调参；不调用时使用全默认值。
     */
    public LayeredCacheBuilder<K, V> settings(LayeredCacheSettings settings) {
        if (settings != null) {
            this.settings = settings;
        }
        return this;
    }

    /**
     * 设置「值是否值得缓存」的判定；默认仅排除 {@code null}。
     * <p>集合类值应传入排除空集合的谓词，避免把空结果固化成 stale empty。</p>
     */
    public LayeredCacheBuilder<K, V> cacheable(Predicate<V> cacheable) {
        if (cacheable != null) {
            this.cacheable = cacheable;
        }
        return this;
    }

    /**
     * 设置加载器返回 {@code null} 时的替代值工厂，用于把「合法空」表达为空集合或空对象。
     */
    public LayeredCacheBuilder<K, V> emptyValue(Supplier<V> emptyValue) {
        this.emptyValue = emptyValue;
        return this;
    }

    /**
     * 设置来源持有者；不指定时自动创建一个。
     */
    public LayeredCacheBuilder<K, V> sourceHolder(CacheSourceHolder sourceHolder) {
        this.sourceHolder = sourceHolder;
        return this;
    }

    /**
     * 启用降级阶梯，直接提供已构建的 LKG 存储与地板供给器。
     *
     * @param lkgStore      最近成功快照存储；{@code null} 表示不使用 LKG
     * @param floorSupplier 地板供给器；{@code null} 表示不使用地板
     */
    public LayeredCacheBuilder<K, V> resilient(LastKnownGoodStore<K, V> lkgStore,
                                               CacheFloorSupplier<K, V> floorSupplier) {
        this.lkgStore = lkgStore;
        this.floorSupplier = floorSupplier;
        return this;
    }

    /**
     * 启用降级阶梯，并按固定 Redis key 构建单 key 场景的 LKG 存储。
     *
     * @param redisTemplate Redis 模板；{@code null} 时 LKG 不可用
     * @param objectMapper  JSON 序列化器
     * @param valueType     值类型引用
     * @param lkgKey        LKG 的完整 Redis key
     * @param floorSupplier 地板供给器
     */
    public LayeredCacheBuilder<K, V> resilientSingleKey(StringRedisTemplate redisTemplate,
                                                        ObjectMapper objectMapper,
                                                        TypeReference<V> valueType,
                                                        String lkgKey,
                                                        CacheFloorSupplier<K, V> floorSupplier) {
        this.lkgStore = new LastKnownGoodStore<>(name, redisTemplate, objectMapper, valueType,
                k -> lkgKey, settings.getLkgTtl());
        this.floorSupplier = floorSupplier;
        return this;
    }

    /**
     * 配置 L2，键映射与清理范围由调用方完全掌控。
     *
     * @param evictAllPattern {@code evictAll} 的清理范围；含通配符走 SCAN，否则直接 DEL
     */
    public LayeredCacheBuilder<K, V> l2(StringRedisTemplate redisTemplate,
                                        ObjectMapper objectMapper,
                                        TypeReference<V> valueType,
                                        Function<K, String> keyResolver,
                                        String evictAllPattern) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.valueType = valueType;
        this.l2KeyResolver = keyResolver;
        this.l2EvictAllPattern = evictAllPattern;
        return this;
    }

    /**
     * 配置单 key 场景的 L2：所有键映射到同一个 Redis key，清理时直接 DEL。
     *
     * @param redisKey 完整 Redis key
     */
    public LayeredCacheBuilder<K, V> l2SingleKey(StringRedisTemplate redisTemplate,
                                                 ObjectMapper objectMapper,
                                                 TypeReference<V> valueType,
                                                 String redisKey) {
        return l2(redisTemplate, objectMapper, valueType, k -> redisKey, redisKey);
    }

    /**
     * 配置多 key 场景的 L2：Redis key 为 {@code 前缀 + 缓存键}，清理时按前缀 SCAN 批量删除。
     *
     * @param keyPrefix Redis key 前缀
     */
    public LayeredCacheBuilder<K, V> l2MultiKey(StringRedisTemplate redisTemplate,
                                                ObjectMapper objectMapper,
                                                TypeReference<V> valueType,
                                                String keyPrefix) {
        return l2(redisTemplate, objectMapper, valueType, k -> keyPrefix + k, keyPrefix + "*");
    }

    /**
     * 挂载刷新事件发布器，让无请求路径读者的组件也能感知数据更新。
     * <p>仅适用于单 key 的共享快照场景，见
     * {@link com.ingot.framework.cache.internal.RefreshNotifyingCacheLayer}。</p>
     */
    public LayeredCacheBuilder<K, V> refreshPublisher(CacheRefreshPublisher<V> refreshPublisher) {
        this.refreshPublisher = refreshPublisher;
        return this;
    }

    /**
     * 登记到统一注册表，使该实例出现在汇总 Actuator 端点中。
     */
    public LayeredCacheBuilder<K, V> registry(LayeredCacheRegistry registry) {
        this.registry = registry;
        return this;
    }

    /**
     * 组装装饰器链。
     *
     * @return 最外层缓存入口
     * @throws IllegalStateException 未设置 loader
     */
    public LayeredCache<K, V> build() {
        if (loader == null) {
            throw new IllegalStateException("[Cache:" + name + "] loader is required");
        }
        if (sourceHolder == null) {
            sourceHolder = new CacheSourceHolder();
        }

        boolean resilient = settings.isResilienceEnabled() && (lkgStore != null || floorSupplier != null);
        LayeredCache<K, V> chain = resilient
                ? new ResilientCacheLayer<>(name, loader, lkgStore, floorSupplier,
                settings.isLocalFloorEnabled(), emptyValue, sourceHolder)
                : new LoaderCacheLayer<>(name, loader, emptyValue, sourceHolder);

        boolean l2 = settings.isL2Enabled() && redisTemplate != null
                && objectMapper != null && valueType != null && l2KeyResolver != null;
        if (l2) {
            chain = new RedisCacheLayer<>(name, chain, redisTemplate, objectMapper, valueType,
                    l2KeyResolver, l2EvictAllPattern, settings.getL2Ttl(), cacheable);
        }

        boolean l1 = settings.isL1Enabled();
        if (l1) {
            chain = new CaffeineCacheLayer<>(name, chain, settings.getL1Ttl(),
                    settings.getL1MaximumSize(), cacheable);
        }

        if (refreshPublisher != null) {
            chain = new RefreshNotifyingCacheLayer<>(name, chain, refreshPublisher);
        }

        if (registry != null) {
            registry.register(new LayeredCacheDescriptor(name, l1, l2, resilient,
                    settings.isLocalFloorEnabled(), sourceHolder));
        }

        log.info("[Cache:{}] assembled l1={} l2={} resilience={} localFloor={}",
                name, l1, l2, resilient, settings.isLocalFloorEnabled());
        return chain;
    }

    /**
     * 当前配置下的来源持有者；{@link #build()} 前调用会即时创建，便于与 Actuator 共享同一实例。
     *
     * @return 来源持有者
     */
    public CacheSourceHolder resolveSourceHolder() {
        if (sourceHolder == null) {
            sourceHolder = new CacheSourceHolder();
        }
        return sourceHolder;
    }
}
