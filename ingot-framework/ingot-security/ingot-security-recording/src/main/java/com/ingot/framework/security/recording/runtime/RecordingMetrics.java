package com.ingot.framework.security.recording.runtime;

import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>安全事件 recording 计数器；Micrometer 可用时由 auto-config 桥接为指标。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class RecordingMetrics {

    private final AtomicLong published = new AtomicLong();
    private final AtomicLong accepted = new AtomicLong();
    private final AtomicLong persisted = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong replayed = new AtomicLong();
    private final AtomicLong duplicate = new AtomicLong();
    private final AtomicLong shadowFailures = new AtomicLong();

    public void incrementPublished() {
        published.incrementAndGet();
    }

    public void incrementAccepted() {
        accepted.incrementAndGet();
    }

    public void incrementPersisted() {
        persisted.incrementAndGet();
    }

    public void incrementDropped() {
        dropped.incrementAndGet();
    }

    public void incrementFailed() {
        failed.incrementAndGet();
    }

    public void incrementReplayed() {
        replayed.incrementAndGet();
    }

    public void incrementDuplicate() {
        duplicate.incrementAndGet();
    }

    public void incrementShadowFailures() {
        shadowFailures.incrementAndGet();
    }

    public long getPublished() {
        return published.get();
    }

    public long getAccepted() {
        return accepted.get();
    }

    public long getPersisted() {
        return persisted.get();
    }

    public long getDropped() {
        return dropped.get();
    }

    public long getFailed() {
        return failed.get();
    }

    public long getReplayed() {
        return replayed.get();
    }

    public long getDuplicate() {
        return duplicate.get();
    }

    public long getShadowFailures() {
        return shadowFailures.get();
    }
}
