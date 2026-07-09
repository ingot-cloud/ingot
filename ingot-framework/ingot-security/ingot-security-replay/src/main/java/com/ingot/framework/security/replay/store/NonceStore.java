package com.ingot.framework.security.replay.store;

import java.time.Duration;

/**
 * <p>nonce 存储抽象，提供"首次写入成功、重复写入失败"的原子占用语义。</p>
 *
 * <p>用于判定请求随机数是否已被使用，是防重放判重的底层能力。默认由 Redis 实现，
 * 可替换为 Redisson、本地缓存等其它后端。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see RedisNonceStore
 */
public interface NonceStore {

    /**
     * 尝试占用一个 nonce。
     *
     * @param key nonce 存储键（调用方已拼接前缀与命名空间）
     * @param ttl 存活时长
     * @return {@code true} 表示首次占用成功；{@code false} 表示该 nonce 已存在（重复）
     */
    boolean tryAcquire(String key, Duration ttl);
}
