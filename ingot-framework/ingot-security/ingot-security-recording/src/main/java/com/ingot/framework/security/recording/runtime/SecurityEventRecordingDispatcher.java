package com.ingot.framework.security.recording.runtime;

import com.ingot.framework.security.recording.config.EffectiveRecordingConfig;
import com.ingot.framework.security.recording.config.SecurityEventProperties;
import com.ingot.framework.security.recording.model.ClaimedRecord;
import com.ingot.framework.security.recording.model.DeliveryResult;
import com.ingot.framework.security.recording.model.EnqueueResult;
import com.ingot.framework.security.recording.model.PublishOutcome;
import com.ingot.framework.security.recording.model.RecordPriority;
import com.ingot.framework.security.recording.model.RecordingTarget;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.spi.RecordQueue;
import com.ingot.framework.security.recording.spi.SecurityEventEnqueue;
import com.ingot.framework.security.recording.spi.SecurityEventStore;
import com.ingot.framework.security.recording.spi.SecurityEventTransport;
import com.ingot.framework.security.recording.support.RateLimitedLogger;
import com.ingot.framework.security.recording.support.SecurityEventIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * <p>安全事件 recording 调度器：分级入队、攒批投递、target/shadow 路由与优雅关闭。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class SecurityEventRecordingDispatcher implements AutoCloseable, SecurityEventEnqueue {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventRecordingDispatcher.class);

    private final EffectiveRecordingConfig config;
    private final SecurityEventProperties properties;
    private final PriorityClassifier priorityClassifier;
    private final MemoryRecordQueue memoryQueue;
    private final RecordQueue<SecurityEventRecord> durableQueue;
    private final RecordingMetrics metrics;
    @Nullable
    private final SecurityEventStore primaryStore;
    @Nullable
    private final SecurityEventTransport transport;
    @Nullable
    private final SecurityEventStore shadowLocalStore;
    private final RateLimitedLogger durableFailureLogger = new RateLimitedLogger("durable-spool");

    private final Thread memoryWorker;
    private final Thread durableWorker;
    private volatile boolean running = true;

    public SecurityEventRecordingDispatcher(
            EffectiveRecordingConfig config,
            SecurityEventProperties properties,
            PriorityClassifier priorityClassifier,
            MemoryRecordQueue memoryQueue,
            RecordQueue<SecurityEventRecord> durableQueue,
            RecordingMetrics metrics,
            @Nullable SecurityEventStore primaryStore,
            @Nullable SecurityEventTransport transport,
            @Nullable SecurityEventStore shadowLocalStore) {
        this.config = config;
        this.properties = properties;
        this.priorityClassifier = priorityClassifier;
        this.memoryQueue = memoryQueue;
        this.durableQueue = durableQueue;
        this.metrics = metrics;
        this.primaryStore = primaryStore;
        this.transport = transport;
        this.shadowLocalStore = shadowLocalStore;

        validateWiring();

        SecurityEventProperties.MemoryQueueSettings memory = config.memory();
        this.memoryWorker = new Thread(this::runMemoryLoop, "security-recording-memory");
        this.memoryWorker.setDaemon(true);
        this.durableWorker = new Thread(this::runDurableLoop, "security-recording-durable");
        this.durableWorker.setDaemon(true);
        this.memoryWorker.start();
        this.durableWorker.start();
    }

    private void validateWiring() {
        if (config.primaryTarget() == RecordingTarget.LOCAL && primaryStore == null) {
            throw new IllegalStateException("target=local requires a SecurityEventStore bean");
        }
        if (config.primaryTarget() == RecordingTarget.CENTER && transport == null) {
            throw new IllegalStateException("target=center requires a SecurityEventTransport bean");
        }
        if (config.shadowTargets().contains(RecordingTarget.LOCAL)
                && shadowLocalStore == null
                && config.primaryTarget() != RecordingTarget.LOCAL) {
            throw new IllegalStateException("shadow local target requires a SecurityEventStore bean");
        }
    }

    public PublishOutcome enqueue(SecurityEventRecord record) {
        metrics.incrementPublished();
        SecurityEventRecord normalized = normalize(record);
        RecordPriority priority = priorityClassifier.classify(normalized);
        SecurityEventRecord withPriority = normalized.toBuilder().priority(priority).build();

        if (priority == RecordPriority.BEST_EFFORT) {
            EnqueueResult result = memoryQueue.enqueue(withPriority);
            if (result.isAccepted()) {
                metrics.incrementAccepted();
                return PublishOutcome.ACCEPTED;
            }
            metrics.incrementDropped();
            return PublishOutcome.DROPPED;
        }

        long timeoutMs = properties.getDelivery().getSpool().getDurableAckTimeoutMs();
        try {
            EnqueueResult result = CompletableFuture
                    .supplyAsync(() -> durableQueue.enqueue(withPriority))
                    .get(timeoutMs, TimeUnit.MILLISECONDS);
            if (result.isAccepted()) {
                metrics.incrementAccepted();
                return PublishOutcome.ACCEPTED;
            }
            metrics.incrementFailed();
            durableFailureLogger.warnOnceThenEvery(
                    "durable spool rejected event: " + result.reason(), 50);
            return PublishOutcome.FAILED;
        } catch (TimeoutException e) {
            metrics.incrementFailed();
            durableFailureLogger.warnOnceThenEvery("durable spool ack timeout", 50);
            return PublishOutcome.FAILED;
        } catch (Exception e) {
            metrics.incrementFailed();
            durableFailureLogger.warnOnceThenEvery(
                    "durable spool enqueue failed: " + e.getMessage(), 50);
            return PublishOutcome.FAILED;
        }
    }

    private SecurityEventRecord normalize(SecurityEventRecord record) {
        SecurityEventRecord.SecurityEventRecordBuilder builder = record.toBuilder();
        if (record.getEventId() == null || record.getEventId().isBlank()) {
            builder.eventId(SecurityEventIds.newEventId());
        }
        if (record.getSourceModule() == null || record.getSourceModule().isBlank()) {
            builder.sourceModule(properties.getSourceModule());
        }
        return builder.build();
    }

    private void runMemoryLoop() {
        SecurityEventProperties.MemoryQueueSettings memory = config.memory();
        int batchSize = Math.max(memory.getBatchSize(), 1);
        Duration pollTimeout = Duration.ofMillis(Math.max(memory.getPollTimeoutMs(), 1));
        while (running || memoryQueue.size() > 0) {
            List<ClaimedRecord<SecurityEventRecord>> claimed = memoryQueue.claim(batchSize, pollTimeout);
            if (claimed.isEmpty()) {
                continue;
            }
            List<SecurityEventRecord> batch = claimed.stream()
                    .map(ClaimedRecord::record)
                    .collect(Collectors.toCollection(ArrayList::new));
            deliverBatch(batch);
            memoryQueue.ack(claimed.stream().map(ClaimedRecord::claimId).toList());
        }
    }

    private void runDurableLoop() {
        SecurityEventProperties.Spool spool = properties.getDelivery().getSpool();
        int batchSize = Math.max(spool.getReplayBatchSize(), 1);
        Duration pollTimeout = Duration.ofMillis(Math.max(config.memory().getPollTimeoutMs(), 1));
        while (running) {
            List<ClaimedRecord<SecurityEventRecord>> claimed = durableQueue.claim(batchSize, pollTimeout);
            if (claimed.isEmpty()) {
                continue;
            }
            List<String> claimIds = new ArrayList<>(claimed.size());
            List<SecurityEventRecord> batch = new ArrayList<>(claimed.size());
            for (ClaimedRecord<SecurityEventRecord> item : claimed) {
                claimIds.add(item.claimId());
                batch.add(item.record());
            }
            try {
                deliverBatch(batch);
                durableQueue.ack(claimIds);
                metrics.incrementReplayed();
            } catch (Exception e) {
                durableQueue.nack(claimIds, e);
                log.debug("durable batch nack: {}", e.getMessage());
            }
        }
    }

    void deliverBatch(List<SecurityEventRecord> batch) {
        if (batch.isEmpty()) {
            return;
        }
        deliverToPrimary(batch);
        for (RecordingTarget shadow : config.shadowTargets()) {
            deliverShadow(batch, shadow);
        }
    }

    private void deliverToPrimary(List<SecurityEventRecord> batch) {
        if (config.primaryTarget() == RecordingTarget.LOCAL) {
            primaryStore.appendBatch(batch);
            metrics.incrementPersisted();
            return;
        }
        DeliveryResult result = transport.deliverBatch(batch);
        if (result.success()) {
            metrics.incrementPersisted();
            return;
        }
        if (result.retryable()) {
            throw new RetryableDeliveryException(result.message());
        }
        metrics.incrementFailed();
    }

    private void deliverShadow(List<SecurityEventRecord> batch, RecordingTarget shadow) {
        try {
            if (shadow == RecordingTarget.LOCAL) {
                SecurityEventStore store = shadowLocalStore != null ? shadowLocalStore : primaryStore;
                if (store != null) {
                    store.appendBatch(batch);
                }
                return;
            }
            if (transport != null) {
                DeliveryResult result = transport.deliverBatch(batch);
                if (!result.success()) {
                    metrics.incrementShadowFailures();
                }
            }
        } catch (Exception e) {
            metrics.incrementShadowFailures();
            log.debug("shadow delivery failed for {}: {}", shadow, e.getMessage());
        }
    }

    public double queueUsageRatio() {
        double memoryUsage = memoryQueue.usageRatio();
        double durableUsage = durableQueue instanceof UnconfiguredDurableRecordQueue unconfigured
                ? unconfigured.usageRatio()
                : 0.0;
        return Math.max(memoryUsage, durableUsage);
    }

    @Override
    public void close() {
        running = false;
        memoryWorker.interrupt();
        durableWorker.interrupt();
        long shutdownTimeoutMs = config.memory().getShutdownTimeoutMs();
        try {
            memoryWorker.join(shutdownTimeoutMs);
            durableWorker.join(shutdownTimeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * <p>可重试投递失败，供 durable worker nack。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    static final class RetryableDeliveryException extends RuntimeException {
        RetryableDeliveryException(String message) {
            super(message);
        }
    }
}
