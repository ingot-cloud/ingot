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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * <p>DURABLE file spool：按应用隔离目录、append-only segment、claim/ack/nack 与配额控制。</p>
 *
 * <p>完好 {@code state.json} 表示消费位点；pending 与 inFlight 皆空时启动不得扫描 segment。
 * 同目录仅允许一个 writer（{@code spool.lock}）。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see SpoolRecovery
 */
public final class FileSpoolRecordQueue implements RecordQueue<SecurityEventRecord>, AutoCloseable {

    /** {@code spring.application.name} 缺失时的子目录名。 */
    public static final String DEFAULT_APPLICATION_NAME = "application";

    static final String LOCK_FILE_NAME = "spool.lock";
    static final String STATE_FILE_NAME = "state.json";
    static final String SEGMENTS_DIR_NAME = "segments";
    static final String QUARANTINE_DIR_NAME = "quarantine";

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
    private final Condition pendingAvailable = lock.newCondition();
    private final SpoolState state;
    private final FileChannel lockChannel;
    private final FileLock fileLock;

    /**
     * 使用配置目录作为 spool 根（单测或已隔离的绝对路径）。
     *
     * @param properties   recording 配置，读取 {@code delivery.spool}
     * @param objectMapper 序列化 segment payload；{@code null} 时新建并注册 JSR-310 模块
     * @throws IOException 创建目录、获取独占锁或启动恢复失败
     */
    public FileSpoolRecordQueue(SecurityEventProperties properties, ObjectMapper objectMapper)
            throws IOException {
        this(properties, objectMapper, null);
    }

    /**
     * 在 {@code delivery.spool.directory} 下再拼 {@code applicationName} 作为实际根目录。
     *
     * @param properties      recording 配置
     * @param objectMapper    序列化 segment payload；{@code null} 时新建并注册 JSR-310 模块
     * @param applicationName {@code spring.application.name}；空白则不拼子目录
     * @throws IOException 创建目录、获取独占锁或启动恢复失败
     */
    public FileSpoolRecordQueue(
            SecurityEventProperties properties,
            ObjectMapper objectMapper,
            String applicationName)
            throws IOException {
        this.objectMapper = (objectMapper == null ? new ObjectMapper() : objectMapper.copy())
                .findAndRegisterModules();
        SecurityEventProperties.Spool spool = properties.getDelivery().getSpool();
        Path base = Path.of(spool.getDirectory()).toAbsolutePath().normalize();
        this.rootDir = resolveRootDir(base, applicationName);
        this.segmentsDir = rootDir.resolve(SEGMENTS_DIR_NAME);
        this.quarantineDir = rootDir.resolve(QUARANTINE_DIR_NAME);
        this.stateFile = rootDir.resolve(STATE_FILE_NAME);
        this.maxBytes = ByteSizeParser.parse(spool.getMaxBytes(), 1024L * 1024L * 1024L);
        this.segmentBytes = ByteSizeParser.parse(spool.getSegmentBytes(), 64L * 1024L * 1024L);
        this.retryInitialMs = Math.max(spool.getRetryInitialMs(), 1);
        this.retryMaxMs = Math.max(spool.getRetryMaxMs(), retryInitialMs);
        Files.createDirectories(segmentsDir);
        Files.createDirectories(quarantineDir);
        this.lockChannel = FileChannel.open(
                rootDir.resolve(LOCK_FILE_NAME),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);
        try {
            this.fileLock = tryAcquireLock();
        } catch (IOException e) {
            lockChannel.close();
            throw e;
        }
        SpoolState.LoadResult loadResult = SpoolState.load(stateFile, this.objectMapper);
        this.state = loadResult.state();
        try {
            SpoolRecovery.recoverOnStartup(this, loadResult);
        } catch (IOException e) {
            closeQuietly();
            throw e;
        }
    }

    private FileLock tryAcquireLock() throws IOException {
        try {
            FileLock acquired = lockChannel.tryLock();
            if (acquired == null) {
                throw new IOException("spool directory already in use: " + rootDir);
            }
            return acquired;
        } catch (OverlappingFileLockException e) {
            throw new IOException("spool directory already in use: " + rootDir, e);
        }
    }

