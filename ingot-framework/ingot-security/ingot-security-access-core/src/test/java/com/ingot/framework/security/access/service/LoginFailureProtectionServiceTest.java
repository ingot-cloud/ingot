package com.ingot.framework.security.access.service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.ingot.cloud.security.api.model.dto.SecurityEventReportDTO;
import com.ingot.cloud.security.api.model.enums.LoginFailureDimension;
import com.ingot.framework.security.access.model.LoginFailureContext;
import com.ingot.framework.security.access.model.LoginFailurePolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link LoginFailureProtectionService} 超阈值边沿去重单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class LoginFailureProtectionServiceTest {

    @Test
    void recordFailure_overThreshold_reportsOnlyOnce() {
        AtomicInteger reports = new AtomicInteger();
        AtomicInteger tryBlockCalls = new AtomicInteger();
        LoginFailureCounter counter = new LoginFailureCounter() {
            private long value;

            @Override
            public long increment(String key, Duration window) {
                return ++value;
            }

            @Override
            public void reset(String key) {
                value = 0;
            }
        };
        TempBlockWriter writer = new TempBlockWriter() {
            private boolean blocked;

            @Override
            public void block(String keyType, String keyValue, Duration ttl) {
                blocked = true;
            }

            @Override
            public boolean tryBlockFirst(String keyType, String keyValue, Duration ttl) {
                tryBlockCalls.incrementAndGet();
                if (blocked) {
                    return false;
                }
                blocked = true;
                return true;
            }

            @Override
            public boolean isBlocked(String keyType, String keyValue) {
                return blocked;
            }

            @Override
            public void refreshTtl(String keyType, String keyValue, Duration ttl) {
                // no-op
            }
        };
        Consumer<SecurityEventReportDTO> reporter = dto -> reports.incrementAndGet();
        LoginFailurePolicyLoader loader = () -> List.of(new LoginFailurePolicy(
                LoginFailureDimension.IP, true, 2, 10, 60, "IP"));

        LoginFailureProtectionService service =
                new LoginFailureProtectionService(loader, counter, writer, reporter);
        LoginFailureContext ctx = new LoginFailureContext("1.2.3.4", null, null, "admin", "0");

        service.recordFailure(ctx); // count=1 < 2
        service.recordFailure(ctx); // count=2 first block + report
        service.recordFailure(ctx); // count=3 already blocked, skip report
        service.recordFailure(ctx); // count=4 skip report

        assertEquals(1, reports.get());
        assertEquals(3, tryBlockCalls.get());
    }
}
