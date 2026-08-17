package com.ingot.framework.security.recording.runtime;

import com.ingot.framework.security.recording.model.ClaimedRecord;
import com.ingot.framework.security.recording.model.EnqueueResult;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.spi.RecordQueue;

import java.time.Duration;
import java.util.List;

/**
 * <p>未装配 file spool 时的 DURABLE 队列占位实现，入队始终失败。</p>
 *
 * @author jy
 * @since 1.0.0
 * @implNote Phase 02 的 file segment engine 将替换为本 bean。
 */
public final class UnconfiguredDurableRecordQueue implements RecordQueue<SecurityEventRecord> {

    @Override
    public EnqueueResult enqueue(SecurityEventRecord record) {
        return EnqueueResult.rejected("durable file spool not configured");
    }

    @Override
    public List<ClaimedRecord<SecurityEventRecord>> claim(int limit, Duration wait) {
        return List.of();
    }

    @Override
    public void ack(List<String> claimIds) {
    }

    @Override
    public void nack(List<String> claimIds, Throwable cause) {
    }

    public double usageRatio() {
        return 0.0;
    }
}
