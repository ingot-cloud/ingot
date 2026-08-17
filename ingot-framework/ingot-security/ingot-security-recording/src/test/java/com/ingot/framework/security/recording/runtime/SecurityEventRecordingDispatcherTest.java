package com.ingot.framework.security.recording.runtime;

import com.ingot.framework.security.recording.config.EffectiveRecordingConfig;
import com.ingot.framework.security.recording.config.SecurityEventProperties;
import com.ingot.framework.security.recording.model.PublishOutcome;
import com.ingot.framework.security.recording.model.RecordPriority;
import com.ingot.framework.security.recording.model.RecordingTarget;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.model.StoreCapabilities;
import com.ingot.framework.security.recording.spi.SecurityEventStore;
import com.ingot.framework.security.recording.spi.SecurityEventTransport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>{@link SecurityEventRecordingDispatcher} 路由、队列满与 fail-open 单测。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class SecurityEventRecordingDispatcherTest {

    private SecurityEventRecordingDispatcher dispatcher;

    @AfterEach
    void tearDown() {
        if (dispatcher != null) {
            dispatcher.close();
        }
    }

    @Test
    @DisplayName("target=local 时 BEST_EFFORT 事件写入 Store")
    void localTargetPersistsBestEffort() throws Exception {
        CopyOnWriteArrayList<SecurityEventRecord> persisted = new CopyOnWriteArrayList<>();
        SecurityEventStore store = capturingStore("mysql", persisted);
        dispatcher = createDispatcher(RecordingTarget.LOCAL, List.of(), store, null, null);

        PublishOutcome outcome = dispatcher.enqueue(bestEffortRecord());

        assertThat(outcome).isEqualTo(PublishOutcome.ACCEPTED);
        awaitPersisted(persisted, 1);
    }

    @Test
    @DisplayName("内存队列满时 BEST_EFFORT 非阻塞拒绝")
    void memoryQueueFullDropsBestEffort() {
        MemoryRecordQueue tinyQueue = new MemoryRecordQueue(1, "test");
        assertThat(tinyQueue.enqueue(bestEffortRecord()).isAccepted()).isTrue();
        assertThat(tinyQueue.enqueue(bestEffortRecord()).isAccepted()).isFalse();
    }

    @Test
    @DisplayName("DURABLE 未配置 spool 时返回 FAILED 且 fail-open")
    void durableWithoutSpoolFailsOpen() {
        dispatcher = createDispatcher(
                RecordingTarget.LOCAL,
                List.of(),
                capturingStore("mysql", new CopyOnWriteArrayList<>()),
                null,
                null);

        PublishOutcome outcome = dispatcher.enqueue(durableRecord());
        assertThat(outcome).isEqualTo(PublishOutcome.FAILED);
    }

    @Test
    @DisplayName("shadow local 在主 target=center 时额外写入本地 Store")
    void shadowLocalWritesSecondaryStore() throws Exception {
        CopyOnWriteArrayList<SecurityEventRecord> shadowPersisted = new CopyOnWriteArrayList<>();
        SecurityEventStore shadowStore = capturingStore("mysql", shadowPersisted);
        dispatcher = createDispatcher(
                RecordingTarget.CENTER,
                List.of(RecordingTarget.LOCAL),
                null,
                capturingTransport(batch ->
                        com.ingot.framework.security.recording.model.DeliveryResult.success(batch.size())),
                shadowStore);

        PublishOutcome outcome = dispatcher.enqueue(bestEffortRecord());
        assertThat(outcome).isEqualTo(PublishOutcome.ACCEPTED);
        awaitPersisted(shadowPersisted, 1);
    }

    private SecurityEventRecordingDispatcher createDispatcher(
            RecordingTarget target,
            List<RecordingTarget> shadows,
            SecurityEventStore primaryStore,
            SecurityEventTransport transport,
            SecurityEventStore shadowStore) {
        SecurityEventProperties properties = new SecurityEventProperties();
        EffectiveRecordingConfig config = new EffectiveRecordingConfig(
                target,
                shadows,
                properties.getDelivery().getMemory());
        return new SecurityEventRecordingDispatcher(
                config,
                properties,
                new DefaultPriorityClassifier(properties),
                new MemoryRecordQueue(64, "test"),
                new UnconfiguredDurableRecordQueue(),
                new RecordingMetrics(),
                primaryStore,
                transport,
                shadowStore);
    }

    private static SecurityEventStore capturingStore(
            String id,
            CopyOnWriteArrayList<SecurityEventRecord> sink) {
        return new SecurityEventStore() {
            @Override
            public String storeId() {
                return id;
            }

            @Override
            public StoreCapabilities capabilities() {
                return StoreCapabilities.none();
            }

            @Override
            public void appendBatch(List<SecurityEventRecord> records) {
                sink.addAll(records);
            }
        };
    }

    private static SecurityEventTransport capturingTransport(
            java.util.function.Function<List<SecurityEventRecord>, com.ingot.framework.security.recording.model.DeliveryResult> fn) {
        return new SecurityEventTransport() {
            @Override
            public String transportId() {
                return "feign";
            }

            @Override
            public com.ingot.framework.security.recording.model.DeliveryResult deliverBatch(
                    List<SecurityEventRecord> records) {
                return fn.apply(records);
            }
        };
    }

    private static SecurityEventRecord bestEffortRecord() {
        return SecurityEventRecord.builder()
                .eventId("01234567890123456789012345678901")
                .eventType("LOGIN_SUCCESS")
                .eventCategory("AUTH")
                .occurredAt(Instant.parse("2026-08-04T00:00:00Z"))
                .build();
    }

    private static SecurityEventRecord durableRecord() {
        return SecurityEventRecord.builder()
                .eventId("11234567890123456789012345678901")
                .eventType("ACCOUNT_LOCKED")
                .eventCategory("ACCOUNT")
                .occurredAt(Instant.parse("2026-08-04T00:00:00Z"))
                .build();
    }

    private static void awaitPersisted(CopyOnWriteArrayList<SecurityEventRecord> sink, int expected)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            if (sink.size() >= expected) {
                return;
            }
            Thread.sleep(20);
        }
        assertThat(sink).hasSize(expected);
    }
}
