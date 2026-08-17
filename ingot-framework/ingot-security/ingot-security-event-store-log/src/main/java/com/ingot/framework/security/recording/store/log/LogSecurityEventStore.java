package com.ingot.framework.security.recording.store.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.model.StoreCapabilities;
import com.ingot.framework.security.recording.segment.ByteSizeParser;
import com.ingot.framework.security.recording.spi.SecurityEventStore;
import com.ingot.framework.security.recording.store.log.config.LogStoreProperties;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * <p>JSONL 结构化文件日志 Store；不提供查询，支持 segment 滚动与 retention。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class LogSecurityEventStore implements SecurityEventStore {

    private final Path rootDir;
    private final ObjectMapper objectMapper;
    private final long segmentBytes;
    private final long totalSizeCap;
    private final int retentionDays;
    private final ReentrantLock lock = new ReentrantLock();

    private String activeSegment;
    private long activeSize;

    public LogSecurityEventStore(LogStoreProperties properties, ObjectMapper objectMapper) throws IOException {
        this.objectMapper = objectMapper.findAndRegisterModules();
        this.rootDir = Path.of(properties.getDirectory()).toAbsolutePath().normalize();
        this.segmentBytes = ByteSizeParser.parse(properties.getSegmentBytes(), 64L * 1024 * 1024);
        this.totalSizeCap = ByteSizeParser.parse(properties.getTotalSizeCap(), 2L * 1024L * 1024L * 1024L);
        this.retentionDays = Math.max(properties.getRetentionDays(), 0);
        Files.createDirectories(rootDir);
        initActiveSegment();
        applyRetention();
    }

    @Override
    public String storeId() {
        return "log";
    }

    @Override
    public StoreCapabilities capabilities() {
        return StoreCapabilities.none();
    }

    @Override
    public void appendBatch(List<SecurityEventRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        lock.lock();
        try {
            for (SecurityEventRecord record : records) {
                byte[] line = (objectMapper.writeValueAsString(record) + System.lineSeparator())
                        .getBytes(StandardCharsets.UTF_8);
                rotateIfNeeded(line.length);
                Path path = rootDir.resolve(activeSegment);
                try (BufferedWriter writer = Files.newBufferedWriter(
                        path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    writer.write(new String(line, StandardCharsets.UTF_8));
                }
                activeSize += line.length;
            }
            enforceTotalSizeCap();
        } catch (IOException e) {
            throw new IllegalStateException("log store write failed", e);
        } finally {
            lock.unlock();
        }
    }

    private void initActiveSegment() throws IOException {
        try (Stream<Path> files = Files.list(rootDir)) {
            activeSegment = files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".jsonl"))
                    .max(Comparator.naturalOrder())
                    .orElse(null);
        }
        if (activeSegment == null) {
            activeSegment = newSegmentName();
            Files.createFile(rootDir.resolve(activeSegment));
            activeSize = 0;
            return;
        }
        activeSize = Files.size(rootDir.resolve(activeSegment));
    }

    private void rotateIfNeeded(int incomingBytes) throws IOException {
        if (activeSize + incomingBytes <= segmentBytes) {
            return;
        }
        activeSegment = newSegmentName();
        Path path = rootDir.resolve(activeSegment);
        Files.createFile(path);
        activeSize = 0;
    }

    private void enforceTotalSizeCap() throws IOException {
        long total = directorySize();
        if (total <= totalSizeCap) {
            return;
        }
        try (Stream<Path> files = Files.list(rootDir)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().equals(activeSegment))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    private void applyRetention() throws IOException {
        if (retentionDays <= 0) {
            return;
        }
        Instant cutoff = Instant.now().minusSeconds(retentionDays * 86400L);
        try (Stream<Path> files = Files.list(rootDir)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().equals(activeSegment))
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    private long directorySize() throws IOException {
        try (Stream<Path> files = Files.list(rootDir)) {
            return files.filter(Files::isRegularFile).mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException e) {
                    return 0;
                }
            }).sum();
        }
    }

    private static String newSegmentName() {
        return "events-" + Instant.now().toEpochMilli() + ".jsonl";
    }
}
