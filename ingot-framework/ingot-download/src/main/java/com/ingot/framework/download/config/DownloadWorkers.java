package com.ingot.framework.download.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.ingot.framework.download.DownloadCenter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

/**
 * <p>下载中心私有的工作线程与清理调度。刻意不注册为 {@code Executor} Bean，避免替换应用默认执行器。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
public class DownloadWorkers implements SmartLifecycle {
    private static final String WORKER_THREAD = "download-worker-";
    private static final String CLEANUP_THREAD = "download-cleanup-";
    private static final long KEEP_ALIVE_SECONDS = 60L;
    private static final long SHUTDOWN_WAIT_SECONDS = 10L;

    private final DownloadProperties properties;
    private final ThreadPoolExecutor workers;
    private ScheduledExecutorService cleanup;
    private volatile boolean running;

    /**
     * 按配置创建有界工作线程池。
     *
     * @param properties 配置
     */
    public DownloadWorkers(DownloadProperties properties) {
        this.properties = properties;
        int threads = Math.max(1, properties.getWorkerThreads());
        this.workers = new ThreadPoolExecutor(threads, threads, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(Math.max(1, properties.getQueueCapacity())), threads(WORKER_THREAD),
                new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * 提交生成任务；队列已满时抛出拒绝异常。
     *
     * @param task 任务
     */
    public void execute(Runnable task) {
        workers.execute(task);
    }

    /**
     * 按配置间隔调度清理。
     *
     * @param center 下载中心
     */
    public synchronized void scheduleCleanup(DownloadCenter center) {
        if (cleanup != null) {
            return;
        }
        long interval = Math.max(1L, properties.getCleanupInterval().toSeconds());
        ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1, threads(CLEANUP_THREAD));
        timer.scheduleWithFixedDelay(() -> runCleanup(center), interval, interval, TimeUnit.SECONDS);
        cleanup = timer;
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public synchronized void stop() {
        running = false;
        if (cleanup != null) {
            cleanup.shutdownNow();
        }
        workers.shutdown();
        try {
            workers.awaitTermination(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private static void runCleanup(DownloadCenter center) {
        try {
            center.cleanup();
        } catch (RuntimeException exception) {
            log.warn("[Download] 清理失败，下一轮重试", exception);
        }
    }

    private static ThreadFactory threads(String prefix) {
        AtomicInteger sequence = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
