package com.ingot.framework.security.recording.spool;

import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.segment.SegmentRecordCodec;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * <p>spool 启动恢复：截断不完整尾记录、回收 inFlight，仅在 state 缺失或损坏时扫描 segment。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class SpoolRecovery {

    private SpoolRecovery() {
    }

    /**
     * 按 load 结果恢复队列：完好 state 只退回 inFlight；缺失或损坏才扫描 segment 重建 pending。
     *
     * @param queue      已持有目录锁的 spool
     * @param loadResult {@link SpoolState#load} 的结果
     * @throws IOException 扫描 segment、隔离损坏文件或落盘失败
     */
    public static void recoverOnStartup(FileSpoolRecordQueue queue, SpoolState.LoadResult loadResult)
            throws IOException {
        truncateAllSegmentTails(queue.segmentsDir());
        if (loadResult.isCorrupt()) {
            queue.quarantineStateFile();
        }
        if (loadResult.needsSegmentRebuild()) {
            rebuildPendingFromSegments(queue);
        } else {
            requeueInFlight(queue);
        }
        queue.hydrateRecords();
        queue.reclaimUnreferencedSegments();
        queue.refreshTotalBytes();
        queue.persistState();
    }

    static void requeueInFlight(FileSpoolRecordQueue queue) {
        SpoolState state = queue.state();
        for (SpoolState.SpoolEntry entry : state.getInFlight()) {
            entry.setNextRetryAtEpochMs(0);
        }
        state.getPending().addAll(state.getInFlight());
        state.getInFlight().clear();
    }

    static void rebuildPendingFromSegments(FileSpoolRecordQueue queue) throws IOException {
        SpoolState state = queue.state();
        state.getPending().clear();
        state.getInFlight().clear();
        if (!Files.exists(queue.segmentsDir())) {
            return;
        }
        try (Stream<Path> files = Files.list(queue.segmentsDir())) {
            files.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> scanSegment(queue, path));
        }
        try (Stream<Path> files = Files.list(queue.segmentsDir())) {
            files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .max(Comparator.naturalOrder())
                    .ifPresent(state::setActiveSegment);
        }
    }

    private static void scanSegment(FileSpoolRecordQueue queue, Path segmentPath) {
        String segmentName = segmentPath.getFileName().toString();
        long offset = 0;
        try (InputStream in = Files.newInputStream(segmentPath)) {
            while (true) {
                long recordStart = offset;
                byte[] payload;
                try {
                    payload = SegmentRecordCodec.readRecord(in);
                } catch (SegmentRecordCodec.CorruptSegmentException e) {
                    truncate(segmentPath, recordStart);
                    break;
                }
                if (payload == null) {
                    break;
                }
                int recordBytes = SegmentRecordCodec.HEADER_BYTES + payload.length;
                SecurityEventRecord record = queue.objectMapper().readValue(payload, SecurityEventRecord.class);
                queue.state().getPending().add(new SpoolState.SpoolEntry(segmentName, recordStart, recordBytes, record));
                offset += recordBytes;
            }
        } catch (Exception e) {
            try {
                queue.quarantineSegment(segmentName);
            } catch (IOException ignored) {
            }
        }
    }

    static void truncateAllSegmentTails(Path segmentsDir) throws IOException {
        if (!Files.exists(segmentsDir)) {
            return;
        }
        try (Stream<Path> files = Files.list(segmentsDir)) {
            for (Path path : files.toList()) {
                if (Files.isRegularFile(path)) {
                    truncateSegmentTail(path);
                }
            }
        }
    }

    private static void truncateSegmentTail(Path segmentPath) throws IOException {
        long offset = 0;
        try (InputStream in = Files.newInputStream(segmentPath)) {
            while (true) {
                long recordStart = offset;
                byte[] payload;
                try {
                    payload = SegmentRecordCodec.readRecord(in);
                } catch (SegmentRecordCodec.CorruptSegmentException e) {
                    truncate(segmentPath, recordStart);
                    return;
                }
                if (payload == null) {
                    return;
                }
                offset += SegmentRecordCodec.HEADER_BYTES + payload.length;
            }
        }
    }

    private static void truncate(Path segmentPath, long length) throws IOException {
        if (length <= 0) {
            Files.deleteIfExists(segmentPath);
            return;
        }
        try (var channel = java.nio.channels.FileChannel.open(
                segmentPath, java.nio.file.StandardOpenOption.WRITE)) {
            channel.truncate(length);
        }
    }
}
