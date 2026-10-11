package com.ingot.cloud.iam.role;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.LongUnaryOperator;

import com.ingot.cloud.iam.persistence.RoleApplicationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

/**
 * <p>有界去重任务调度：按ID连续执行，独立线程低频发现遗漏的持久任务。</p>
 * <p>提示可丢失，所有领取及授权仍由持久执行入口校验；计时采用单调时钟。</p>
 * @author jy
 * @since 1.0.0
 */
@Slf4j
final class RoleApplicationScheduler implements SmartLifecycle, AutoCloseable {
    private static final Duration ERROR_RETRY_MIN = Duration.ofSeconds(60);
    private static final Duration ERROR_RETRY_MAX = Duration.ofSeconds(300);
    private static final Duration MIN_REMOTE_DELAY = Duration.ofMillis(200);
    private static final Duration REMOTE_DELAY_RANGE = Duration.ofMillis(800);
    private static final long SHUTDOWN_WAIT_SECONDS = 30;
    private final RoleApplicationWork work;
    private final RoleApplicationWorkerProperties properties;
    private final ScheduledExecutorService timer;
    private final ExecutorService executor;
    private final ExecutorService scanner;
    private final LongSupplier nanoClock;
    private final LongUnaryOperator randomDelay;
    private final RoleApplicationWorkerMetrics metrics;
    private final Map<BigInteger, Hint> hints = new LinkedHashMap<>();
    private boolean running;
    private boolean busy;
    private long pumpGeneration;
    private ScheduledFuture<?> pumpFuture;
    private ScheduledFuture<?> discoveryFuture;
    private BigInteger cursor = BigInteger.ZERO;
    private Duration errorDelay = ERROR_RETRY_MIN;
    private long nextWarning;
    private boolean warned;

    // 初始化队列指标并明确线程资源所有权，使用显式构造器。
    RoleApplicationScheduler(RoleApplicationWork work, RoleApplicationWorkerProperties properties,
            ScheduledExecutorService timer, ExecutorService executor, ExecutorService scanner,
            LongSupplier nanoClock, LongUnaryOperator randomDelay, RoleApplicationWorkerMetrics metrics) {
        this.work = work;
        this.properties = properties;
        this.timer = timer;
        this.executor = executor;
        this.scanner = scanner;
        this.nanoClock = nanoClock;
        this.randomDelay = randomDelay;
        this.metrics = metrics;
        metrics.registerQueue(this::depth);
    }

    /** 框架启动全部单例后接收提示，并为首次发现随机错峰。 */
    @Override
    public synchronized void start() {
        if (running) return;
        running = true;
        scheduleDiscovery(Duration.ofNanos(randomDelay.applyAsLong(properties.getFallbackScanInterval().toNanos())));
    }

    /** 本地提交提示立即调度；队列满时只丢弃提示，由数据库发现补齐。 */
    public void wake(BigInteger taskId) {
        enqueue(taskId, Duration.ZERO);
    }

    /** 远端提示随机延迟后检查主键领取资格，减少节点同时争抢。 */
    public void wakeRemote(BigInteger taskId) {
        enqueue(taskId, MIN_REMOTE_DELAY.plusNanos(randomDelay.applyAsLong(REMOTE_DELAY_RANGE.toNanos())));
    }

    private synchronized void enqueue(BigInteger taskId, Duration delay) {
        if (!running || taskId == null || taskId.signum() <= 0) return;
        long due = nanoClock.getAsLong() + delay.toNanos();
        var existing = hints.get(taskId);
        if (existing != null) {
            if (existing.executing) existing.dirty = true;
            else if (due < existing.due) existing.due = due;
        } else {
            if (hints.size() >= properties.getQueueCapacity()) {
                metrics.dropped();
                return;
            }
            hints.put(taskId, new Hint(taskId, due));
        }
        schedulePump();
    }

    private void schedulePump() {
        if (!running || busy) return;
        var next = hints.values().stream().filter(hint -> !hint.executing)
                .min(Comparator.comparingLong(hint -> hint.due)).orElse(null);
        if (next == null) return;
        if (pumpFuture != null) pumpFuture.cancel(false);
        long generation = ++pumpGeneration;
        long delay = Math.max(0, next.due - nanoClock.getAsLong());
        pumpFuture = timer.schedule(() -> pump(generation), delay, TimeUnit.NANOSECONDS);
    }

