package com.ingot.cloud.iam.role;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <p>验证实际调度入口的频率、去重、公平批次、恢复和停止边界。</p>
 * @author jy
 * @since 1.0.0
 */
class RoleApplicationSchedulerTest {
    private static final BigInteger TASK = BigInteger.ONE;

    @Test
    void twoIdleNodesOnlyScanOncePerMinuteWithDifferentStartupPhases() {
        var first = mock(RoleApplicationWork.class);
        var second = mock(RoleApplicationWork.class);
        when(first.discover(any(), anyInt())).thenReturn(List.of());
        when(second.discover(any(), anyInt())).thenReturn(List.of());
        var one = new ManualRoleApplicationRuntime();
        var two = new ManualRoleApplicationRuntime();
        try (var worker1 = one.worker(first, new RoleApplicationWorkerProperties(), 0);
             var worker2 = two.worker(second, new RoleApplicationWorkerProperties(), Duration.ofSeconds(30).toNanos())) {
            worker1.start(); worker2.start();
            one.advance(Duration.ofMinutes(10)); two.advance(Duration.ofMinutes(10));
            verify(first, times(11)).discover(BigInteger.ZERO, 100);
            verify(second, times(10)).discover(BigInteger.ZERO, 100);
            verify(first, never()).executeTask(any());
            verify(second, never()).executeTask(any());
        }
    }

    @Test
    void confirmedTaskRunsImmediatelyAndContinuesByIdWithoutDiscovery() {
        var work = mock(RoleApplicationWork.class);
        when(work.executeTask(TASK)).thenReturn(RoleApplicationWork.BatchResult.MORE,
                RoleApplicationWork.BatchResult.MORE, RoleApplicationWork.BatchResult.STOP);
        var runtime = new ManualRoleApplicationRuntime();
        try (var worker = runtime.worker(work, new RoleApplicationWorkerProperties(), Duration.ofSeconds(59).toNanos())) {
            worker.start(); worker.wake(TASK);
            runtime.advance(Duration.ZERO);
            verify(work, times(1)).executeTask(TASK);
            runtime.advance(Duration.ofMillis(249));
            verify(work, times(1)).executeTask(TASK);
            runtime.advance(Duration.ofMillis(251));
            verify(work, times(3)).executeTask(TASK);
            verify(work, never()).discover(any(), anyInt());
            assertEquals(0, runtime.registry.get("iam.role.application.queue.depth").gauge().value());
        }
    }

    @Test
    void duplicateHintsMergeAndDifferentTasksTakeTurns() {
        var work = mock(RoleApplicationWork.class);
        when(work.executeTask(TASK)).thenReturn(RoleApplicationWork.BatchResult.MORE, RoleApplicationWork.BatchResult.STOP);
        when(work.executeTask(BigInteger.TWO)).thenReturn(RoleApplicationWork.BatchResult.STOP);
        var runtime = new ManualRoleApplicationRuntime();
        try (var worker = runtime.worker(work, new RoleApplicationWorkerProperties(), Duration.ofSeconds(59).toNanos())) {
            worker.start();
            for (int i = 0; i < 20; i++) worker.wake(TASK);
            worker.wake(BigInteger.TWO);
            runtime.advance(Duration.ofSeconds(1));
            var ordered = inOrder(work);
            ordered.verify(work).executeTask(TASK);
            ordered.verify(work).executeTask(BigInteger.TWO);
            ordered.verify(work).executeTask(TASK);
            verify(work, times(2)).executeTask(TASK);
        }
    }

    @Test
    void hintArrivingWhileBatchFinishesIsNotLost() {
        var work = mock(RoleApplicationWork.class);
        var runtime = new ManualRoleApplicationRuntime();
        try (var worker = runtime.worker(work, new RoleApplicationWorkerProperties(), Duration.ofSeconds(59).toNanos())) {
            when(work.executeTask(TASK)).thenAnswer(invocation -> {
                worker.wake(TASK);
                return RoleApplicationWork.BatchResult.STOP;
            }).thenReturn(RoleApplicationWork.BatchResult.STOP);
            worker.start(); worker.wake(TASK); runtime.advance(Duration.ZERO);
            verify(work, times(2)).executeTask(TASK);
        }
    }

