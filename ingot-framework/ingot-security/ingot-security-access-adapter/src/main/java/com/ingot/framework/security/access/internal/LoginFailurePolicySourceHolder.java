package com.ingot.framework.security.access.internal;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 登录失败策略生效来源持有者。
 *
 * @author jy
 * @since 1.0.0
 */
public class LoginFailurePolicySourceHolder {

    private final AtomicReference<LoginFailurePolicySource> current =
            new AtomicReference<>(LoginFailurePolicySource.REMOTE);
    private final AtomicLong lastKnownGoodCount = new AtomicLong();
    private final AtomicLong localFloorCount = new AtomicLong();
    private final AtomicReference<LocalDateTime> lastDegradeAt = new AtomicReference<>();

    public void mark(LoginFailurePolicySource source) {
        current.set(source);
        switch (source) {
            case LAST_KNOWN_GOOD -> {
                lastKnownGoodCount.incrementAndGet();
                lastDegradeAt.set(LocalDateTime.now());
            }
            case LOCAL_FLOOR -> {
                localFloorCount.incrementAndGet();
                lastDegradeAt.set(LocalDateTime.now());
            }
            default -> {
                // REMOTE
            }
        }
    }

    public LoginFailurePolicySource current() {
        return current.get();
    }

    public long lastKnownGoodCount() {
        return lastKnownGoodCount.get();
    }

    public long localFloorCount() {
        return localFloorCount.get();
    }

    public LocalDateTime lastDegradeAt() {
        return lastDegradeAt.get();
    }
}
