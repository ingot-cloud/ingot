package com.ingot.framework.security.replay;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.security.replay.store.NonceStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link DefaultReplayGuard} 单元测试。
 *
 * @author ingot
 */
class DefaultReplayGuardTest {

    /**
     * 内存实现的 NonceStore，用于隔离 Redis 依赖验证防重放逻辑。
     */
    static class InMemoryNonceStore implements NonceStore {
        private final Set<String> seen = new HashSet<>();

        @Override
        public boolean tryAcquire(String key, Duration ttl) {
            return seen.add(key);
        }
    }

    private DefaultReplayGuard guard(ReplayProperties properties, NonceStore store) {
        return new DefaultReplayGuard(properties, store);
    }

    @Test
    void firstRequestPasses() {
        DefaultReplayGuard guard = guard(new ReplayProperties(), new InMemoryNonceStore());
        assertDoesNotThrow(() -> guard.check("crypto", "n1", System.currentTimeMillis()));
    }

    @Test
    void duplicateNonceRejected() {
        DefaultReplayGuard guard = guard(new ReplayProperties(), new InMemoryNonceStore());
        long ts = System.currentTimeMillis();
        guard.check("crypto", "n1", ts);
        BizException ex = assertThrows(BizException.class, () -> guard.check("crypto", "n1", ts));
        assertEquals(ReplayErrorCode.REPLAY_NONCE_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    void expiredTimestampRejected() {
        ReplayProperties properties = new ReplayProperties();
        properties.setClockSkew(Duration.ofMinutes(1));
        DefaultReplayGuard guard = guard(properties, new InMemoryNonceStore());
        long oldTs = System.currentTimeMillis() - Duration.ofMinutes(10).toMillis();
        BizException ex = assertThrows(BizException.class, () -> guard.check("crypto", "n1", oldTs));
        assertEquals(ReplayErrorCode.REPLAY_TIMESTAMP_EXPIRED.getCode(), ex.getCode());
    }

    @Test
    void differentNamespaceIsolated() {
        DefaultReplayGuard guard = guard(new ReplayProperties(), new InMemoryNonceStore());
        long ts = System.currentTimeMillis();
        guard.check("crypto", "n1", ts);
        assertDoesNotThrow(() -> guard.check("sign", "n1", ts));
    }

    @Test
    void disabledSkipsCheck() {
        ReplayProperties properties = new ReplayProperties();
        properties.setEnabled(false);
        DefaultReplayGuard guard = guard(properties, new InMemoryNonceStore());
        long ts = System.currentTimeMillis();
        guard.check("crypto", "n1", ts);
        assertDoesNotThrow(() -> guard.check("crypto", "n1", ts));
    }

    @Test
    void storeUnavailableFailClose() {
        NonceStore failing = (key, ttl) -> {
            throw new IllegalStateException("redis down");
        };
        DefaultReplayGuard guard = guard(new ReplayProperties(), failing);
        BizException ex = assertThrows(BizException.class,
                () -> guard.check("crypto", "n1", System.currentTimeMillis()));
        assertEquals(ReplayErrorCode.REPLAY_STORE_UNAVAILABLE.getCode(), ex.getCode());
    }

    @Test
    void storeUnavailableFailOpen() {
        ReplayProperties properties = new ReplayProperties();
        properties.setFailOpen(true);
        NonceStore failing = (key, ttl) -> {
            throw new IllegalStateException("redis down");
        };
        DefaultReplayGuard guard = guard(properties, failing);
        assertDoesNotThrow(() -> guard.check("crypto", "n1", System.currentTimeMillis()));
    }
}
