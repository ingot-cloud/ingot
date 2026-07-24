package com.ingot.framework.security.credential.internal;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.ingot.framework.security.credential.model.CredentialPolicySource;

/**
 * 凭证策略生效来源与降级计数的进程内持有者。
 *
 * <p>由 {@link ResilientCredentialPolicyConfigService} 在每次返回数据时更新，供日志、指标与
 * actuator 端点查询当前是否处于降级态及累计降级次数。线程安全。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public class CredentialPolicySourceHolder {

    private final AtomicReference<CredentialPolicySource> current =
            new AtomicReference<>(CredentialPolicySource.REMOTE);
    private final AtomicLong lastKnownGoodCount = new AtomicLong();
    private final AtomicLong localFloorCount = new AtomicLong();
    private final AtomicReference<LocalDateTime> lastDegradeAt = new AtomicReference<>();

    /**
     * 标记当前生效来源；降级来源会累加对应计数并记录时间。
     */
    public void mark(CredentialPolicySource source) {
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
                // REMOTE：正常态，不计数
            }
        }
    }

    public CredentialPolicySource current() {
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
