package com.ingot.framework.security.recording.store.mysql.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>{@link SecurityEventWriteSemaphore} 资源隔离单测（Phase 04 V1）。</p>
 */
class SecurityEventWriteSemaphoreTest {

    @Test
    @DisplayName("maxConcurrentWrites=1 时第二写入者需等待释放")
    void singlePermitBlocksSecondWriter() throws Exception {
        SecurityEventWriteSemaphore semaphore = new SecurityEventWriteSemaphore(1, 2);
        CountDownLatch firstAcquired = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger concurrent = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            pool.submit(() -> {
                semaphore.acquire();
                concurrent.incrementAndGet();
                firstAcquired.countDown();
                try {
                    releaseFirst.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    concurrent.decrementAndGet();
                    semaphore.release();
                }
            });
            assertThat(firstAcquired.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(semaphore.availablePermits()).isZero();

            CountDownLatch secondDone = new CountDownLatch(1);
            pool.submit(() -> {
                semaphore.acquire();
                try {
                    assertThat(concurrent.get()).isZero();
                } finally {
                    semaphore.release();
                    secondDone.countDown();
                }
            });

            Thread.sleep(100);
            assertThat(secondDone.getCount()).isEqualTo(1);
            releaseFirst.countDown();
            assertThat(secondDone.await(2, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("availablePermits 反映剩余许可")
    void availablePermitsReflectsCapacity() {
        SecurityEventWriteSemaphore semaphore = new SecurityEventWriteSemaphore(2, 1);
        assertThat(semaphore.availablePermits()).isEqualTo(2);
        semaphore.acquire();
        assertThat(semaphore.availablePermits()).isEqualTo(1);
        semaphore.release();
        assertThat(semaphore.availablePermits()).isEqualTo(2);
    }
}
