package com.ingot.framework.security.recording.spool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.segment.SegmentRecordCodec;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * <p>离线 spool 恢复/导出工具，供回滚或运维重放使用。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class SpoolRecoveryTool {

    private SpoolRecoveryTool() {
    }

    public static List<SecurityEventRecord> exportRecords(Path spoolDirectory, ObjectMapper objectMapper)
            throws IOException {
        Path segmentsDir = spoolDirectory.resolve("segments");
        ObjectMapper mapper = objectMapper.findAndRegisterModules();
        List<SecurityEventRecord> records = new ArrayList<>();
        if (!Files.exists(segmentsDir)) {
            return records;
        }
        try (Stream<Path> files = Files.list(segmentsDir)) {
            for (Path segment : files.filter(Files::isRegularFile).sorted().toList()) {
                records.addAll(readSegment(segment, mapper));
            }
        }
        return records;
    }

    private static List<SecurityEventRecord> readSegment(Path segment, ObjectMapper mapper) throws IOException {
        List<SecurityEventRecord> records = new ArrayList<>();
        try (InputStream in = Files.newInputStream(segment)) {
            while (true) {
                byte[] payload;
                try {
                    payload = SegmentRecordCodec.readRecord(in);
                } catch (SegmentRecordCodec.CorruptSegmentException e) {
                    break;
                }
                if (payload == null) {
                    break;
                }
                records.add(mapper.readValue(payload, SecurityEventRecord.class));
            }
        }
        return records;
    }
}