    @Test
    void overflowOnlyDropsHintAndFallbackFindsThePersistedTask() {
        var work = mock(RoleApplicationWork.class);
        when(work.executeTask(any())).thenReturn(RoleApplicationWork.BatchResult.STOP);
        var third = BigInteger.valueOf(3);
        when(work.discover(any(), anyInt())).thenReturn(List.of(third));
        var properties = new RoleApplicationWorkerProperties();
        properties.setQueueCapacity(2);
        var runtime = new ManualRoleApplicationRuntime();
        try (var worker = runtime.worker(work, properties, Duration.ofSeconds(59).toNanos())) {
            worker.start(); worker.wake(TASK); worker.wake(BigInteger.TWO); worker.wake(third);
            runtime.advance(Duration.ZERO);
            verify(work, never()).executeTask(third);
            assertEquals(1, runtime.registry.get("iam.role.application.hints.dropped").counter().count());
            runtime.advance(Duration.ofSeconds(60));
            verify(work).executeTask(third);
        }
    }

    @Test
    void remoteHintWaitsForJitterAndLostLeaseDoesNotBusyRetry() {
        var work = mock(RoleApplicationWork.class);
        when(work.executeTask(TASK)).thenReturn(RoleApplicationWork.BatchResult.CONFLICT);
        var runtime = new ManualRoleApplicationRuntime();
        try (var worker = runtime.worker(work, new RoleApplicationWorkerProperties(), Duration.ofSeconds(59).toNanos())) {
            worker.start(); worker.wakeRemote(TASK);
            runtime.advance(Duration.ofMillis(999));
            verify(work, never()).executeTask(TASK);
            runtime.advance(Duration.ofMillis(1));
            verify(work).executeTask(TASK);
            assertEquals(1, runtime.registry.get("iam.role.application.lease.conflicts").counter().count());
        }
    }

    @Test
    void databaseErrorsBackOffAndSuccessRestoresNormalInterval() {
        var work = mock(RoleApplicationWork.class);
        when(work.discover(any(), anyInt())).thenThrow(new IllegalStateException("database unavailable"),
                new IllegalStateException("database unavailable"), new IllegalStateException("database unavailable"),
                new IllegalStateException("database unavailable")).thenReturn(List.of());
        var runtime = new ManualRoleApplicationRuntime();
        try (var worker = runtime.worker(work, new RoleApplicationWorkerProperties(), 0)) {
            worker.start();
            runtime.advance(Duration.ofSeconds(719));
            verify(work, times(4)).discover(any(), anyInt());
            runtime.advance(Duration.ofSeconds(1));
            verify(work, times(5)).discover(any(), anyInt());
            runtime.advance(Duration.ofSeconds(60));
            verify(work, times(6)).discover(any(), anyInt());
        }
    }

    @Test
    void stoppingRejectsHintsAndCancelsPendingBatchesAndDiscovery() {
        var work = mock(RoleApplicationWork.class);
        when(work.executeTask(TASK)).thenReturn(RoleApplicationWork.BatchResult.MORE);
        var runtime = new ManualRoleApplicationRuntime();
        var worker = runtime.worker(work, new RoleApplicationWorkerProperties(), Duration.ofSeconds(59).toNanos());
        worker.start(); worker.wake(TASK); runtime.advance(Duration.ZERO);
        worker.close(); worker.wake(TASK); runtime.advance(Duration.ofMinutes(2));
        verify(work, times(1)).executeTask(TASK);
        verify(work, never()).discover(any(), anyInt());
        verify(runtime.executor).shutdown();
    }
}
