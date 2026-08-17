package com.ingot.framework.security.recording.actuate;

import com.ingot.framework.security.recording.config.EffectiveRecordingConfig;
import com.ingot.framework.security.recording.runtime.MemoryRecordQueue;
import com.ingot.framework.security.recording.runtime.RecordingMetrics;
import com.ingot.framework.security.recording.runtime.SecurityEventRecordingDispatcher;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>安全事件 recording 运行时观测端点。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Endpoint(id = "securityrecording")
public class SecurityEventRecordingEndpoint {

    private final EffectiveRecordingConfig config;
    private final RecordingMetrics metrics;
    private final MemoryRecordQueue memoryQueue;
    private final SecurityEventRecordingDispatcher dispatcher;

    public SecurityEventRecordingEndpoint(
            EffectiveRecordingConfig config,
            RecordingMetrics metrics,
            MemoryRecordQueue memoryQueue,
            SecurityEventRecordingDispatcher dispatcher) {
        this.config = config;
        this.metrics = metrics;
        this.memoryQueue = memoryQueue;
        this.dispatcher = dispatcher;
    }

    @ReadOperation
    public Map<String, Object> snapshot() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("primaryTarget", config.primaryTarget().name());
        body.put("shadowTargets", config.shadowTargets().stream().map(Enum::name).toList());
        body.put("memoryQueueDepth", memoryQueue.size());
        body.put("memoryQueueCapacity", memoryQueue.capacity());
        body.put("queueUsageRatio", dispatcher.queueUsageRatio());
        body.put("published", metrics.getPublished());
        body.put("accepted", metrics.getAccepted());
        body.put("persisted", metrics.getPersisted());
        body.put("dropped", metrics.getDropped());
        body.put("failed", metrics.getFailed());
        body.put("replayed", metrics.getReplayed());
        body.put("duplicate", metrics.getDuplicate());
        body.put("shadowFailures", metrics.getShadowFailures());
        return body;
    }
}
