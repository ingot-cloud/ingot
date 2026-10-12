package com.ingot.framework.security.account.web.support;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import com.ingot.framework.commons.model.security.CredentialExpirationReason;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.model.LockState;
import com.ingot.framework.security.account.domain.model.LockoutPolicy;
import com.ingot.framework.security.account.domain.model.UserAccount;
import com.ingot.framework.security.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.security.account.domain.port.outbound.UserAccountPort;
import com.ingot.framework.security.account.domain.service.AccountLockoutPolicyLoader;
import com.ingot.framework.security.core.userdetails.InUserMetaKeys;
import com.ingot.framework.security.credential.model.CredentialErrorCode;
import com.ingot.framework.security.credential.model.InitialPasswordConfig;
import com.ingot.framework.security.credential.model.PasswordCheckResult;
import com.ingot.framework.security.credential.model.PolicyCheckContext;
import com.ingot.framework.security.credential.policy.PasswordExpirationPolicy;
import com.ingot.framework.security.credential.service.CredentialPolicyLoader;
import com.ingot.framework.security.credential.service.CredentialSecurityService;
import com.ingot.framework.security.credential.service.InitialPasswordService;
import com.ingot.framework.security.credential.service.impl.DefaultInitialPasswordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>验证初始／重置有效期、普通过期和宽限的提示分类，同时保留既有准入和锁定上下文。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class AuthContextSupportExpirationTest {
    private static final long USER_ID = 1L;
    private static final LocalDateTime ISSUED_AT = LocalDateTime.of(2026, 10, 8, 1, 0);
    private static final LocalDateTime CUTOFF = ISSUED_AT.plusHours(72);

    private final CredentialSecurityService credentials = mock(CredentialSecurityService.class);
    private final UserAccountPort accounts = mock(UserAccountPort.class);
    private final LockStatePort locks = mock(LockStatePort.class);
    private final CredentialPolicyLoader policyLoader = mock(CredentialPolicyLoader.class);
    private final InitialPasswordService initialPasswords = new DefaultInitialPasswordService(policyLoader);
    private final AccountLockoutPolicyLoader lockPolicy =
            type -> new LockoutPolicy(type, true, 5, 30, 15, 3);
    private final AuthContextSupport support = new AuthContextSupport(
            credentials, locks, lockPolicy, initialPasswords, accounts);

    AuthContextSupportExpirationTest() {
        when(credentials.validate(any())).thenReturn(PasswordCheckResult.pass());
        when(policyLoader.getInitialPasswordConfig()).thenReturn(InitialPasswordConfig.defaults());
        when(locks.findByUser(any(), any())).thenReturn(Optional.empty());
    }

    @ParameterizedTest
    @EnumSource(UserTypeEnum.class)
    void initialOrResetPasswordExpiresAtUtcCutoff(UserTypeEnum type) {
        when(accounts.findById(USER_ID, type)).thenReturn(Optional.of(account(true, ISSUED_AT)));
        try (var clock = mockStatic(LocalDateTime.class)) {
            clock.when(() -> LocalDateTime.now(ZoneOffset.UTC)).thenReturn(CUTOFF.minusNanos(1));
            UserDetailsResponse before = response();
            support.fill(before, USER_ID, type);
            assertTrue(before.getCredentialsNonExpired());
            assertNull(reason(before));

            clock.when(() -> LocalDateTime.now(ZoneOffset.UTC)).thenReturn(CUTOFF);
            UserDetailsResponse expired = response();
            support.fill(expired, USER_ID, type);
            assertFalse(expired.getCredentialsNonExpired());
            assertEquals(CredentialExpirationReason.INITIAL_PASSWORD_EXPIRED.getValue(), reason(expired));
        }
    }

    @Test
    void loginDoesNotExtendExpiryButResetTimestampDoes() {
        UserAccount account = account(true, ISSUED_AT);
        account.setLastLoginAt(CUTOFF);
        when(accounts.findById(USER_ID, UserTypeEnum.ADMIN)).thenReturn(Optional.of(account));
        try (var clock = mockStatic(LocalDateTime.class)) {
            clock.when(() -> LocalDateTime.now(ZoneOffset.UTC)).thenReturn(CUTOFF);
            UserDetailsResponse expired = response();
            support.fill(expired, USER_ID, UserTypeEnum.ADMIN);
            assertFalse(expired.getCredentialsNonExpired());
            account.setPasswordChangedAt(CUTOFF);
            UserDetailsResponse reset = response();
            support.fill(reset, USER_ID, UserTypeEnum.ADMIN);
            assertTrue(reset.getCredentialsNonExpired());
            assertNull(reason(reset));
        }
    }

    @Test
    void changedPasswordIsNotSubjectToInitialExpiry() {
        when(accounts.findById(USER_ID, UserTypeEnum.ADMIN)).thenReturn(Optional.of(account(false, ISSUED_AT)));
        UserDetailsResponse result = response();
        support.fill(result, USER_ID, UserTypeEnum.ADMIN);
        assertTrue(result.getCredentialsNonExpired());
        assertNull(reason(result));
    }

    @Test
    void initialUnlimitedPolicyPreservesAdmission() {
        InitialPasswordConfig defaults = InitialPasswordConfig.defaults();
        when(policyLoader.getInitialPasswordConfig()).thenReturn(new InitialPasswordConfig(
                defaults.generation(), defaults.length(), defaults.fixedPassword(), 0, true, true));
        when(accounts.findById(USER_ID, UserTypeEnum.ADMIN)).thenReturn(Optional.of(account(true, ISSUED_AT)));
        UserDetailsResponse result = response();
        support.fill(result, USER_ID, UserTypeEnum.ADMIN);
        assertTrue(result.getCredentialsNonExpired());
        assertNull(reason(result));
    }

    @Test
    void ordinaryExpirationUsesErrorCodeAndGraceRemainsAllowed() {
        PasswordExpirationPolicy expiration = new PasswordExpirationPolicy();
        expiration.setEnabled(true);
        expiration.setMaxDays(1);
        LocalDateTime lastChangedAt = LocalDateTime.now(ZoneOffset.UTC).minusDays(2);
        PasswordCheckResult grace = expiration.check(PolicyCheckContext.builder()
                .lastPasswordChangedAt(lastChangedAt).graceLoginRemaining(1).build());
        when(credentials.validate(any())).thenReturn(grace);
        UserDetailsResponse allowed = response();
        support.fill(allowed, USER_ID, UserTypeEnum.ADMIN);
        assertTrue(allowed.getCredentialsNonExpired());
        assertNull(reason(allowed));

        PasswordCheckResult hardExpired = expiration.check(PolicyCheckContext.builder()
                .lastPasswordChangedAt(lastChangedAt).graceLoginRemaining(0).build());
        hardExpired.getFailureReasons().clear();
        hardExpired.getFailureReasons().add("任意语言的错误说明");
        when(credentials.validate(any())).thenReturn(hardExpired);
        UserDetailsResponse denied = response();
        support.fill(denied, USER_ID, UserTypeEnum.ADMIN);
        assertFalse(denied.getCredentialsNonExpired());
        assertEquals(CredentialExpirationReason.PASSWORD_EXPIRED.getValue(), reason(denied));
    }

    @Test
    void initialExpiryTakesPriorityAndMetaIsMerged() {
        when(credentials.validate(any())).thenReturn(PasswordCheckResult.fail("ordinary", CredentialErrorCode.EXPIRED));
        when(accounts.findById(USER_ID, UserTypeEnum.ADMIN)).thenReturn(Optional.of(account(true, ISSUED_AT)));
        when(locks.findByUser(USER_ID, UserTypeEnum.ADMIN)).thenReturn(Optional.of(
                LockState.builder().failedLoginCount(2).build()));
        Map<String, Object> existing = Map.of("existing", "preserve");
        UserDetailsResponse result = response();
        result.setMeta(existing);
        try (var clock = mockStatic(LocalDateTime.class)) {
            clock.when(() -> LocalDateTime.now(ZoneOffset.UTC)).thenReturn(CUTOFF);
            support.fill(result, USER_ID, UserTypeEnum.ADMIN);
        }
        assertFalse(result.getCredentialsNonExpired());
        assertEquals(CredentialExpirationReason.INITIAL_PASSWORD_EXPIRED.getValue(), reason(result));
        assertEquals("preserve", result.getMeta().get("existing"));
        assertEquals(2, result.getMeta().get(InUserMetaKeys.FAILED_LOGIN_COUNT));
        assertEquals(5, result.getMeta().get(InUserMetaKeys.MAX_FAILED_ATTEMPTS));
        assertEquals(Map.of("existing", "preserve"), existing);
    }

    @Test
    void unknownFailureDoesNotPretendToBeOrdinaryExpiration() {
        when(credentials.validate(any())).thenReturn(PasswordCheckResult.fail("other", CredentialErrorCode.STRENGTH));
        UserDetailsResponse result = response();
        support.fill(result, USER_ID, UserTypeEnum.ADMIN);
        assertFalse(result.getCredentialsNonExpired());
        assertEquals(CredentialExpirationReason.UNKNOWN.getValue(), reason(result));
    }

    @Test
    void missingOptionalDependenciesDoNotClearExistingDenial() {
        UserDetailsResponse result = response();
        result.setCredentialsNonExpired(false);
        new AuthContextSupport(null, null, null, null, null).fill(result, USER_ID, UserTypeEnum.ADMIN);
        assertFalse(result.getCredentialsNonExpired());
        assertEquals(CredentialExpirationReason.UNKNOWN.getValue(), reason(result));
    }

    @Test
    void validCredentialsRemoveStaleReasonWithoutCreatingDenial() {
        UserDetailsResponse result = response();
        result.setMeta(Map.of(InUserMetaKeys.CREDENTIAL_EXPIRATION_REASON,
                CredentialExpirationReason.INITIAL_PASSWORD_EXPIRED.getValue()));
        new AuthContextSupport(null, null, null, null, null).fill(result, USER_ID, UserTypeEnum.ADMIN);
        assertTrue(result.getCredentialsNonExpired());
        assertTrue(result.getMeta().isEmpty());
    }

    @Test
    void initialLookupFailureRetainsOrdinaryDenial() {
        when(credentials.validate(any())).thenReturn(PasswordCheckResult.fail("ordinary", CredentialErrorCode.EXPIRED));
        when(accounts.findById(USER_ID, UserTypeEnum.ADMIN)).thenThrow(new IllegalStateException("unavailable"));
        UserDetailsResponse result = response();
        support.fill(result, USER_ID, UserTypeEnum.ADMIN);
        assertFalse(result.getCredentialsNonExpired());
        assertEquals(CredentialExpirationReason.PASSWORD_EXPIRED.getValue(), reason(result));
    }

    @Test
    void credentialLookupFailurePreservesExistingExceptionAdmission() {
        when(credentials.validate(any())).thenThrow(new IllegalStateException("unavailable"));
        UserDetailsResponse result = response();
        support.fill(result, USER_ID, UserTypeEnum.ADMIN);
        assertTrue(result.getCredentialsNonExpired());
        assertNull(reason(result));
    }

    @Test
    void lockedAccountSkipsExpiryAndKeepsDeadline() {
        UserDetailsResponse result = response();
        result.setLocked(true);
        when(locks.findByUser(USER_ID, UserTypeEnum.ADMIN)).thenReturn(Optional.of(
                LockState.builder().locked(true).lockedUntil(CUTOFF).build()));
        support.fill(result, USER_ID, UserTypeEnum.ADMIN);
        verify(credentials, never()).validate(any());
        verify(accounts, never()).findById(any(), any());
        assertEquals(CUTOFF.toString(), result.getMeta().get(InUserMetaKeys.LOCKED_UNTIL));
    }

    private UserAccount account(boolean mustChange, LocalDateTime changedAt) {
        return UserAccount.builder().id(USER_ID).mustChangePwd(mustChange).passwordChangedAt(changedAt).build();
    }

    private UserDetailsResponse response() {
        UserDetailsResponse result = new UserDetailsResponse();
        result.setEnabled(true);
        result.setLocked(false);
        result.setCredentialsNonExpired(true);
        return result;
    }

    private Object reason(UserDetailsResponse result) {
        return result.getMeta() == null ? null : result.getMeta().get(InUserMetaKeys.CREDENTIAL_EXPIRATION_REASON);
    }
}
