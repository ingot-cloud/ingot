package com.ingot.framework.security.recording.spool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.recording.config.SecurityEventProperties;
import com.ingot.framework.security.recording.model.ClaimedRecord;
import com.ingot.framework.security.recording.model.EnqueueResult;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.segment.ByteSizeParser;
import com.ingot.framework.security.recording.segment.SegmentRecordCodec;
import com.ingot.framework.security.recording.spi.RecordQueue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * <p>DURABLE file spool 实现：append-only segment、checksum、claim/ack/nack 与配额控制。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class FileSpoolRecordQueue implements RecordQueue<SecurityEventRecord> {

    private final Path rootDir;
    private final Path segmentsDir;
    private final Path quarantineDir;
    private final Path stateFile;
    private final ObjectMapper objectMapper;
    private final long maxBytes;
    private final long segmentBytes;
    private final long retryInitialMs;
    private final long retryMaxMs;
    private final ReentrantLock lock = new ReentrantLock();
    private final SpoolState state;

    public FileSpoolRecordQueue(SecurityEventProperties properties, ObjectMapper objectMapper)
            throws IOException {
        this.objectMapper = (objectMapper == null ? new ObjectMapper() : objectMapper.copy())
                .findAndRegisterModules();
        SecurityEventProperties.Spool spool = properties.getDelivery().getSpool();
        this.rootDir = Path.of(spool.getDirectory()).toAbsolutePath().normalize();
        this.segmentsDir = rootDir.resolve("segments");
        this.quarantineDir = rootDir.resolve("quarantine");
        this.stateFile = rootDir.resolve("state.json");
        this.maxBytes = ByteSizeParser.parse(spool.getMaxBytes(), 1024L * 1024L * 1024L);
        this.segmentBytes = ByteSizeParser.parse(spool.getSegmentBytes(), 64L * 1024L * 1024L);
        this.retryInitialMs = Math.max(spool.getRetryInitialMs(), 1);
        this.retryMaxMs = Math.max(spool.getRetryMaxMs(), retryInitialMs);
        Files.createDirectories(segmentsDir);
        Files.createDirectories(quarantineDir);
        this.state = SpoolState.load(stateFile, this.objectMapper);
        SpoolRecovery.recoverOnStartup(this);
    }

    Path rootDir() {
        return rootDir;
    }

    SpoolState state() {
        return state;
    }

    long maxBytes() {
        return maxBytes;
    }

    long segmentBytes() {
        return segmentBytes;
    }

    Path segmentsDir() {
        return segmentsDir;
    }

    Path quarantineDir() {
        return quarantineDir;
    }

    Path stateFile() {
        return stateFile;
    }

    ObjectMapper objectMapper() {
        return objectMapper;
    }

    @Override
    public EnqueueResult enqueue(SecurityEventRecord record) {
        lock.lock();
        try {
            if (state.getTotalBytes() >= maxBytes) {
                return EnqueueResult.rejected("spool quota exceeded");
            }
            byte[] payload = objectMapper.writeValueAsBytes(record);
            int recordBytes = SegmentRecordCodec.HEADER_BYTES + payload.length;
            rotateIfNeeded(recordBytes);
            String segmentName = ensureActiveSegment();
            Path segmentPath = segmentsDir.resolve(segmentName);
            long offset = Files.size(segmentPath);
            try (OutputStream out = Files.newOutputStream(
                    segmentPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                SegmentRecordCodec.writeRecord(out, payload);
            }
            SpoolState.SpoolEntry entry = new SpoolState.SpoolEntry(segmentName, offset, recordBytes, record);
            state.getPending().add(entry);
            state.setTotalBytes(state.getTotalBytes() + recordBytes);
            persistState();
            return EnqueueResult.success();
        } catch (IOException e) {
            return EnqueueResult.rejected("spool write failed: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<ClaimedRecord<SecurityEventRecord>> claim(int limit, Duration wait) {
        lock.lock();
        try {
            long now = Instant.now().toEpochMilli();
            List<ClaimedRecord<SecurityEventRecord>> claimed = new ArrayList<>(Math.max(limit, 1));
            var iterator = state.getPending().iterator();
            while (iterator.hasNext() && claimed.size() < limit) {
                SpoolState.SpoolEntry entry = iterator.next();
                if (entry.getNextRetryAtEpochMs() > now) {
                    continue;
                }
                iterator.remove();
                state.getInFlight().add(entry);
                claimed.add(new ClaimedRecord<>(entry.getClaimId(), entry.getRecord()));
            }
            if (!claimed.isEmpty()) {
                persistState();
            }
            return claimed;
        } catch (IOException e) {
            return List.of();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void ack(List<String> claimIds) {
        if (claimIds == null || claimIds.isEmpty()) {
            return;
        }
        lock.lock();
        try {
            state.getInFlight().removeIf(entry -> claimIds.contains(entry.getClaimId()));
            deleteAcknowledgedSegments();
            persistState();
        } catch (IOException ignored) {
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void nack(List<String> claimIds, Throwable cause) {
        if (claimIds == null || claimIds.isEmpty()) {
            return;
        }
        lock.lock();
        try {
            List<SpoolState.SpoolEntry> requeue = new ArrayList<>();
            state.getInFlight().removeIf(entry -> {
                if (!claimIds.contains(entry.getClaimId())) {
                    return false;
                }
                int attempts = entry.getAttempts() + 1;
                entry.setAttempts(attempts);
                long backoff = Math.min(retryInitialMs * (1L << Math.min(attempts, 10)), retryMaxMs);
                entry.setNextRetryAtEpochMs(Instant.now().toEpochMilli() + backoff);
                requeue.add(entry);
                return true;
            });
            state.getPending().addAll(requeue);
            persistState();
        } catch (IOException ignored) {
        } finally {
            lock.unlock();
        }
    }

    public double usageRatio() {
        return maxBytes <= 0 ? 0.0 : Math.min(1.0, (double) state.getTotalBytes() / maxBytes);
    }

    public long pendingCount() {
        return state.getPending().size();
    }

    void persistState() throws IOException {
        state.save(stateFile, objectMapper);
    }

    private void rotateIfNeeded(int incomingBytes) throws IOException {
        String active = state.getActiveSegment();
        if (active == null) {
            return;
        }
        Path activePath = segmentsDir.resolve(active);
        if (!Files.exists(activePath)) {
            return;
        }
        long currentSize = Files.size(activePath);
        if (currentSize + incomingBytes > segmentBytes) {
            state.setActiveSegment(null);
        }
    }

    private String ensureActiveSegment() throws IOException {
        if (state.getActiveSegment() != null) {
            return state.getActiveSegment();
        }
        String name = "segment-" + Instant.now().toEpochMilli() + ".spool";
        Path path = segmentsDir.resolve(name);
        Files.createFile(path);
        state.setActiveSegment(name);
        return name;
    }

    private void deleteAcknowledgedSegments() throws IOException {
        List<String> referenced = new ArrayList<>();
        for (SpoolState.SpoolEntry entry : state.getPending()) {
            referenced.add(entry.getSegment());
        }
        for (SpoolState.SpoolEntry entry : state.getInFlight()) {
            referenced.add(entry.getSegment());
        }
        try (Stream<Path> files = Files.list(segmentsDir)) {
            files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".spool"))
                    .filter(name -> !name.equals(state.getActiveSegment()))
                    .filter(name -> referenced.stream().noneMatch(name::equals))
                    .sorted(Comparator.naturalOrder())
                    .forEach(name -> {
                        try {
                            Files.deleteIfExists(segmentsDir.resolve(name));
                        } catch (IOException ignored) {
                        }
                    });
        }
        recalculateTotalBytes();
    }

    void refreshTotalBytes() throws IOException {
        recalculateTotalBytes();
    }

    private void recalculateTotalBytes() throws IOException {
        long total = 0;
        try (Stream<Path> files = Files.list(segmentsDir)) {
            for (Path path : files.toList()) {
                if (Files.isRegularFile(path)) {
                    total += Files.size(path);
                }
            }
        }
        state.setTotalBytes(total);
    }

    void quarantineSegment(String segmentName) throws IOException {
        Path source = segmentsDir.resolve(segmentName);
        if (!Files.exists(source)) {
            return;
        }
        Path target = quarantineDir.resolve(segmentName + "-" + Instant.now().toEpochMilli());
        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        state.getPending().removeIf(entry -> segmentName.equals(entry.getSegment()));
        state.getInFlight().removeIf(entry -> segmentName.equals(entry.getSegment()));
        if (segmentName.equals(state.getActiveSegment())) {
            state.setActiveSegment(null);
        }
        recalculateTotalBytes();
        persistState();
    }
}
