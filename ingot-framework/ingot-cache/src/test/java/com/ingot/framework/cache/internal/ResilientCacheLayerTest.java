package com.ingot.framework.cache.internal;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.cache.source.CacheSource;
import com.ingot.framework.cache.source.CacheSourceHolder;
import com.ingot.framework.cache.spi.CacheFloorSupplier;
import com.ingot.framework.cache.spi.CacheValueLoader;
import com.ingot.framework.cache.spi.RemoteUnavailableException;
import com.ingot.framework.cache.support.FakeRedis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <p>{@link ResilientCacheLayer} 降级阶梯与 LKG 生命周期的行为验证。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class ResilientCacheLayerTest {

    private static final String KEY = "all";
    private static final String LKG_KEY = "in:test:lkg";
    private static final TypeReference<List<String>> TYPE = new TypeReference<>() {
    };

    private FakeRedis redis;
    private ObjectMapper objectMapper;
    private CacheSourceHolder sourceHolder;
    private LastKnownGoodStore<String, List<String>> lkgStore;

    @BeforeEach
    void setUp() {
        redis = new FakeRedis();
        objectMapper = new ObjectMapper();
        sourceHolder = new CacheSourceHolder();
        lkgStore = new LastKnownGoodStore<>("test", redis.template(), objectMapper, TYPE,
                k -> LKG_KEY, null);
    }

    private ResilientCacheLayer<String, List<String>> layer(CacheValueLoader<String, List<String>> loader,
                                                            CacheFloorSupplier<String, List<String>> floor,
                                                            boolean floorEnabled) {
        return new ResilientCacheLayer<>("test", loader, lkgStore, floor, floorEnabled, List::of, sourceHolder);
    }

    @Test
    @DisplayName("远端成功时刷新 LKG 并标记 REMOTE")
    void remoteSuccessRefreshesLkg() {
        ResilientCacheLayer<String, List<String>> layer =
                layer(k -> List.of("a", "b"), k -> List.of("floor"), true);

        assertThat(layer.get(KEY)).containsExactly("a", "b");
        assertThat(sourceHolder.current()).isEqualTo(CacheSource.REMOTE);
        assertThat(redis.get(LKG_KEY)).isEqualTo("[\"a\",\"b\"]");
    }

    @Test
    @DisplayName("远端返回合法空时接受并刷新 LKG，不触发降级")
    void legitimateEmptyIsNotDegradation() {
        ResilientCacheLayer<String, List<String>> layer =
                layer(k -> List.of(), k -> List.of("floor"), true);

        assertThat(layer.get(KEY)).isEmpty();
        assertThat(sourceHolder.current()).isEqualTo(CacheSource.REMOTE);
        assertThat(sourceHolder.lastKnownGoodCount()).isZero();
        assertThat(sourceHolder.localFloorCount()).isZero();
        assertThat(redis.get(LKG_KEY)).isEqualTo("[]");
    }

    @Test
    @DisplayName("远端返回 null 时替换为空值工厂的结果")
    void nullFromLoaderBecomesEmptyValue() {
        ResilientCacheLayer<String, List<String>> layer =
                layer(k -> null, k -> List.of("floor"), true);

        assertThat(layer.get(KEY)).isEmpty();
        assertThat(sourceHolder.current()).isEqualTo(CacheSource.REMOTE);
    }

    @Test
    @DisplayName("远端不可用且有 LKG 时降级到 LKG")
    void degradesToLastKnownGood() {
        AtomicReference<Boolean> down = new AtomicReference<>(false);
        CacheValueLoader<String, List<String>> loader = k -> {
            if (down.get()) {
                throw new RemoteUnavailableException("down");
            }
            return List.of("fresh");
        };
        ResilientCacheLayer<String, List<String>> layer = layer(loader, k -> List.of("floor"), true);

        layer.get(KEY);
        down.set(true);

        assertThat(layer.get(KEY)).containsExactly("fresh");
        assertThat(sourceHolder.current()).isEqualTo(CacheSource.LAST_KNOWN_GOOD);
        assertThat(sourceHolder.lastKnownGoodCount()).isEqualTo(1);
        assertThat(sourceHolder.lastDegradeAt()).isNotNull();
    }

    @Test
    @DisplayName("远端不可用且无 LKG 时落地板")
    void degradesToLocalFloor() {
        ResilientCacheLayer<String, List<String>> layer = layer(k -> {
            throw new RemoteUnavailableException("down");
        }, k -> List.of("floor"), true);

        assertThat(layer.get(KEY)).containsExactly("floor");
        assertThat(sourceHolder.current()).isEqualTo(CacheSource.LOCAL_FLOOR);
        assertThat(sourceHolder.localFloorCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("地板禁用且无 LKG 时 fail-closed 上抛而非返回空")
    void failsClosedWhenFloorDisabled() {
        ResilientCacheLayer<String, List<String>> layer = layer(k -> {
            throw new RemoteUnavailableException("down");
        }, k -> List.of("floor"), false);

        assertThatThrownBy(() -> layer.get(KEY))
                .isInstanceOf(RemoteUnavailableException.class)
                .hasMessage("down");
    }

    @Test
    @DisplayName("evictAll 不清除 LKG，故障时仍可降级")
    void evictAllKeepsLastKnownGood() {
        AtomicReference<Boolean> down = new AtomicReference<>(false);
        ResilientCacheLayer<String, List<String>> layer = layer(k -> {
            if (down.get()) {
                throw new RemoteUnavailableException("down");
            }
            return List.of("fresh");
        }, k -> List.of("floor"), true);

        layer.get(KEY);
        layer.evictAll();
        layer.evict(KEY);

        assertThat(redis.has(LKG_KEY)).isTrue();

        down.set(true);
        assertThat(layer.get(KEY)).containsExactly("fresh");
        assertThat(sourceHolder.current()).isEqualTo(CacheSource.LAST_KNOWN_GOOD);
    }

    @Test
    @DisplayName("远端每次调用都真实穿透，本层不缓存")
    void doesNotCache() {
        AtomicInteger calls = new AtomicInteger();
        ResilientCacheLayer<String, List<String>> layer = layer(k -> {
            calls.incrementAndGet();
            return List.of("x");
        }, k -> List.of("floor"), true);

        layer.get(KEY);
        layer.get(KEY);

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("Redis 缺失时 LKG 静默不可用，直接落地板")
    void worksWithoutRedis() {
        LastKnownGoodStore<String, List<String>> noRedis =
                new LastKnownGoodStore<>("test", null, objectMapper, TYPE, k -> LKG_KEY, null);
        ResilientCacheLayer<String, List<String>> layer = new ResilientCacheLayer<>(
                "test", k -> {
            throw new RemoteUnavailableException("down");
        }, noRedis, k -> List.of("floor"), true, List::of, sourceHolder);

        assertThat(noRedis.available()).isFalse();
        assertThat(layer.get(KEY)).containsExactly("floor");
        assertThat(sourceHolder.current()).isEqualTo(CacheSource.LOCAL_FLOOR);
    }
}
