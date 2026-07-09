package com.ingot.framework.security.replay;

import java.time.Duration;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.security.replay.store.NonceStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>{@link ReplayGuard} 的默认实现。</p>
 *
 * <p>校验顺序：① 时间戳是否在允许窗口内；② nonce 是否首次出现（原子占用）。
 * 未启用时直接放行，存储不可用时按 {@link ReplayProperties#isFailOpen()} 决定放行或拒绝。</p>
 *
 * @author jy
 * @since 1.0.0
 * @implNote nonce 判重委托 {@link NonceStore}，键由 {@code keyPrefix + namespace + nonce} 组成，TTL 取窗口时长。
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultReplayGuard implements ReplayGuard {
    private final ReplayProperties properties;
    private final NonceStore nonceStore;

    @Override
    public void check(String namespace, String nonce, long timestamp) {
        if (!properties.isEnabled()) {
            return;
        }
        if (StrUtil.isBlank(nonce)) {
            throw new BizException(ReplayErrorCode.REPLAY_NONCE_DUPLICATE);
        }

        long now = System.currentTimeMillis();
        long skewMs = properties.getClockSkew().toMillis();
        if (Math.abs(now - timestamp) > skewMs) {
            log.warn("[ingot-replay] 时间戳超窗 - namespace={}, ts={}, now={}, skewMs={}",
                    namespace, timestamp, now, skewMs);
            throw new BizException(ReplayErrorCode.REPLAY_TIMESTAMP_EXPIRED);
        }

        String key = buildKey(namespace, nonce);
        Duration ttl = properties.getWindow();
        boolean acquired;
        try {
            acquired = nonceStore.tryAcquire(key, ttl);
        } catch (Exception e) {
            if (properties.isFailOpen()) {
                log.warn("[ingot-replay] 存储不可用，fail-open 放行 - namespace={}", namespace, e);
                return;
            }
            log.error("[ingot-replay] 存储不可用，fail-close 拒绝 - namespace={}", namespace, e);
            throw new BizException(ReplayErrorCode.REPLAY_STORE_UNAVAILABLE);
        }
        if (!acquired) {
            log.warn("[ingot-replay] nonce 重复 - namespace={}, nonce={}", namespace, nonce);
            throw new BizException(ReplayErrorCode.REPLAY_NONCE_DUPLICATE);
        }
    }

    private String buildKey(String namespace, String nonce) {
        return properties.getKeyPrefix() + namespace + ":" + nonce;
    }
}