    private static Path resolveRootDir(Path base, String applicationName) {
        if (applicationName == null || applicationName.isBlank()) {
            return base;
        }
        String sanitized = sanitizeApplicationName(applicationName);
        return base.resolve(sanitized);
    }

    static String sanitizeApplicationName(String applicationName) {
        String trimmed = applicationName.trim();
        if (trimmed.isEmpty()
                || trimmed.contains("..")
                || trimmed.indexOf('/') >= 0
                || trimmed.indexOf('\\') >= 0) {
            return DEFAULT_APPLICATION_NAME;
        }
        return trimmed;
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

    /** {@inheritDoc} */
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
            pendingAvailable.signalAll();
            persistState();
            return EnqueueResult.success();
        } catch (IOException e) {
            return EnqueueResult.rejected("spool write failed: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<ClaimedRecord<SecurityEventRecord>> claim(int limit, Duration wait) {
        if (limit <= 0) {
            return List.of();
        }
        long timeoutNanos = wait.isNegative() ? 0 : wait.toNanos();
        long deadlineNanos = System.nanoTime() + timeoutNanos;
        lock.lock();
        try {
            while (true) {
                long now = Instant.now().toEpochMilli();
                List<ClaimedRecord<SecurityEventRecord>> claimed = new ArrayList<>(limit);
                List<SpoolState.SpoolEntry> moved = new ArrayList<>();
                var iterator = state.getPending().iterator();
                while (iterator.hasNext() && claimed.size() < limit) {
                    SpoolState.SpoolEntry entry = iterator.next();
                    if (entry.getNextRetryAtEpochMs() > now) {
                        continue;
                    }
                    if (entry.getRecord() == null) {
                        continue;
                    }
                    iterator.remove();
                    state.getInFlight().add(entry);
                    moved.add(entry);
                    claimed.add(new ClaimedRecord<>(entry.getClaimId(), entry.getRecord()));
                }
                if (!claimed.isEmpty()) {
                    try {
                        persistState();
                        return claimed;
                    } catch (IOException e) {
                        state.getInFlight().removeAll(moved);
                        state.getPending().addAll(moved);
                        return List.of();
                    }
                }

                long remainingNanos = deadlineNanos - System.nanoTime();
                if (remainingNanos <= 0) {
                    return List.of();
                }
                try {
                    pendingAvailable.awaitNanos(remainingNanos);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return List.of();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void ack(List<String> claimIds) {
        if (claimIds == null || claimIds.isEmpty()) {
            return;
        }
        lock.lock();
        try {
            List<SpoolState.SpoolEntry> acked = new ArrayList<>();
            state.getInFlight().removeIf(entry -> {
                if (!claimIds.contains(entry.getClaimId())) {
                    return false;
                }
                acked.add(entry);
                return true;
            });
            if (acked.isEmpty()) {
                return;
            }
            try {
                persistState();
            } catch (IOException e) {
                state.getPending().addAll(acked);
                return;
            }
            try {
                reclaimUnreferencedSegments();
                persistState();
            } catch (IOException ignored) {
                // 消费位点已落盘；容量统计下次启动 refreshTotalBytes
            }
        } finally {
            lock.unlock();
        }
    }

    /** {@inheritDoc} */
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
            if (!requeue.isEmpty()) {
                pendingAvailable.signalAll();
            }
            persistState();
        } catch (IOException ignored) {
            // 内存已退回 pending；下次成功 persist 或重启 inFlight 回收与磁盘对齐
        } finally {
            lock.unlock();
        }
    }

    /**
     * 当前占用相对 {@code max-bytes} 配额的比例，上限 1.0。
     *
     * @return 0 到 1；{@code max-bytes} 非法时为 0
     */
    public double usageRatio() {
        return maxBytes <= 0 ? 0.0 : Math.min(1.0, (double) state.getTotalBytes() / maxBytes);
    }

    /**
     * 尚未 claim 的 pending 条数，不含 inFlight。
     *
     * @return pending 列表大小
     */
    public long pendingCount() {
        return state.getPending().size();
    }

    /**
     * 释放目录独占锁。Spring 销毁 Bean 时调用；单测重启前必须先关闭上一实例。
     *
     * @throws IOException 释放文件锁或关闭通道失败
     */
    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            if (fileLock != null && fileLock.isValid()) {
                fileLock.release();
            }
            if (lockChannel != null) {
                lockChannel.close();
            }
        } finally {
            lock.unlock();
        }
    }

    void persistState() throws IOException {
        state.save(stateFile, objectMapper);
    }

    void hydrateRecords() throws IOException {
        hydrateList(state.getPending());
        hydrateList(state.getInFlight());
    }

    private void hydrateList(List<SpoolState.SpoolEntry> entries) throws IOException {
        Set<String> failedSegments = new HashSet<>();
        for (SpoolState.SpoolEntry entry : List.copyOf(entries)) {
            if (entry.getRecord() != null || failedSegments.contains(entry.getSegment())) {
                continue;
            }
            try {
                entry.setRecord(readRecordAt(entry.getSegment(), entry.getOffset()));
            } catch (IOException e) {
                failedSegments.add(entry.getSegment());
                quarantineSegment(entry.getSegment());
            }
        }
    }

    private SecurityEventRecord readRecordAt(String segmentName, long offset) throws IOException {
        Path path = segmentsDir.resolve(segmentName);
        try (InputStream in = Files.newInputStream(path)) {
            in.skipNBytes(offset);
            byte[] payload = SegmentRecordCodec.readRecord(in);
            if (payload == null) {
                throw new IOException("no record at offset " + offset + " in " + segmentName);
            }
            return objectMapper.readValue(payload, SecurityEventRecord.class);
        }
    }

    void quarantineStateFile() throws IOException {
        long epoch = Instant.now().toEpochMilli();
        if (Files.exists(stateFile)) {
            Files.move(
                    stateFile,
                    quarantineDir.resolve(STATE_FILE_NAME + "-" + epoch),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        Path tmp = stateFile.resolveSibling(stateFile.getFileName() + ".tmp");
        if (Files.exists(tmp)) {
            Files.move(
                    tmp,
                    quarantineDir.resolve(tmp.getFileName() + "-" + epoch),
                    StandardCopyOption.REPLACE_EXISTING);
        }
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
            Path activePath = segmentsDir.resolve(state.getActiveSegment());
            if (!Files.exists(activePath)) {
                Files.createFile(activePath);
            }
            return state.getActiveSegment();
        }
        String name = "segment-" + Instant.now().toEpochMilli() + ".spool";
        Path path = segmentsDir.resolve(name);
        Files.createFile(path);
        state.setActiveSegment(name);
        return name;
    }

    void reclaimUnreferencedSegments() throws IOException {
        Set<String> referenced = new HashSet<>();
        for (SpoolState.SpoolEntry entry : state.getPending()) {
            referenced.add(entry.getSegment());
        }
        for (SpoolState.SpoolEntry entry : state.getInFlight()) {
            referenced.add(entry.getSegment());
        }
        String active = state.getActiveSegment();
        try (Stream<Path> files = Files.list(segmentsDir)) {
            files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".spool"))
                    .filter(name -> !name.equals(active))
                    .filter(name -> !referenced.contains(name))
                    .sorted(Comparator.naturalOrder())
                    .forEach(name -> {
                        try {
                            Files.deleteIfExists(segmentsDir.resolve(name));
                        } catch (IOException ignored) {
                        }
                    });
        }
        if (active != null && !referenced.contains(active)) {
            Path activePath = segmentsDir.resolve(active);
            if (Files.exists(activePath)) {
                try (FileChannel channel = FileChannel.open(activePath, StandardOpenOption.WRITE)) {
                    channel.truncate(0);
                }
            }
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
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        state.getPending().removeIf(entry -> segmentName.equals(entry.getSegment()));
        state.getInFlight().removeIf(entry -> segmentName.equals(entry.getSegment()));
        if (segmentName.equals(state.getActiveSegment())) {
            state.setActiveSegment(null);
        }
        recalculateTotalBytes();
        persistState();
    }

    private void closeQuietly() {
        try {
            close();
        } catch (IOException ignored) {
        }
    }
}
