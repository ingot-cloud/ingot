package com.ingot.cloud.iam.role;

import java.math.BigInteger;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;

/**
 * <p>提交后的本地唤醒及异步跨节点提示，网络故障不阻塞任务执行或兜底扫描。</p>
 * <p>worker关闭时只广播；可选总线及worker延迟获取以保持配置关闭语义。</p>
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
final class RoleApplicationWakeupNotifier implements SmartLifecycle, AutoCloseable {
    private static final Duration SUBSCRIPTION_RETRY = Duration.ofSeconds(60);
    private static final Duration WARNING_INTERVAL = Duration.ofSeconds(60);
    private static final int MAX_ID_BITS = 63;
    private final Supplier<RoleApplicationScheduler> worker;
    private final Supplier<InvalidationBus> bus;
    private final ExecutorService sender;
    private final ScheduledExecutorService retryTimer;
    private final RoleApplicationWorkerMetrics metrics;
    private volatile boolean running;
    private Subscription subscription;
    private long nextWarning;
    private boolean warned;

    /** 事务成功提交后才收到事件，本地调度先于异步网络调用。 */
    @EventListener
    public void onReady(RoleApplicationReadyEvent event) {
        if (!running) return;
        var local = worker.get();
        if (local != null) local.wake(event.taskId());
        submit(() -> {
            var transport = bus.get();
            if (transport != null) transport.publish(RoleApplicationWakeupEvent.of(event.taskId(), event.roleId()));
        });
    }

    /** 启动异步订阅重试，不在调度线程等待Redis。 */
    @Override
    public void start() {
        running = true;
        retryTimer.scheduleWithFixedDelay(() -> submit(this::subscribe), 0,
                SUBSCRIPTION_RETRY.toNanos(), TimeUnit.NANOSECONDS);
    }

    private void subscribe() {
        synchronized (this) { if (!running || subscription != null) return; }
        if (worker.get() == null) return;
        var transport = bus.get();
        if (transport == null) return;
        var created = transport.subscribe(RoleApplicationWakeupEvent.class, this::onRemote);
        synchronized (this) {
            if (running && subscription == null) subscription = created;
            else created.close();
        }
    }

    private void onRemote(RoleApplicationWakeupEvent event) {
        if (!running || event == null || event.getTaskId() == null) return;
        try {
            var id = new BigInteger(event.getTaskId());
            if (id.signum() <= 0 || id.bitLength() > MAX_ID_BITS) return;
            var local = worker.get();
            if (local != null) local.wakeRemote(id);
        } catch (NumberFormatException invalid) {
            metrics.dropped();
        }
    }

    private void submit(Runnable operation) {
        if (!running) return;
        try {
            sender.execute(() -> {
                if (!running) return;
                try { operation.run(); }
                catch (RuntimeException failure) { warn(failure); }
            });
        } catch (RejectedExecutionException full) {
            metrics.dropped();
            warn(full);
        }
    }

    private synchronized void warn(RuntimeException failure) {
        if (!running) return;
        long now = System.nanoTime();
        if (!warned || now >= nextWarning) {
            warned = true;
            nextWarning = now + WARNING_INTERVAL.toNanos();
            log.warn("角色应用提示发送或订阅失败，持久任务由兜底扫描恢复", failure);
        }
    }

    /** 取消网络重试及订阅，不中断正在执行的业务事务。 */
    @Override
    public void stop() {
        running = false;
        retryTimer.shutdownNow();
        sender.shutdown();
        Subscription closing;
        synchronized (this) {
            closing = subscription;
            subscription = null;
        }
        if (closing != null) {
            try { closing.close(); }
            catch (RuntimeException failure) { log.warn("关闭角色应用提示订阅失败", failure); }
        }
    }

    /** 关闭提示资源。 */
    @Override
    public void close() { stop(); }

    /** 当前是否发送或接受通知。 */
    @Override
    public boolean isRunning() { return running; }
}
