package com.ingot.cloud.iam.role;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.InvalidationEvent;
import com.ingot.framework.eventbus.Subscription;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <p>验证本地先唤醒、发送隔离、关闭worker仍广播，以及远端无可信权限数据。</p>
 * @author jy
 * @since 1.0.0
 */
class RoleApplicationWakeupNotifierTest {
    @Test
    void localWakePrecedesAsyncBroadcastAndNetworkFailureDoesNotEscape() {
        var worker = mock(RoleApplicationScheduler.class);
        var bus = mock(InvalidationBus.class);
        var sender = mock(ExecutorService.class);
        var timer = mock(ScheduledExecutorService.class);
        var messages = new ArrayList<Runnable>();
        doAnswer(invocation -> { messages.add(invocation.getArgument(0)); return null; }).when(sender).execute(any());
        doThrow(new IllegalStateException("redis unavailable")).when(bus).publish(any());
        var runtime = new ManualRoleApplicationRuntime();
        try (var notifier = new RoleApplicationWakeupNotifier(() -> worker, () -> bus, sender, timer, runtime.metrics)) {
            notifier.start();
            assertDoesNotThrow(() -> notifier.onReady(new RoleApplicationReadyEvent(BigInteger.ONE, BigInteger.TWO)));
            verify(worker).wake(BigInteger.ONE);
            verify(bus, never()).publish(any());
            assertDoesNotThrow(() -> messages.getFirst().run());
            verify(bus).publish(argThat(event -> event instanceof RoleApplicationWakeupEvent wakeup
                    && wakeup.getTaskId().equals("1") && wakeup.getRoleId().equals("2")));
        }
    }

    @Test
    void disabledLocalWorkerStillBroadcastsAndMissingBusIsSupported() {
        var bus = mock(InvalidationBus.class);
        var runtime = new ManualRoleApplicationRuntime();
        try (var notifier = new RoleApplicationWakeupNotifier(() -> null, () -> bus, runtime.executor, runtime.timer, runtime.metrics)) {
            notifier.start();
            notifier.onReady(new RoleApplicationReadyEvent(BigInteger.ONE, BigInteger.TWO));
            verify(bus).publish(any(RoleApplicationWakeupEvent.class));
        }
        var second = new ManualRoleApplicationRuntime();
        try (var notifier = new RoleApplicationWakeupNotifier(() -> null, () -> null, second.executor, second.timer, second.metrics)) {
            notifier.start();
            assertDoesNotThrow(() -> notifier.onReady(new RoleApplicationReadyEvent(BigInteger.ONE, BigInteger.TWO)));
        }
    }

    @Test
    void remoteHintsOnlyEnqueueValidIdsAndSubscriptionCloses() {
        var worker = mock(RoleApplicationScheduler.class);
        var bus = new CapturingBus();
        var runtime = new ManualRoleApplicationRuntime();
        doAnswer(invocation -> { invocation.<Runnable>getArgument(0).run(); return null; })
                .when(runtime.timer).scheduleWithFixedDelay(any(), anyLong(), anyLong(), any());
        var notifier = new RoleApplicationWakeupNotifier(() -> worker, () -> bus, runtime.executor, runtime.timer, runtime.metrics);
        notifier.start();
        bus.receive(RoleApplicationWakeupEvent.of(BigInteger.ONE, BigInteger.TWO));
        var invalid = new RoleApplicationWakeupEvent();
        invalid.setTaskId("not-an-id"); bus.receive(invalid);
        invalid.setTaskId("-1"); bus.receive(invalid);
        invalid.setTaskId("999999999999999999999999999"); bus.receive(invalid);
        verify(worker, times(1)).wakeRemote(BigInteger.ONE);
        notifier.close();
        assertTrue(bus.closed);
        bus.receive(RoleApplicationWakeupEvent.of(BigInteger.TWO, BigInteger.TWO));
        verify(worker, never()).wakeRemote(BigInteger.TWO);
    }

    @Test
    void saturatedBroadcastQueueOnlyDropsHints() {
        var sender = mock(ExecutorService.class);
        doThrow(new java.util.concurrent.RejectedExecutionException("full")).when(sender).execute(any());
        var runtime = new ManualRoleApplicationRuntime();
        var worker = mock(RoleApplicationScheduler.class);
        try (var notifier = new RoleApplicationWakeupNotifier(() -> worker, () -> null,
                sender, runtime.timer, runtime.metrics)) {
            notifier.start();
            assertDoesNotThrow(() -> notifier.onReady(new RoleApplicationReadyEvent(BigInteger.ONE, BigInteger.TWO)));
            verify(worker).wake(BigInteger.ONE);
            assertEquals(1, runtime.registry.get("iam.role.application.hints.dropped").counter().count());
        }
    }

    /**
     * <p>只捕获订阅边界的传输替身，不提供持久消息语义。</p>
     * @author jy
     * @since 1.0.0
     */
    private static final class CapturingBus implements InvalidationBus {
        private Consumer<RoleApplicationWakeupEvent> handler;
        private boolean closed;

        @Override
        public <E extends InvalidationEvent> void publish(E event) { }

        @Override
        @SuppressWarnings("unchecked")
        public <E extends InvalidationEvent> Subscription subscribe(Class<E> type, Consumer<E> consumer) {
            handler = (Consumer<RoleApplicationWakeupEvent>) consumer;
            return () -> closed = true;
        }

        private void receive(RoleApplicationWakeupEvent event) { handler.accept(event); }
    }
}
