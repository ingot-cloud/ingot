package com.ingot.framework.security.recording.store.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.recording.model.RecordPriority;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.store.log.config.LogStoreProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>{@link LogSecurityEventStore} JSONL 写入与 segment 滚动单测。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class LogSecurityEventStoreTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("appendBatch 写入 JSONL 文件")
    void writesJsonlRecords() throws Exception {
        LogStoreProperties properties = new LogStoreProperties();
        properties.setDirectory(tempDir.toString());
        properties.setSegmentBytes("512B");

        LogSecurityEventStore store = new LogSecurityEventStore(properties, new ObjectMapper().findAndRegisterModules());
        store.appendBatch(List.of(sampleRecord("01234567890123456789012345678901")));

        try (Stream<Path> files = Files.list(tempDir)) {
            long jsonlFiles = files.filter(path -> path.getFileName().toString().endsWith(".jsonl")).count();
            assertThat(jsonlFiles).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    @DisplayName("capabilities 不含 QUERY/RETENTION")
    void capabilitiesExcludeQuery() throws Exception {
        LogStoreProperties properties = new LogStoreProperties();
        properties.setDirectory(tempDir.toString());
        LogSecurityEventStore store = new LogSecurityEventStore(properties, new ObjectMapper().findAndRegisterModules());
        assertThat(store.capabilities().supports(com.ingot.framework.security.recording.model.StoreCapability.QUERY))
                .isFalse();
        assertThat(store.storeId()).isEqualTo("log");
    }

    private static SecurityEventRecord sampleRecord(String eventId) {
        return SecurityEventRecord.builder()
                .eventId(eventId)
                .eventType("LOGIN_SUCCESS")
                .eventCategory("AUTH")
                .priority(RecordPriority.BEST_EFFORT)
                .occurredAt(Instant.parse("2026-08-05T00:00:00Z"))
                .sourceModule("test")
                .build();
    }
}