    private void pump(long generation) {
        Hint next;
        synchronized (this) {
            if (!running || busy || generation != pumpGeneration) return;
            pumpFuture = null;
            next = hints.values().stream().filter(hint -> !hint.executing)
                    .min(Comparator.comparingLong(hint -> hint.due)).orElse(null);
            if (next == null) return;
            if (next.due > nanoClock.getAsLong()) { schedulePump(); return; }
            busy = true;
            next.executing = true;
        }
        try {
            executor.execute(() -> execute(next));
        } catch (RejectedExecutionException closed) {
            complete(next, false);
        }
    }

    private void execute(Hint hint) {
        if (!isRunning()) { complete(hint, false); return; }
        boolean more = false;
        var sample = metrics.startBatch();
        try {
            var result = work.executeTask(hint.taskId);
            more = result.more();
            if (result.leaseConflict()) metrics.conflict();
        } catch (RuntimeException failure) {
            metrics.batchError();
            warn("角色应用任务执行失败，保留持久任务等待兜底发现 task=" + hint.taskId, failure);
        } finally {
            metrics.finishBatch(sample);
            complete(hint, more);
        }
    }

    private synchronized void complete(Hint hint, boolean more) {
        busy = false;
        if (!running) return;
        if (more || hint.dirty) {
            hint.executing = false;
            hint.due = nanoClock.getAsLong() + (more ? properties.getActiveBatchDelay().toNanos() : 0);
            hint.dirty = false;
        } else {
            hints.remove(hint.taskId);
        }
        schedulePump();
    }

    private synchronized void scheduleDiscovery(Duration delay) {
        if (!running) return;
        discoveryFuture = timer.schedule(() -> {
            try {
                scanner.execute(this::discover);
            } catch (RejectedExecutionException closed) {
                // 生命周期结束时不再安排扫描。
            }
        }, delay.toNanos(), TimeUnit.NANOSECONDS);
    }

    private void discover() {
        Duration nextDelay = properties.getFallbackScanInterval();
        try {
            while (isRunning()) {
                int capacity = properties.getQueueCapacity() - depth();
                if (capacity <= 0) break;
                int size = Math.min(capacity, RoleApplicationRepository.DISCOVERY_PAGE_SIZE);
                metrics.discovery();
                var found = work.discover(cursor, size);
                for (var id : found) {
                    wake(id);
                    cursor = id;
                }
                if (found.size() < size) { cursor = BigInteger.ZERO; break; }
            }
            errorDelay = ERROR_RETRY_MIN;
        } catch (RuntimeException failure) {
            metrics.discoveryError();
            nextDelay = errorDelay;
            errorDelay = errorDelay.multipliedBy(2).compareTo(ERROR_RETRY_MAX) > 0
                    ? ERROR_RETRY_MAX : errorDelay.multipliedBy(2);
            warn("角色应用兜底发现失败，将退避重试", failure);
        } finally {
            scheduleDiscovery(nextDelay);
        }
    }

    private synchronized int depth() { return hints.size(); }

    private synchronized void warn(String message, RuntimeException failure) {
        if (!running) return;
        long now = nanoClock.getAsLong();
        if (!warned || now >= nextWarning) {
            warned = true;
            nextWarning = now + ERROR_RETRY_MIN.toNanos();
            log.warn(message, failure);
        }
    }

    /** 关闭提示入口，取消定时器并允许当前事务最多等待30秒完成。 */
    @Override
    public void stop() {
        synchronized (this) {
            if (!running) return;
            running = false;
            pumpGeneration++;
            if (pumpFuture != null) pumpFuture.cancel(false);
            if (discoveryFuture != null) discoveryFuture.cancel(false);
            hints.clear();
        }
        timer.shutdownNow();
        scanner.shutdown();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS)) {
                log.warn("角色应用批次尚未结束，保留租约；不强制中断事务");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /** Spring生命周期完成后关闭资源；重复关闭无额外操作。 */
    @Override
    public void close() { stop(); }

    /** 当前是否接受新提示。 */
    @Override
    public synchronized boolean isRunning() { return running; }

    /**
     * <p>一个合并后的本地提示，执行中保留标记防止重入。</p>
     * @author jy
     * @since 1.0.0
     */
    private static final class Hint {
        private final BigInteger taskId;
        private long due;
        private boolean executing;
        private boolean dirty;

        private Hint(BigInteger taskId, long due) {
            this.taskId = taskId;
            this.due = due;
        }
    }
}
