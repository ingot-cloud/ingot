package com.ingot.framework.cache.internal;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.cache.support.FakeRedis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>{@link RedisCacheLayer} 的读写回填、空值拦截与单/多 key 清理行为验证。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class RedisCacheLayerTest {

    private static final TypeReference<List<String>> TYPE = new TypeReference<>() {
    };
    private static final Predicate<List<String>> NON_EMPTY = v -> v != null && !v.isEmpty();

    private FakeRedis redis;
    private ObjectMapper objectMapper;
    private AtomicInteger calls;
    private AtomicReference<List<String>> downstream;

    @BeforeEach
    void setUp() {
        redis = new FakeRedis();
        objectMapper = new ObjectMapper();
        calls = new AtomicInteger();
        downstream = new AtomicReference<>(List.of("a"));
    }

    private LayeredCache<String, List<String>> inner() {
        return new LayeredCache<>() {
            @Override
            public List<String> get(String key) {
                calls.incrementAndGet();
                return downstream.get();
            }

            @Override
            public void evict(String key) {
            }

            @Override
            public void evictAll() {
            }

            @Override
            public String name() {
                return "inner";
            }
        };
    }

    private LayeredCache<String, List<String>> singleKey() {
        return new RedisCacheLayer<>("test", inner(), redis.template(), objectMapper, TYPE,
                k -> "in:test:all", "in:test:all", Duration.ofMinutes(5), NON_EMPTY);
    }

    private LayeredCache<String, List<String>> multiKey() {
        return new RedisCacheLayer<>("test", inner(), redis.template(), objectMapper, TYPE,
                k -> "in:test:items:" + k, "in:test:items:*", Duration.ofMinutes(5), NON_EMPTY);
    }

    @Test
    @DisplayName("miss 时穿透并回填，二次读命中 Redis")
    void writesThroughAndHits() {
        LayeredCache<String, List<String>> layer = singleKey();

        assertThat(layer.get("all")).containsExactly("a");
        assertThat(redis.get("in:test:all")).isEqualTo("[\"a\"]");
        assertThat(layer.get("all")).containsExactly("a");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("不写入空集合")
    void doesNotCacheEmpty() {
        downstream.set(List.of());
        LayeredCache<String, List<String>> layer = singleKey();

        layer.get("all");

        assertThat(redis.has("in:test:all")).isFalse();
        layer.get("all");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("读到历史遗留的空值时删除该 key 并穿透")
    void deletesStaleEmptyValue() {
        redis.store().put("in:test:all", "[]");
        LayeredCache<String, List<String>> layer = singleKey();

        assertThat(layer.get("all")).containsExactly("a");
        assertThat(calls.get()).isEqualTo(1);
        assertThat(redis.get("in:test:all")).isEqualTo("[\"a\"]");
    }

    @Test
    @DisplayName("反序列化失败时降级为 miss 而非抛出")
    void malformedValueFallsThrough() {
        redis.store().put("in:test:all", "not-json");
        LayeredCache<String, List<String>> layer = singleKey();

        assertThat(layer.get("all")).containsExactly("a");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("单 key 场景 evictAll 直接删除固定键")
    void evictAllDeletesSingleKey() {
        LayeredCache<String, List<String>> layer = singleKey();
        layer.get("all");
        assertThat(redis.has("in:test:all")).isTrue();

        layer.evictAll();

        assertThat(redis.has("in:test:all")).isFalse();
    }

    @Test
    @DisplayName("多 key 场景 evict 只删目标键")
    void evictRemovesSingleEntry() {
        LayeredCache<String, List<String>> layer = multiKey();
        layer.get("k1");
        layer.get("k2");

        layer.evict("k1");

        assertThat(redis.has("in:test:items:k1")).isFalse();
        assertThat(redis.has("in:test:items:k2")).isTrue();
    }

    @Test
    @DisplayName("多 key 场景 evictAll 按前缀 SCAN 批量清理，不误删其他命名空间")
    void evictAllScansByPrefix() {
        LayeredCache<String, List<String>> layer = multiKey();
        layer.get("k1");
        layer.get("k2");
        redis.store().put("in:other:keep", "[\"x\"]");

        layer.evictAll();

        assertThat(redis.has("in:test:items:k1")).isFalse();
        assertThat(redis.has("in:test:items:k2")).isFalse();
        assertThat(redis.has("in:other:keep")).isTrue();
    }

    @Test
    @DisplayName("evictMatching 按 SCAN 模式只清指定前缀，不误删同命名空间的其他键")
    void evictMatchingScansSubset() {
        LayeredCache<String, List<String>> layer = new RedisCacheLayer<>("test", inner(),
                redis.template(), objectMapper, TYPE,
                k -> "in:test:items:" + k, "in:test:items:*", Duration.ofMinutes(5), NON_EMPTY);
        layer.get("user_status:PLATFORM");
        layer.get("user_status:TENANT");
        layer.get("other:PLATFORM");
        redis.store().put("in:other:keep", "[\"x\"]");

        layer.evictMatching(k -> k.startsWith("user_status:"), "in:test:items:user_status:*");

        assertThat(redis.has("in:test:items:user_status:PLATFORM")).isFalse();
        assertThat(redis.has("in:test:items:user_status:TENANT")).isFalse();
        assertThat(redis.has("in:test:items:other:PLATFORM")).isTrue();
        assertThat(redis.has("in:other:keep")).isTrue();
    }

    @Test
    @DisplayName("多 key 各自独立回填")
    void cachesPerKey() {
        LayeredCache<String, List<String>> layer = multiKey();

        layer.get("k1");
        layer.get("k2");
        layer.get("k1");

        assertThat(calls.get()).isEqualTo(2);
        assertThat(redis.size()).isEqualTo(2);
    }
}
