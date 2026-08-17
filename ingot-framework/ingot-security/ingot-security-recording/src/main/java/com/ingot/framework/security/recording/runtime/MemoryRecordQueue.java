package com.ingot.framework.security.recording.runtime;

import com.ingot.framework.security.recording.model.ClaimedRecord;
import com.ingot.framework.security.recording.model.EnqueueResult;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.spi.RecordQueue;
import com.ingot.framework.security.recording.support.RateLimitedLogger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>BEST_EFFORT 有界内存队列；满时非阻塞拒绝。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class MemoryRecordQueue implements RecordQueue<SecurityEventRecord> {

    private final BlockingQueue<ClaimedRecord<SecurityEventRecord>> queue;
    private final RateLimitedLogger rateLimitedLogger;
    private final AtomicInteger depth = new AtomicInteger();

    public MemoryRecordQueue(int capacity, String tag) {
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.rateLimitedLogger = new RateLimitedLogger(tag);
    }

    @Override
    public EnqueueResult enqueue(SecurityEventRecord record) {
        ClaimedRecord<SecurityEventRecord> claimed = new ClaimedRecord<>(UUID.randomUUID().toString(), record);
        if (queue.offer(claimed)) {
            depth.incrementAndGet();
            return EnqueueResult.success();
        }
        rateLimitedLogger.warnOnceThenEvery(
                "memory queue full, dropping BEST_EFFORT event", 100);
        return EnqueueResult.rejected("memory queue full");
    }

    @Override
    public List<ClaimedRecord<SecurityEventRecord>> claim(int limit, Duration wait) {
        List<ClaimedRecord<SecurityEventRecord>> batch = new ArrayList<>(Math.max(limit, 1));
        try {
            ClaimedRecord<SecurityEventRecord> first = queue.poll(
                    wait.toMillis(), TimeUnit.MILLISECONDS);
            if (first == null) {
                return batch;
            }
            batch.add(first);
            queue.drainTo(batch, limit - 1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        depth.addAndGet(-batch.size());
        return batch;
    }

    @Override
    public void ack(List<String> claimIds) {
        // memory queue: ack is no-op, records already removed on claim
    }

    @Override
    public void nack(List<String> claimIds, Throwable cause) {
        // dropped on failure for BEST_EFFORT
    }

    public int size() {
        return queue.size();
    }

    public int capacity() {
        return queue.size() + queue.remainingCapacity();
    }

    public double usageRatio() {
        int cap = capacity();
        return cap <= 0 ? 0.0 : (double) size() / cap;
    }
}
