package com.ingot.framework.security.oauth2.server.authorization;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>会话存储可用性哨兵：在 Redis 读写异常时提供有界宽限期，并累计降级次数供观测。</p>
 *
 * <p>严格区分两种「读不到会话」：会话键不存在说明会话已撤销或已过期，必须拒绝；Redis 抛异常说明
 * 存储不可用，此时短暂放行签名仍有效的 JWT，避免一次 Redis 抖动把所有在线用户踢下线。宽限期一过
 * 立即转为拒绝 —— 不存在长期「只验签名」的降级形态。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote 状态是单实例内存态，不跨节点同步；每个资源服务器实例独立计时。
 */
@Slf4j
public class SessionStoreAvailability {

    /**
     * {@link #degradedSinceMillis} 的哨兵值：存储可用。
     */
    private static final long AVAILABLE = -1L;

    private final long graceMillis;
    private final AtomicLong degradedSinceMillis = new AtomicLong(AVAILABLE);
    private final AtomicLong unavailableCount = new AtomicLong();
    private final AtomicLong degradedPassCount = new AtomicLong();

    public SessionStoreAvailability(Duration grace) {
        this.graceMillis = grace == null ? 0L : Math.max(grace.toMillis(), 0L);
    }

    /**
     * 标记一次成功读取，结束宽限期计时。
     */
    public void markAvailable() {
        long previous = degradedSinceMillis.getAndSet(AVAILABLE);
        if (previous != AVAILABLE) {
            log.info("[SessionStoreAvailability] 会话存储已恢复: degradedFor={}ms",
                    System.currentTimeMillis() - previous);
        }
    }

    /**
     * 记录一次存储访问失败，并判断当前是否仍在宽限期内。
     *
     * @return {@code true} 表示允许本次请求降级通过；{@code false} 表示宽限期已过，必须拒绝
     */
    public boolean markUnavailableAndAllow() {
        unavailableCount.incrementAndGet();
        long now = System.currentTimeMillis();
        long witnessed = degradedSinceMillis.compareAndExchange(AVAILABLE, now);
        long degradedSince = witnessed == AVAILABLE ? now : witnessed;

        boolean allow = now - degradedSince <= graceMillis;
        if (allow) {
            degradedPassCount.incrementAndGet();
            log.warn("[SessionStoreAvailability] 会话存储不可用，宽限期内降级放行: degradedSince={}, graceMillis={}",
                    Instant.ofEpochMilli(degradedSince), graceMillis);
        } else {
            log.error("[SessionStoreAvailability] 会话存储不可用且已超出宽限期，拒绝请求: degradedSince={}, graceMillis={}",
                    Instant.ofEpochMilli(degradedSince), graceMillis);
        }
        return allow;
    }

    /**
     * 存储访问失败累计次数。
     */
    public long getUnavailableCount() {
        return unavailableCount.get();
    }

    /**
     * 宽限期内降级放行的累计请求数。
     */
    public long getDegradedPassCount() {
        return degradedPassCount.get();
    }

    /**
     * 当前是否处于降级状态。
     */
    public boolean isDegraded() {
        return degradedSinceMillis.get() != AVAILABLE;
    }
}
