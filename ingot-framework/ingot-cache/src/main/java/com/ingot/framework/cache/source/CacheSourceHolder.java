package com.ingot.framework.cache.source;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>当前缓存来源与累计降级次数的进程内持有者，供日志与 Actuator 端点观测降级态。</p>
 *
 * <p>仅由 {@code ResilientCacheLayer} 在每次真实加载后更新；L1/L2 命中不改变来源，
 * 因此该值反映的是「最近一次穿透到底层时数据从哪来」。线程安全。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see CacheSource
 */
public class CacheSourceHolder {

    private final AtomicReference<CacheSource> current = new AtomicReference<>(CacheSource.REMOTE);
    private final AtomicLong lastKnownGoodCount = new AtomicLong();
    private final AtomicLong localFloorCount = new AtomicLong();
    private final AtomicReference<LocalDateTime> lastDegradeAt = new AtomicReference<>();

    /**
     * 标记当前生效来源；降级来源会累加对应计数并记录发生时间。
     *
     * @param source 本次数据来源
     */
    public void mark(CacheSource source) {
        current.set(source);
        switch (source) {
            case LAST_KNOWN_GOOD -> {
                lastKnownGoodCount.incrementAndGet();
                lastDegradeAt.set(LocalDateTime.now());
            }
            case LOCAL_FLOOR -> {
                localFloorCount.incrementAndGet();
                lastDegradeAt.set(LocalDateTime.now());
            }
            default -> {
                // REMOTE 为正常态，不计数
            }
        }
    }

    public CacheSource current() {
        return current.get();
    }

    public long lastKnownGoodCount() {
        return lastKnownGoodCount.get();
    }

    public long localFloorCount() {
        return localFloorCount.get();
    }

    public LocalDateTime lastDegradeAt() {
        return lastDegradeAt.get();
    }
}
