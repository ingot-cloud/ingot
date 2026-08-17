package com.ingot.framework.security.recording.store.mysql.internal;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * <p>事件 writer 数据库连接舱壁，限制并发写入许可。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class SecurityEventWriteSemaphore {

    private final Semaphore semaphore;
    private final int transactionTimeoutSeconds;

    public SecurityEventWriteSemaphore(int maxConcurrentWrites, int transactionTimeoutSeconds) {
        int permits = Math.max(maxConcurrentWrites, 1);
        this.semaphore = new Semaphore(permits, true);
        this.transactionTimeoutSeconds = Math.max(transactionTimeoutSeconds, 1);
    }

    public void acquire() {
        try {
            if (!semaphore.tryAcquire(transactionTimeoutSeconds, TimeUnit.SECONDS)) {
                throw new IllegalStateException("security event writer semaphore timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("security event writer interrupted", e);
        }
    }

    public void release() {
        semaphore.release();
    }

    public int availablePermits() {
        return semaphore.availablePermits();
    }
}
