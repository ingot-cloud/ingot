package com.ingot.framework.security.recording.runtime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <p>{@link UnconfiguredDurableRecordQueue} 空闲等待与中断语义单测。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class UnconfiguredDurableRecordQueueTest {

    private static final Duration OBSERVABLE_WAIT = Duration.ofMillis(250);
    private static final Duration LONG_CLAIM_WAIT = Duration.ofSeconds(5);
    private static final long EARLY_RETURN_CHECK_MS = 50;
    private static final long ASYNC_RESULT_TIMEOUT_SECONDS = 2;
    private static final long THREAD_JOIN_TIMEOUT_MS = 1000;

    @Test
    @DisplayName("正等待时间不立即返回")
    void positiveWaitDoesNotReturnImmediately() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            UnconfiguredDurableRecordQueue queue = new UnconfiguredDurableRecordQueue();
            CountDownLatch started = new CountDownLatch(1);
            Future<?> future = executor.submit(() -> {
                started.countDown();
                return queue.claim(1, OBSERVABLE_WAIT);
            });

            assertThat(started.await(ASYNC_RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> future.get(EARLY_RETURN_CHECK_MS, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            assertThat(future.get(ASYNC_RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isEqualTo(java.util.List.of());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("中断停放后及时返回并保留中断标记")
    void interruptReturnsAndPreservesFlag() throws Exception {
        UnconfiguredDurableRecordQueue queue = new UnconfiguredDurableRecordQueue();
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        Thread claimant = new Thread(() -> {
            started.countDown();
            queue.claim(1, LONG_CLAIM_WAIT);
            interrupted.set(Thread.currentThread().isInterrupted());
        }, "unconfigured-spool-claim-test");
        claimant.start();
        try {
            assertThat(started.await(ASYNC_RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
            claimant.interrupt();
            claimant.join(THREAD_JOIN_TIMEOUT_MS);

            assertThat(claimant.isAlive()).isFalse();
            assertThat(interrupted).isTrue();
        } finally {
            claimant.interrupt();
            claimant.join(THREAD_JOIN_TIMEOUT_MS);
        }
    }
}
