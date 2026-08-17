package com.ingot.framework.security.recording.spool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * <p>spool 持久化状态：未 claim、in-flight 与已 ack 的 segment 位置。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
public final class SpoolState {

    private String activeSegment;
    private long totalBytes;
    private List<SpoolEntry> pending = new ArrayList<>();
    private List<SpoolEntry> inFlight = new ArrayList<>();

    static SpoolState load(Path stateFile, ObjectMapper objectMapper) throws IOException {
        if (!Files.exists(stateFile)) {
            return new SpoolState();
        }
        return objectMapper.readValue(Files.readString(stateFile), SpoolState.class);
    }

    void save(Path stateFile, ObjectMapper objectMapper) throws IOException {
        Files.createDirectories(stateFile.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(stateFile.toFile(), this);
    }

    /**
     * <p>spool 内单条记录位置与 claim 元数据。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    @Data
    public static final class SpoolEntry {
        private String claimId;
        private String segment;
        private long offset;
        private int length;
        private int attempts;
        private long nextRetryAtEpochMs;
        private SecurityEventRecord record;

        public SpoolEntry() {
        }

        SpoolEntry(String segment, long offset, int length, SecurityEventRecord record) {
            this.claimId = UUID.randomUUID().toString();
            this.segment = segment;
            this.offset = offset;
            this.length = length;
            this.record = record;
        }
    }
}
