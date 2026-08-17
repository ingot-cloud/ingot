package com.ingot.framework.security.recording.spool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.recording.config.SecurityEventProperties;
import com.ingot.framework.security.recording.model.ClaimedRecord;
import com.ingot.framework.security.recording.model.RecordPriority;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>{@link FileSpoolRecordQueue} 重启、截断、配额与 claim/ack 单测。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class FileSpoolRecordQueueTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("enqueue/claim/ack 后 pending 清空")
    void enqueueClaimAck() throws Exception {
        FileSpoolRecordQueue queue = newQueue();
        SecurityEventRecord record = sampleRecord("01234567890123456789012345678901");

        assertThat(queue.enqueue(record).isAccepted()).isTrue();
        List<ClaimedRecord<SecurityEventRecord>> claimed = queue.claim(10, Duration.ofMillis(10));
        assertThat(claimed).hasSize(1);
        queue.ack(List.of(claimed.get(0).claimId()));

        assertThat(queue.claim(10, Duration.ofMillis(10))).isEmpty();
    }

    @Test
    @DisplayName("配额满时拒绝新 DURABLE 记录")
    void rejectsWhenQuotaFull() throws Exception {
        SecurityEventProperties properties = properties(tempDir, "256B", "128B");
        FileSpoolRecordQueue queue = new FileSpoolRecordQueue(properties, new ObjectMapper());

        assertThat(queue.enqueue(sampleRecord("11234567890123456789012345678901")).isAccepted()).isTrue();
        assertThat(queue.enqueue(sampleRecord("21234567890123456789012345678901")).isAccepted()).isFalse();
    }

    @Test
    @DisplayName("重启后恢复 pending 记录")
    void recoversPendingAfterRestart() throws Exception {
        SecurityEventProperties properties = properties(tempDir, "1MB", "512KB");
        FileSpoolRecordQueue queue = new FileSpoolRecordQueue(properties, new ObjectMapper());
        queue.enqueue(sampleRecord("31234567890123456789012345678901"));

        FileSpoolRecordQueue restarted = new FileSpoolRecordQueue(properties, new ObjectMapper());
        List<ClaimedRecord<SecurityEventRecord>> claimed = restarted.claim(10, Duration.ofMillis(10));
        assertThat(claimed).hasSize(1);
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
        Files.createDirectories(tempDir);
        properties.getDelivery().getSpool().setDirectory(tempDir.toString());

        FileSpoolRecordQueue queue = new FileSpoolRecordQueue(properties, new ObjectMapper());
        Path segmentPath = segmentsDir.resolve("segment-1.spool");
        assertThat(Files.exists(segmentPath)).isFalse();
        assertThat(queue.pendingCount()).isZero();
    }

    private FileSpoolRecordQueue newQueue() throws IOException {
        return new FileSpoolRecordQueue(properties(tempDir, "1MB", "512KB"), new ObjectMapper());
    }

    private static SecurityEventProperties properties(Path dir, String maxBytes, String segmentBytes) {
        SecurityEventProperties properties = new SecurityEventProperties();
        properties.getDelivery().getSpool().setDirectory(dir.toString());
        properties.getDelivery().getSpool().setMaxBytes(maxBytes);
        properties.getDelivery().getSpool().setSegmentBytes(segmentBytes);
        return properties;
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
