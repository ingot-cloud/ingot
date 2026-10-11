package com.ingot.cloud.iam.role;

import java.time.Duration;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <p>单调虚拟时钟与可控执行器，不通过真实等待验证分钟级调度。</p>
 * @author jy
 * @since 1.0.0
 */
final class ManualRoleApplicationRuntime {
    final ScheduledExecutorService timer = mock(ScheduledExecutorService.class);
    final ExecutorService executor = mock(ExecutorService.class);
    final ExecutorService scanner = mock(ExecutorService.class);
    final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    final RoleApplicationWorkerMetrics metrics = new RoleApplicationWorkerMetrics(registry);
    private final PriorityQueue<Tick> ticks = new PriorityQueue<>(Comparator.comparingLong(Tick::due).thenComparingLong(Tick::sequence));
    long now;
    private long sequence;

    ManualRoleApplicationRuntime() {
        when(timer.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenAnswer(invocation ->
                schedule(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));
        doAnswer(invocation -> { invocation.<Runnable>getArgument(0).run(); return null; }).when(executor).execute(any());
        doAnswer(invocation -> { invocation.<Runnable>getArgument(0).run(); return null; }).when(scanner).execute(any());
        try { when(executor.awaitTermination(anyLong(), any())).thenReturn(true); }
        catch (InterruptedException impossible) { throw new AssertionError(impossible); }
    }

    RoleApplicationScheduler worker(RoleApplicationWork work, RoleApplicationWorkerProperties properties, long initialDelay) {
        properties.validate();
        return new RoleApplicationScheduler(work, properties, timer, executor, scanner, () -> now,
                bound -> Math.min(initialDelay, bound - 1), metrics);
    }

    void advance(Duration duration) {
        long end = now + duration.toNanos();
        int guard = 10_000;
        while (!ticks.isEmpty() && ticks.peek().due <= end) {
            if (--guard == 0) throw new AssertionError("Unexpected scheduler loop");
            var tick = ticks.remove();
            now = tick.due;
            if (!tick.cancelled.get()) tick.operation.run();
        }
        now = end;
    }

    private ScheduledFuture<?> schedule(Runnable operation, long delay, TimeUnit unit) {
        var cancelled = new AtomicBoolean();
        var future = mock(ScheduledFuture.class);
        when(future.cancel(anyBoolean())).thenAnswer(invocation -> { cancelled.set(true); return true; });
        when(future.isCancelled()).thenAnswer(invocation -> cancelled.get());
        ticks.add(new Tick(now + unit.toNanos(delay), sequence++, operation, cancelled));
        return future;
    }

    /**
     * <p>一个可取消的虚拟定时回调。</p>
     * @author jy
     * @since 1.0.0
     */
    private record Tick(long due, long sequence, Runnable operation, AtomicBoolean cancelled) { }
}
