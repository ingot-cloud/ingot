package com.ingot.framework.security.recording.runtime;

import com.ingot.framework.security.recording.model.ClaimedRecord;
import com.ingot.framework.security.recording.model.EnqueueResult;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.spi.RecordQueue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 * <p>未装配 file spool 时的 DURABLE 队列占位实现，入队始终失败。</p>
 *
 * @author jy
 * @since 1.0.0
 * @implNote Phase 02 的 file segment engine 将替换为本 bean。
 */
public final class UnconfiguredDurableRecordQueue implements RecordQueue<SecurityEventRecord> {

    /** {@inheritDoc} */
    @Override
    public EnqueueResult enqueue(SecurityEventRecord record) {
        return EnqueueResult.rejected("durable file spool not configured");
    }

    /** {@inheritDoc} */
    @Override
    public List<ClaimedRecord<SecurityEventRecord>> claim(int limit, Duration wait) {
        if (limit <= 0 || wait.isZero() || wait.isNegative()) {
            return List.of();
        }
        LockSupport.parkNanos(wait.toNanos());
        return List.of();
    }

    /** {@inheritDoc} */
    @Override
    public void ack(List<String> claimIds) {
    }

    /** {@inheritDoc} */
    @Override
    public void nack(List<String> claimIds, Throwable cause) {
    }

    /**
     * 返回占位队列的固定零占用率。
     *
     * @return 固定为 {@code 0.0}
     */
    public double usageRatio() {
        return 0.0;
    }
}
