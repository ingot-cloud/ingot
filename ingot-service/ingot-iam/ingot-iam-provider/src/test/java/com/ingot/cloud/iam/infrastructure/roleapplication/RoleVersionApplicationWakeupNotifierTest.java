package com.ingot.cloud.iam.infrastructure.roleapplication;

import com.ingot.cloud.iam.domain.roleapplication.RoleVersionApplicationReadyEvent;
import com.ingot.cloud.iam.domain.roleapplication.RoleVersionApplicationWakeupEvent;

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
class RoleVersionApplicationWakeupNotifierTest {
    @Test
    void localWakePrecedesAsyncBroadcastAndNetworkFailureDoesNotEscape() {
        var worker = mock(RoleVersionApplicationScheduler.class);
        var bus = mock(InvalidationBus.class);
        var sender = mock(ExecutorService.class);
        var timer = mock(ScheduledExecutorService.class);
        var messages = new ArrayList<Runnable>();
        doAnswer(invocation -> { messages.add(invocation.getArgument(0)); return null; }).when(sender).execute(any());
        doThrow(new IllegalStateException("redis unavailable")).when(bus).publish(any());
        var runtime = new ManualRoleVersionApplicationRuntime();
        try (var notifier = new RoleVersionApplicationWakeupNotifier(() -> worker, () -> bus, sender, timer, runtime.metrics)) {
            notifier.start();
            assertDoesNotThrow(() -> notifier.onReady(new RoleVersionApplicationReadyEvent(BigInteger.ONE, BigInteger.TWO)));
            verify(worker).wake(BigInteger.ONE);
            verify(bus, never()).publish(any());
            assertDoesNotThrow(() -> messages.getFirst().run());
            verify(bus).publish(argThat(event -> event instanceof RoleVersionApplicationWakeupEvent wakeup
                    && wakeup.getTaskId().equals("1") && wakeup.getRoleId().equals("2")));
        }
    }

    @Test
    void disabledLocalWorkerStillBroadcastsAndMissingBusIsSupported() {
        var bus = mock(InvalidationBus.class);
        var runtime = new ManualRoleVersionApplicationRuntime();
        try (var notifier = new RoleVersionApplicationWakeupNotifier(() -> null, () -> bus, runtime.executor, runtime.timer, runtime.metrics)) {
            notifier.start();
            notifier.onReady(new RoleVersionApplicationReadyEvent(BigInteger.ONE, BigInteger.TWO));
            verify(bus).publish(any(RoleVersionApplicationWakeupEvent.class));
        }
        var second = new ManualRoleVersionApplicationRuntime();
        try (var notifier = new RoleVersionApplicationWakeupNotifier(() -> null, () -> null, second.executor, second.timer, second.metrics)) {
            notifier.start();
            assertDoesNotThrow(() -> notifier.onReady(new RoleVersionApplicationReadyEvent(BigInteger.ONE, BigInteger.TWO)));
        }
    }

    @Test
    void remoteHintsOnlyEnqueueValidIdsAndSubscriptionCloses() {
        var worker = mock(RoleVersionApplicationScheduler.class);
        var bus = new CapturingBus();
        var runtime = new ManualRoleVersionApplicationRuntime();
        doAnswer(invocation -> { invocation.<Runnable>getArgument(0).run(); return null; })
                .when(runtime.timer).scheduleWithFixedDelay(any(), anyLong(), anyLong(), any());
        var notifier = new RoleVersionApplicationWakeupNotifier(() -> worker, () -> bus, runtime.executor, runtime.timer, runtime.metrics);
        notifier.start();
        bus.receive(RoleVersionApplicationWakeupEvent.of(BigInteger.ONE, BigInteger.TWO));
        var invalid = new RoleVersionApplicationWakeupEvent();
        invalid.setTaskId("not-an-id"); bus.receive(invalid);
        invalid.setTaskId("-1"); bus.receive(invalid);
        invalid.setTaskId("999999999999999999999999999"); bus.receive(invalid);
        verify(worker, times(1)).wakeRemote(BigInteger.ONE);
        notifier.close();
        assertTrue(bus.closed);
        bus.receive(RoleVersionApplicationWakeupEvent.of(BigInteger.TWO, BigInteger.TWO));
        verify(worker, never()).wakeRemote(BigInteger.TWO);
    }

    @Test
    void saturatedBroadcastQueueOnlyDropsHints() {
        var sender = mock(ExecutorService.class);
        doThrow(new java.util.concurrent.RejectedExecutionException("full")).when(sender).execute(any());
        var runtime = new ManualRoleVersionApplicationRuntime();
        var worker = mock(RoleVersionApplicationScheduler.class);
        try (var notifier = new RoleVersionApplicationWakeupNotifier(() -> worker, () -> null,
                sender, runtime.timer, runtime.metrics)) {
            notifier.start();
            assertDoesNotThrow(() -> notifier.onReady(new RoleVersionApplicationReadyEvent(BigInteger.ONE, BigInteger.TWO)));
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
        private Consumer<RoleVersionApplicationWakeupEvent> handler;
        private boolean closed;

        @Override
        public <E extends InvalidationEvent> void publish(E event) { }

        @Override
        @SuppressWarnings("unchecked")
        public <E extends InvalidationEvent> Subscription subscribe(Class<E> type, Consumer<E> consumer) {
            handler = (Consumer<RoleVersionApplicationWakeupEvent>) consumer;
            return () -> closed = true;
        }

        private void receive(RoleVersionApplicationWakeupEvent event) { handler.accept(event); }
    }
}
