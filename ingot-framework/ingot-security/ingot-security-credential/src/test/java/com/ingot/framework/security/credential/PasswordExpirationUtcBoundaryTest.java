package com.ingot.framework.security.credential;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;

import com.ingot.framework.security.credential.model.CredentialErrorCode;
import com.ingot.framework.security.credential.model.PolicyCheckContext;
import com.ingot.framework.security.credential.policy.PasswordExpirationPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <p>密码期限按 UTC 在截止瞬间过期，并保留宽限和无限期限语义。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class PasswordExpirationUtcBoundaryTest {
    @Test
    void beforeAtAfterAndUnlimitedDoNotDependOnJvmZone() {
        TimeZone original = TimeZone.getDefault();
        LocalDateTime cutoff = LocalDateTime.of(2026, 10, 8, 1, 0);
        LocalDateTime changedAt = cutoff.minusDays(90);
        LocalDateTime before = cutoff.minusNanos(1);
        LocalDateTime after = cutoff.plusNanos(1);
        try {
            for (String zone : new String[]{"UTC", "Asia/Shanghai", "America/New_York"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                var policy = new PasswordExpirationPolicy();
                var context = PolicyCheckContext.builder().lastPasswordChangedAt(changedAt).graceLoginRemaining(0).build();
                try (var clock = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
                    clock.when(() -> LocalDateTime.now(ZoneOffset.UTC)).thenReturn(before);
                    assertTrue(policy.check(context).isPassed());
                    clock.when(() -> LocalDateTime.now(ZoneOffset.UTC)).thenReturn(cutoff);
                    assertEquals(CredentialErrorCode.EXPIRED, policy.check(context).getFailureCode());
                    clock.when(() -> LocalDateTime.now(ZoneOffset.UTC)).thenReturn(after);
                    assertEquals(CredentialErrorCode.EXPIRED, policy.check(context).getFailureCode());
                    context.setGraceLoginRemaining(1);
                    assertTrue(policy.check(context).isPassed());
                    assertEquals(CredentialErrorCode.EXPIRED_WITH_GRACE, policy.check(context).getWarningCode());
                    policy.setMaxDays(0);
                    assertTrue(policy.check(context).isPassed());
                }
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
