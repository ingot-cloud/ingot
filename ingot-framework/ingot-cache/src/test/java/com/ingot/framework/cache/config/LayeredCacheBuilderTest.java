package com.ingot.framework.cache.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.cache.coordinator.CacheRefreshPublisher;
import com.ingot.framework.cache.registry.LayeredCacheDescriptor;
import com.ingot.framework.cache.registry.LayeredCacheRegistry;
import com.ingot.framework.cache.source.CacheSource;
import com.ingot.framework.cache.source.CacheSourceHolder;
import com.ingot.framework.cache.spi.CacheValueLoader;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.cache.spi.RemoteUnavailableException;
import com.ingot.framework.cache.support.FakeRedis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <p>{@link LayeredCacheBuilder} 装饰器组合矩阵验证：任一层开关组合下链路都完整可用。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class LayeredCacheBuilderTest {

    private static final String KEY = "all";
    private static final String L2_KEY = "in:test:configs:all";
    private static final String LKG_KEY = "in:test:policy:lkg";
    private static final TypeReference<List<String>> TYPE = new TypeReference<>() {
    };

    private FakeRedis redis;
    private ObjectMapper objectMapper;
    private AtomicInteger loaderCalls;
    private AtomicReference<Boolean> remoteDown;

    @BeforeEach
    void setUp() {
        redis = new FakeRedis();
        objectMapper = new ObjectMapper();
        loaderCalls = new AtomicInteger();
        remoteDown = new AtomicReference<>(false);
    }

    private CacheValueLoader<String, List<String>> loader() {
        return key -> {
            loaderCalls.incrementAndGet();
            if (remoteDown.get()) {
                throw new RemoteUnavailableException("down");
            }
            return List.of("remote");
        };
    }

    private LayeredCacheBuilder<String, List<String>> builder(LayeredCacheSettings settings) {
        return LayeredCacheBuilder.<String, List<String>>named("test")
                .loader(loader())
                .settings(settings)
                .cacheable(v -> v != null && !v.isEmpty())
                .emptyValue(List::of);
    }

    private LayeredCacheBuilder<String, List<String>> full(LayeredCacheSettings settings) {
        return builder(settings)
                .resilientSingleKey(redis.template(), objectMapper, TYPE, LKG_KEY, k -> List.of("floor"))
                .l2SingleKey(redis.template(), objectMapper, TYPE, L2_KEY);
    }

    @Test
    @DisplayName("全部层启用：单次加载、L2 回填、LKG 写入")
    void allLayersEnabled() {
        LayeredCache<String, List<String>> cache = full(LayeredCacheSettings.defaults()).build();

        assertThat(cache.get(KEY)).containsExactly("remote");
        assertThat(cache.get(KEY)).containsExactly("remote");

        assertThat(loaderCalls.get()).isEqualTo(1);
        assertThat(redis.get(L2_KEY)).isEqualTo("[\"remote\"]");
        assertThat(redis.get(LKG_KEY)).isEqualTo("[\"remote\"]");
    }

    @Test
    @DisplayName("全部层关闭：每次直连加载器，不碰 Redis")
    void allLayersDisabled() {
        LayeredCacheSettings settings = LayeredCacheSettings.builder()
                .l1Enabled(false)
                .l2Enabled(false)
                .resilienceEnabled(false)
                .build();
        LayeredCache<String, List<String>> cache = full(settings).build();

        cache.get(KEY);
        cache.get(KEY);

        assertThat(loaderCalls.get()).isEqualTo(2);
        assertThat(redis.size()).isZero();
    }

    @Test
    @DisplayName("仅 L1：命中进程内缓存，Redis 无写入")
    void onlyL1() {
        LayeredCacheSettings settings = LayeredCacheSettings.builder()
                .l2Enabled(false)
                .resilienceEnabled(false)
                .build();
        LayeredCache<String, List<String>> cache = full(settings).build();

        cache.get(KEY);
        cache.get(KEY);

        assertThat(loaderCalls.get()).isEqualTo(1);
        assertThat(redis.size()).isZero();
    }

    @Test
    @DisplayName("仅 L2：命中 Redis，无 LKG 写入")
    void onlyL2() {
        LayeredCacheSettings settings = LayeredCacheSettings.builder()
                .l1Enabled(false)
                .resilienceEnabled(false)
                .build();
        LayeredCache<String, List<String>> cache = full(settings).build();

        cache.get(KEY);
        cache.get(KEY);

        assertThat(loaderCalls.get()).isEqualTo(1);
        assertThat(redis.has(L2_KEY)).isTrue();
        assertThat(redis.has(LKG_KEY)).isFalse();
    }

    @Test
    @DisplayName("仅 Resilient：不缓存但可降级")
    void onlyResilient() {
        LayeredCacheSettings settings = LayeredCacheSettings.builder()
                .l1Enabled(false)
                .l2Enabled(false)
                .build();
        LayeredCache<String, List<String>> cache = full(settings).build();

        cache.get(KEY);
        remoteDown.set(true);

        assertThat(cache.get(KEY)).containsExactly("remote");
        assertThat(loaderCalls.get()).isEqualTo(2);
        assertThat(redis.has(L2_KEY)).isFalse();
    }

    @Test
    @DisplayName("关闭弹性后远端异常直接上抛，链路仍完整装配")
    void resilienceDisabledPropagatesFailure() {
        LayeredCacheSettings settings = LayeredCacheSettings.builder()
                .l1Enabled(false)
                .l2Enabled(false)
                .resilienceEnabled(false)
                .build();
        LayeredCache<String, List<String>> cache = full(settings).build();
        remoteDown.set(true);

        assertThatThrownBy(() -> cache.get(KEY)).isInstanceOf(RemoteUnavailableException.class);
        assertThat(redis.has(LKG_KEY)).isFalse();
    }

    @Test
    @DisplayName("未配置 Redis 时 L2 与 LKG 自动跳过，地板仍生效")
    void redisAbsentDegradesGracefully() {
        LayeredCache<String, List<String>> cache = builder(LayeredCacheSettings.defaults())
                .resilientSingleKey(null, null, TYPE, LKG_KEY, k -> List.of("floor"))
                .l2SingleKey(null, null, TYPE, L2_KEY)
                .build();
        remoteDown.set(true);

        assertThat(cache.get(KEY)).containsExactly("floor");
    }

    @Test
    @DisplayName("evictAll 清 L1 与 L2 但保留 LKG")
    void evictAllKeepsLkg() {
        LayeredCache<String, List<String>> cache = full(LayeredCacheSettings.defaults()).build();
        cache.get(KEY);

        cache.evictAll();

        assertThat(redis.has(L2_KEY)).isFalse();
        assertThat(redis.has(LKG_KEY)).isTrue();

        remoteDown.set(true);
        assertThat(cache.get(KEY)).containsExactly("remote");
    }

    @Test
    @DisplayName("地板禁用且无 LKG 时整链 fail-closed")
    void failsClosedThroughWholeChain() {
        LayeredCacheSettings settings = LayeredCacheSettings.builder().localFloorEnabled(false).build();
        LayeredCache<String, List<String>> cache = full(settings).build();
        remoteDown.set(true);

        assertThatThrownBy(() -> cache.get(KEY)).isInstanceOf(RemoteUnavailableException.class);
    }

    @Test
    @DisplayName("刷新通知在 L1 未命中时发出，命中时不发")
    void publishesRefreshOnMissOnly() {
        List<List<String>> received = new ArrayList<>();
        CacheRefreshPublisher<List<String>> publisher = new CacheRefreshPublisher<>("test");
        publisher.addListener(received::add);

        LayeredCache<String, List<String>> cache = full(LayeredCacheSettings.defaults())
                .refreshPublisher(publisher)
                .build();

        cache.get(KEY);
        cache.get(KEY);
        cache.evictAll();
        cache.get(KEY);

        assertThat(received).hasSize(2);
    }

    @Test
    @DisplayName("构建时登记到注册表并共享来源持有者")
    void registersDescriptor() {
        LayeredCacheRegistry registry = new LayeredCacheRegistry();
        CacheSourceHolder holder = new CacheSourceHolder();

        LayeredCache<String, List<String>> cache = full(LayeredCacheSettings.defaults())
                .sourceHolder(holder)
                .registry(registry)
                .build();
        cache.get(KEY);

        LayeredCacheDescriptor descriptor = registry.find("test");
        assertThat(descriptor).isNotNull();
        assertThat(descriptor.l1Enabled()).isTrue();
        assertThat(descriptor.l2Enabled()).isTrue();
        assertThat(descriptor.resilienceEnabled()).isTrue();
        assertThat(descriptor.sourceHolder()).isSameAs(holder);
        assertThat(holder.current()).isEqualTo(CacheSource.REMOTE);
    }

    @Test
    @DisplayName("缺少加载器时构建失败")
    void loaderIsRequired() {
        assertThatThrownBy(() -> LayeredCacheBuilder.named("test").build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("loader is required");
    }

    @Test
    @DisplayName("L1 TTL 到期后重新穿透到远端，为广播丢失兜底")
    void l1TtlBoundsStaleness() throws InterruptedException {
        LayeredCacheSettings settings = LayeredCacheSettings.builder()
                .l1Ttl(Duration.ofMillis(50))
                .l2Enabled(false)
                .build();
        LayeredCache<String, List<String>> cache = full(settings).build();

        cache.get(KEY);
        Thread.sleep(120);
        cache.get(KEY);

        assertThat(loaderCalls.get()).isEqualTo(2);
    }
}
