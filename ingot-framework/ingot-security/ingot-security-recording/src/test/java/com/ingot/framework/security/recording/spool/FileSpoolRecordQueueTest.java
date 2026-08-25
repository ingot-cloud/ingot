package com.ingot.framework.security.recording.spool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.recording.config.SecurityEventProperties;
import com.ingot.framework.security.recording.model.ClaimedRecord;
import com.ingot.framework.security.recording.model.RecordPriority;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.segment.SegmentRecordCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <p>{@link FileSpoolRecordQueue} 重启、截断、配额、目录隔离与消费位点单测。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class FileSpoolRecordQueueTest {

    private static final Duration CLAIM_WAIT = Duration.ofMillis(10);

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("enqueue/claim/ack 后 pending 清空")
    void enqueueClaimAck() throws Exception {
        try (FileSpoolRecordQueue queue = newQueue()) {
            SecurityEventRecord record = sampleRecord("01234567890123456789012345678901");

            assertThat(queue.enqueue(record).isAccepted()).isTrue();
            List<ClaimedRecord<SecurityEventRecord>> claimed = queue.claim(10, CLAIM_WAIT);
            assertThat(claimed).hasSize(1);
            queue.ack(List.of(claimed.get(0).claimId()));

            assertThat(queue.claim(10, CLAIM_WAIT)).isEmpty();
        }
    }

    @Test
    @DisplayName("配额满时拒绝新 DURABLE 记录")
    void rejectsWhenQuotaFull() throws Exception {
        SecurityEventProperties properties = properties(tempDir, "256B", "128B");
        try (FileSpoolRecordQueue queue = new FileSpoolRecordQueue(properties, mapper())) {
            assertThat(queue.enqueue(sampleRecord("11234567890123456789012345678901")).isAccepted()).isTrue();
            assertThat(queue.enqueue(sampleRecord("21234567890123456789012345678901")).isAccepted()).isFalse();
        }
    }

    @Test
    @DisplayName("重启后恢复未 ack 的 pending 记录")
    void recoversPendingAfterRestart() throws Exception {
        SecurityEventProperties properties = properties(tempDir, "1MB", "512KB");
        try (FileSpoolRecordQueue queue = new FileSpoolRecordQueue(properties, mapper())) {
            queue.enqueue(sampleRecord("31234567890123456789012345678901"));
        }
        try (FileSpoolRecordQueue restarted = new FileSpoolRecordQueue(properties, mapper())) {
            List<ClaimedRecord<SecurityEventRecord>> claimed = restarted.claim(10, CLAIM_WAIT);
            assertThat(claimed).hasSize(1);
            assertThat(claimed.get(0).record().getEventId()).isEqualTo("31234567890123456789012345678901");
        }
    }

    @Test
    @DisplayName("不完整尾记录启动时被截断")
    void truncatesIncompleteTailOnRestart() throws Exception {
        SecurityEventProperties properties = properties(tempDir, "1MB", "512KB");
        Path segmentsDir = tempDir.resolve("segments");
        Files.createDirectories(segmentsDir);
        Path segment = segmentsDir.resolve("segment-1.spool");
        Files.createFile(segment);
        try (OutputStream out = Files.newOutputStream(segment)) {
            out.write(new byte[]{0, 0, 0, 10, 0, 0, 0, 0});
            out.write(new byte[]{1, 2, 3});
        }

        try (FileSpoolRecordQueue queue = new FileSpoolRecordQueue(properties, mapper())) {
            Path segmentPath = segmentsDir.resolve("segment-1.spool");
            assertThat(Files.exists(segmentPath)).isFalse();
            assertThat(queue.pendingCount()).isZero();
        }
    }

    @Test
    @DisplayName("ack 完后重启不再 claim（稳态 no-op）")
    void drainedRestartDoesNotReplay() throws Exception {
        SecurityEventProperties properties = properties(tempDir, "1MB", "512KB");
        try (FileSpoolRecordQueue queue = new FileSpoolRecordQueue(properties, mapper())) {
            assertThat(queue.enqueue(sampleRecord("41234567890123456789012345678901")).isAccepted()).isTrue();
            List<ClaimedRecord<SecurityEventRecord>> claimed = queue.claim(10, CLAIM_WAIT);
            queue.ack(List.of(claimed.get(0).claimId()));
            String active = queue.state().getActiveSegment();
            assertThat(active).isNotNull();
            assertThat(Files.size(tempDir.resolve("segments").resolve(active))).isZero();
        }
        try (FileSpoolRecordQueue restarted = new FileSpoolRecordQueue(properties, mapper())) {
            assertThat(restarted.pendingCount()).isZero();
            assertThat(restarted.claim(10, CLAIM_WAIT)).isEmpty();
        }
    }

    @Test
    @DisplayName("claim 后未 ack 即关闭，重启仍可 claim")
    void inFlightRequeuedOnRestart() throws Exception {
        SecurityEventProperties properties = properties(tempDir, "1MB", "512KB");
        try (FileSpoolRecordQueue queue = new FileSpoolRecordQueue(properties, mapper())) {
            queue.enqueue(sampleRecord("51234567890123456789012345678901"));
            assertThat(queue.claim(10, CLAIM_WAIT)).hasSize(1);
        }
        try (FileSpoolRecordQueue restarted = new FileSpoolRecordQueue(properties, mapper())) {
            List<ClaimedRecord<SecurityEventRecord>> claimed = restarted.claim(10, CLAIM_WAIT);
            assertThat(claimed).hasSize(1);
            assertThat(claimed.get(0).record().getEventId()).isEqualTo("51234567890123456789012345678901");
        }
    }

    @Test
    @DisplayName("截断 state.json 时仍能构造 queue，坏文件进入 quarantine")
    void truncatedStateDoesNotFailConstruction() throws Exception {
        SecurityEventProperties properties = properties(tempDir, "1MB", "512KB");
        try (FileSpoolRecordQueue queue = new FileSpoolRecordQueue(properties, mapper())) {
            queue.enqueue(sampleRecord("61234567890123456789012345678901"));
        }
        Path stateFile = tempDir.resolve("state.json");
        byte[] original = Files.readAllBytes(stateFile);
        Files.write(stateFile, java.util.Arrays.copyOf(original, Math.max(1, original.length / 2)));

        try (FileSpoolRecordQueue restarted = new FileSpoolRecordQueue(properties, mapper())) {
            assertThat(restarted.claim(10, CLAIM_WAIT)).hasSize(1);
            try (Stream<Path> quarantined = Files.list(tempDir.resolve("quarantine"))) {
                assertThat(quarantined.anyMatch(path -> path.getFileName().toString().startsWith("state.json")))
                        .isTrue();
            }
        }
    }

    @Test
    @DisplayName("完好空 pending 加上非空 active segment 不得扫回历史")
    void intactEmptyStateDoesNotRebuildFromSegment() throws Exception {
        ObjectMapper objectMapper = mapper();
        SecurityEventProperties properties = properties(tempDir, "1MB", "512KB");
        Path segmentsDir = tempDir.resolve("segments");
        Files.createDirectories(segmentsDir);
        Path segment = segmentsDir.resolve("segment-stale.spool");
        try (OutputStream out = Files.newOutputStream(segment)) {
            SegmentRecordCodec.writeRecord(out, objectMapper.writeValueAsBytes(
                    sampleRecord("71234567890123456789012345678901")));
        }
        SpoolState empty = new SpoolState();
        empty.setActiveSegment("segment-stale.spool");
        objectMapper.writeValue(tempDir.resolve("state.json").toFile(), empty);

        try (FileSpoolRecordQueue queue = new FileSpoolRecordQueue(properties, objectMapper)) {
            assertThat(queue.pendingCount()).isZero();
            assertThat(queue.claim(10, CLAIM_WAIT)).isEmpty();
            assertThat(Files.size(segment)).isZero();
        }
    }

    @Test
    @DisplayName("同一 parent 不同 application name 互不覆盖")
    void isolatesByApplicationName() throws Exception {
        SecurityEventProperties properties = properties(tempDir, "1MB", "512KB");
        try (FileSpoolRecordQueue pms = new FileSpoolRecordQueue(properties, mapper(), "in-service-pms");
                FileSpoolRecordQueue auth = new FileSpoolRecordQueue(properties, mapper(), "in-service-auth")) {
            pms.enqueue(sampleRecord("81234567890123456789012345678901"));
            assertThat(Files.exists(tempDir.resolve("in-service-pms").resolve("state.json"))).isTrue();
            assertThat(Files.exists(tempDir.resolve("in-service-auth").resolve("state.json"))).isTrue();
            assertThat(pms.rootDir()).isEqualTo(tempDir.resolve("in-service-pms").toAbsolutePath().normalize());
            assertThat(auth.rootDir()).isEqualTo(tempDir.resolve("in-service-auth").toAbsolutePath().normalize());
        }
    }

    @Test
    @DisplayName("同目录第二实例拿不到独占锁")
    void exclusiveLockRejectsSecondWriter() throws Exception {
        SecurityEventProperties properties = properties(tempDir, "1MB", "512KB");
        try (FileSpoolRecordQueue ignored = new FileSpoolRecordQueue(properties, mapper())) {
            assertThatThrownBy(() -> new FileSpoolRecordQueue(properties, mapper()))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("already in use");
        }
    }

    @Test
    @DisplayName("残留损坏 tmp 不影响已落盘的 state.json")
    void corruptTmpDoesNotBreakCommittedState() throws Exception {
        SecurityEventProperties properties = properties(tempDir, "1MB", "512KB");
        try (FileSpoolRecordQueue queue = new FileSpoolRecordQueue(properties, mapper())) {
            queue.enqueue(sampleRecord("91234567890123456789012345678901"));
        }
        Files.writeString(tempDir.resolve("state.json.tmp"), "{not-json");
        try (FileSpoolRecordQueue restarted = new FileSpoolRecordQueue(properties, mapper())) {
            List<ClaimedRecord<SecurityEventRecord>> claimed = restarted.claim(10, CLAIM_WAIT);
            assertThat(claimed).hasSize(1);
            assertThat(claimed.get(0).record().getEventId()).isEqualTo("91234567890123456789012345678901");
        }
    }

    private FileSpoolRecordQueue newQueue() throws IOException {
        return new FileSpoolRecordQueue(properties(tempDir, "1MB", "512KB"), mapper());
    }

    private static SecurityEventProperties properties(Path dir, String maxBytes, String segmentBytes) {
        SecurityEventProperties properties = new SecurityEventProperties();
        properties.getDelivery().getSpool().setDirectory(dir.toString());
        properties.getDelivery().getSpool().setMaxBytes(maxBytes);
        properties.getDelivery().getSpool().setSegmentBytes(segmentBytes);
        return properties;
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    private static SecurityEventRecord sampleRecord(String eventId) {
        return SecurityEventRecord.builder()
                .eventId(eventId)
                .eventType("ACCOUNT_LOCKED")
                .eventCategory("ACCOUNT")
                .priority(RecordPriority.DURABLE)
                .occurredAt(Instant.parse("2026-08-05T00:00:00Z"))
                .build();
    }
}
