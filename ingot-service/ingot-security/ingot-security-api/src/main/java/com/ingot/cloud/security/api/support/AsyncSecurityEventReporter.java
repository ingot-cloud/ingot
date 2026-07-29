package com.ingot.cloud.security.api.support;

import com.ingot.cloud.security.api.config.SecurityEventProperties;
import com.ingot.cloud.security.api.model.dto.SecurityEventReportDTO;
import com.ingot.cloud.security.api.rpc.RemoteSecurityEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 有界队列 + 单消费者攒批，异步上报统一安全事件至 ingot-security。
 *
 * <p>队列满时丢弃并限流打 warn，避免无界堆积导致 OOM；RPC 失败仅记录日志，不重试。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class AsyncSecurityEventReporter implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AsyncSecurityEventReporter.class);

    private final Supplier<RemoteSecurityEventService> remoteSupplier;
    private final String logTag;
    private final int batchSize;
    private final long pollTimeoutMs;
    private final long shutdownTimeoutMs;
    private final BlockingQueue<SecurityEventReportDTO> queue;
    private final AtomicLong droppedCount = new AtomicLong();
    private final Thread worker;
    private volatile boolean running = true;

    public AsyncSecurityEventReporter(
            Supplier<RemoteSecurityEventService> remoteSupplier,
            SecurityEventProperties properties,
            String threadName,
            String logTag) {
        this.remoteSupplier = remoteSupplier;
        this.logTag = logTag;
        SecurityEventProperties.Async async = properties.getAsync() != null
                ? properties.getAsync()
                : new SecurityEventProperties.Async();
        int queueCapacity = async.getQueueCapacity() > 0 ? async.getQueueCapacity() : 2048;
        this.batchSize = async.getBatchSize() > 0 ? async.getBatchSize() : 32;
        this.pollTimeoutMs = async.getPollTimeoutMs() > 0 ? async.getPollTimeoutMs() : 100;
        this.shutdownTimeoutMs = async.getShutdownTimeoutMs() > 0 ? async.getShutdownTimeoutMs() : 5000;
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
        this.worker = new Thread(this::runLoop, threadName);
        this.worker.setDaemon(true);
        this.worker.start();
    }

    /**
     * 非阻塞入队；队列满时丢弃并累计计数。
     */
    public void offer(SecurityEventReportDTO dto) {
        if (dto == null) {
            return;
        }
        if (queue.offer(dto)) {
            return;
        }
        long dropped = droppedCount.incrementAndGet();
        if (dropped == 1 || dropped % 100 == 0) {
            log.warn("[{}] queue full (capacity={}), dropped {} events total",
                    logTag, queue.remainingCapacity() + queue.size(), dropped);
        }
    }

    private void runLoop() {
        List<SecurityEventReportDTO> batch = new ArrayList<>(batchSize);
        while (running || !queue.isEmpty()) {
            try {
                SecurityEventReportDTO first = queue.poll(pollTimeoutMs, TimeUnit.MILLISECONDS);
                if (first == null) {
                    continue;
                }
                batch.add(first);
                queue.drainTo(batch, batchSize - 1);
                flushBatch(batch);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void flushBatch(List<SecurityEventReportDTO> batch) {
        if (batch.isEmpty()) {
            return;
        }
        try {
            RemoteSecurityEventService remote = remoteSupplier.get();
            if (remote == null) {
                log.debug("[{}] RemoteSecurityEventService not available, skip {} events", logTag, batch.size());
                return;
            }
            if (batch.size() == 1) {
                remote.report(batch.get(0));
            } else {
                remote.reportBatch(List.copyOf(batch));
            }
        } catch (Exception e) {
            log.warn("[{}] report failed (batchSize={}): {}", logTag, batch.size(), e.getMessage());
        } finally {
            batch.clear();
        }
    }

    @Override
    public void close() {
        running = false;
        worker.interrupt();
        try {
            worker.join(shutdownTimeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        int remaining = queue.size();
        long dropped = droppedCount.get();
        if (remaining > 0 || dropped > 0) {
            log.warn("[{}] shutdown: {} queued events not reported, {} dropped during lifetime",
                    logTag, remaining, dropped);
        }
    }
}
