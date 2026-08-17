package com.ingot.framework.security.recording.config;

import com.ingot.framework.security.recording.model.RecordingTarget;

import java.util.List;

/**
 * <p>effective recording 配置快照。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param primaryTarget 主投递目标
 * @param shadowTargets 次要投递目标，稳定态为空
 * @param memory 内存队列参数
 */
public record EffectiveRecordingConfig(
        RecordingTarget primaryTarget,
        List<RecordingTarget> shadowTargets,
        SecurityEventProperties.MemoryQueueSettings memory) {
}
