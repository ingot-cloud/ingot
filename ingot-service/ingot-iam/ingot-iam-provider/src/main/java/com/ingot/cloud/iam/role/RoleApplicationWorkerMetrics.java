package com.ingot.cloud.iam.role;

import java.util.function.IntSupplier;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * <p>从现有调度过程采集指标，不为了监控发起数据库轮询。</p>
 * @author jy
 * @since 1.0.0
 */
final class RoleApplicationWorkerMetrics {
    private static final String PREFIX = "iam.role.application.";
    private final MeterRegistry registry;
    private final Counter discoveries;
    private final Counter discoveryErrors;
    private final Counter leaseConflicts;
    private final Counter dropped;
    private final Counter batchErrors;
    private final Timer batchDuration;

    RoleApplicationWorkerMetrics(MeterRegistry registry) {
        this.registry = registry;
        discoveries = registry.counter(PREFIX + "discovery");
        discoveryErrors = registry.counter(PREFIX + "discovery.errors");
        leaseConflicts = registry.counter(PREFIX + "lease.conflicts");
        dropped = registry.counter(PREFIX + "hints.dropped");
        batchErrors = registry.counter(PREFIX + "batch.errors");
        batchDuration = registry.timer(PREFIX + "batch.duration");
    }

    void registerQueue(IntSupplier depth) {
        Gauge.builder(PREFIX + "queue.depth", depth, IntSupplier::getAsInt).strongReference(true).register(registry);
    }

    void discovery() { discoveries.increment(); }
    void discoveryError() { discoveryErrors.increment(); }
    void conflict() { leaseConflicts.increment(); }
    void dropped() { dropped.increment(); }
    void batchError() { batchErrors.increment(); }
    Timer.Sample startBatch() { return Timer.start(registry); }
    void finishBatch(Timer.Sample sample) { sample.stop(batchDuration); }
}
