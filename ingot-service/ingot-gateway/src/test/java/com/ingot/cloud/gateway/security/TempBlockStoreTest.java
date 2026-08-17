package com.ingot.cloud.gateway.security;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link TempBlockStore#tryBlockFirst} 边沿语义单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class TempBlockStoreTest {

    @SuppressWarnings("unchecked")
    private final ReactiveValueOperations<String, String> valueOps = mock(ReactiveValueOperations.class);
    private final ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
    private TempBlockStore store;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
        ObjectProvider<ReactiveStringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(redis);
        store = new TempBlockStore(provider);
        ReflectionTestUtils.setField(store, "redisTemplate", redis);
    }

    @Test
    void tryBlockFirst_firstTrue_secondFalse() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(Mono.just(true))
                .thenReturn(Mono.just(false));

        assertTrue(Boolean.TRUE.equals(store.tryBlockFirst("IP", "1.1.1.1", "RATE_LIMIT", Duration.ofSeconds(60)).block()));
        assertFalse(Boolean.TRUE.equals(store.tryBlockFirst("IP", "1.1.1.1", "RATE_LIMIT", Duration.ofSeconds(60)).block()));
    }

    @Test
    void refreshTtl_delegatesExpire() {
        when(redis.expire(anyString(), eq(Duration.ofSeconds(30)))).thenReturn(Mono.just(true));
        assertTrue(Boolean.TRUE.equals(store.refreshTtl("IP", "1.1.1.1", Duration.ofSeconds(30)).block()));
    }
}
