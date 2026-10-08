package com.ingot.framework.security.account.domain.model;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <p>账号锁定以 UTC 截止瞬间为失效边界，永久和未锁定语义保持不变。</p>
 * @author jy
 * @since 1.0.0
 */
class LockStateUtcBoundaryTest {
    @Test
    void cutoffIsExclusiveAndIndependentOfJvmZone() {
        TimeZone original = TimeZone.getDefault();
        LocalDateTime cutoff = LocalDateTime.of(2026, 10, 8, 1, 0);
        try {
            for (String zone : new String[]{"UTC", "Asia/Shanghai", "America/New_York"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                LockState state = LockState.builder().locked(true).lockedUntil(cutoff).build();
                LocalDateTime before = cutoff.minusNanos(1);
                LocalDateTime after = cutoff.plusNanos(1);
                try (var dates = mockStatic(LocalDateTime.class)) {
                    dates.when(() -> LocalDateTime.now(ZoneOffset.UTC)).thenReturn(before);
                    assertFalse(state.isLockExpired());
                    dates.when(() -> LocalDateTime.now(ZoneOffset.UTC)).thenReturn(cutoff);
                    assertTrue(state.isLockExpired());
                    dates.when(() -> LocalDateTime.now(ZoneOffset.UTC)).thenReturn(after);
                    assertTrue(state.isLockExpired());
                    state.setLockedUntil(null);
                    assertFalse(state.isLockExpired());
                    assertTrue(state.isPermanentLock());
                    state.setLocked(false);
                    assertFalse(state.isLockExpired());
                }
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
