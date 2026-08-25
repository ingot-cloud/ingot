package com.ingot.framework.cache.internal;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import com.ingot.framework.cache.spi.LayeredCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>{@link CaffeineCacheLayer} 的命中、空值拦截与失效行为验证。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class CaffeineCacheLayerTest {

    private static final Predicate<List<String>> NON_EMPTY = v -> v != null && !v.isEmpty();

    /**
     * 记录下游调用次数的桩层。
     */
    private static final class CountingLayer implements LayeredCache<String, List<String>> {

        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicReference<List<String>> value;

        CountingLayer(List<String> initial) {
            this.value = new AtomicReference<>(initial);
        }

        @Override
        public List<String> get(String key) {
            calls.incrementAndGet();
            return value.get();
        }

        @Override
        public void evict(String key) {
        }

        @Override
        public void evictAll() {
        }

        @Override
        public String name() {
            return "counting";
        }
    }

    @Test
    @DisplayName("命中后不再穿透下游")
    void cachesHit() {
        CountingLayer inner = new CountingLayer(List.of("a"));
        LayeredCache<String, List<String>> layer =
                new CaffeineCacheLayer<>("test", inner, Duration.ofMinutes(5), 16, NON_EMPTY);

        layer.get("k");
        layer.get("k");

        assertThat(inner.calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("不缓存空集合，每次都穿透")
    void doesNotCacheEmpty() {
        CountingLayer inner = new CountingLayer(List.of());
        LayeredCache<String, List<String>> layer =
                new CaffeineCacheLayer<>("test", inner, Duration.ofMinutes(5), 16, NON_EMPTY);

        assertThat(layer.get("k")).isEmpty();
        assertThat(layer.get("k")).isEmpty();

        assertThat(inner.calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("空集合变为有值后立即命中新值")
    void picksUpValueAfterEmpty() {
        CountingLayer inner = new CountingLayer(List.of());
        LayeredCache<String, List<String>> layer =
                new CaffeineCacheLayer<>("test", inner, Duration.ofMinutes(5), 16, NON_EMPTY);

        layer.get("k");
        inner.value.set(List.of("a"));

        assertThat(layer.get("k")).containsExactly("a");
        assertThat(layer.get("k")).containsExactly("a");
        assertThat(inner.calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("TTL 到期后重新穿透下游，为广播丢失兜底")
    void expiresAfterTtl() throws InterruptedException {
        CountingLayer inner = new CountingLayer(List.of("a"));
        LayeredCache<String, List<String>> layer =
                new CaffeineCacheLayer<>("test", inner, Duration.ofMillis(50), 16, NON_EMPTY);

        layer.get("k");
        Thread.sleep(120);
        layer.get("k");

        assertThat(inner.calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("evict 与 evictAll 均清空本层并向下透传")
    void evictClearsLayer() {
        CountingLayer inner = new CountingLayer(List.of("a"));
        LayeredCache<String, List<String>> layer =
                new CaffeineCacheLayer<>("test", inner, Duration.ofMinutes(5), 16, NON_EMPTY);

        layer.get("k");
        layer.evict("k");
        layer.get("k");
        layer.evictAll();
        layer.get("k");

        assertThat(inner.calls.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("evictMatching 只清匹配的 L1 键")
    void evictMatchingClearsSubset() {
        CountingLayer inner = new CountingLayer(List.of("a"));
        LayeredCache<String, List<String>> layer =
                new CaffeineCacheLayer<>("test", inner, Duration.ofMinutes(5), 16, NON_EMPTY);

        layer.get("user_status:PLATFORM");
        layer.get("user_status:TENANT");
        layer.get("other:PLATFORM");

        layer.evictMatching(k -> k.startsWith("user_status:"), "in:dict:items:user_status:*");

        layer.get("user_status:PLATFORM");
        layer.get("other:PLATFORM");

        assertThat(inner.calls.get()).isEqualTo(4);
    }

    @Test
    @DisplayName("多 key 各自独立缓存")
    void cachesPerKey() {
        CountingLayer inner = new CountingLayer(List.of("a"));
        LayeredCache<String, List<String>> layer =
                new CaffeineCacheLayer<>("test", inner, Duration.ofMinutes(5), 16, NON_EMPTY);

        layer.get("k1");
        layer.get("k2");
        layer.get("k1");

        assertThat(inner.calls.get()).isEqualTo(2);
    }
}
