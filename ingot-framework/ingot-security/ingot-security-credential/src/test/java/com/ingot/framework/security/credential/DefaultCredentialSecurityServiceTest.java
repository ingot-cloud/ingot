package com.ingot.framework.security.credential;

import java.util.List;

import com.ingot.framework.security.credential.config.CredentialSecurityProperties;
import com.ingot.framework.security.credential.policy.PasswordExpirationPolicy;
import com.ingot.framework.security.credential.policy.PasswordPolicy;
import com.ingot.framework.security.credential.policy.PasswordStrengthPolicy;
import com.ingot.framework.security.credential.service.CredentialPolicyLoader;
import com.ingot.framework.security.credential.service.PasswordExpirationService;
import com.ingot.framework.security.credential.service.PasswordHistoryService;
import com.ingot.framework.security.credential.service.impl.DefaultCredentialSecurityService;
import com.ingot.framework.security.credential.validator.PasswordValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DefaultCredentialSecurityService} 宽限扣减与强制改密标记单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class DefaultCredentialSecurityServiceTest {

    private final PasswordValidator passwordValidator = mock(PasswordValidator.class);
    private final PasswordHistoryService passwordHistoryService = mock(PasswordHistoryService.class);
    private final PasswordExpirationService passwordExpirationService = mock(PasswordExpirationService.class);
    private final CredentialPolicyLoader policyLoader = mock(CredentialPolicyLoader.class);
    private final CredentialSecurityProperties properties = new CredentialSecurityProperties();

    private DefaultCredentialSecurityService newService(List<PasswordPolicy> policies) {
        when(policyLoader.loadPolicies()).thenReturn(policies);
        return new DefaultCredentialSecurityService(
                passwordValidator, passwordHistoryService, passwordExpirationService, properties, policyLoader);
    }

    @Test
    void consumeGrace_userIdNull_returnsMinusOneAndNoDecrement() {
        DefaultCredentialSecurityService service = newService(List.of(new PasswordExpirationPolicy()));

        int result = service.consumeGraceLoginOnSuccess(null);

        assertEquals(-1, result);
        verify(passwordExpirationService, never()).decrementGraceLogin(anyLong());
    }

    @Test
    void consumeGrace_noExpirationPolicy_returnsMinusOne() {
        DefaultCredentialSecurityService service = newService(List.of(new PasswordStrengthPolicy()));

        int result = service.consumeGraceLoginOnSuccess(1L);

        assertEquals(-1, result);
        verify(passwordExpirationService, never()).isExpired(anyLong());
        verify(passwordExpirationService, never()).decrementGraceLogin(anyLong());
    }

    @Test
    void consumeGrace_policyEnabledButNotExpired_returnsMinusOne() {
        DefaultCredentialSecurityService service = newService(List.of(new PasswordExpirationPolicy()));
        when(passwordExpirationService.isExpired(1L)).thenReturn(false);

        int result = service.consumeGraceLoginOnSuccess(1L);

        assertEquals(-1, result);
        verify(passwordExpirationService, never()).decrementGraceLogin(anyLong());
    }

    @Test
    void consumeGrace_policyEnabledAndExpired_decrementsAndReturnsRemaining() {
        DefaultCredentialSecurityService service = newService(List.of(new PasswordExpirationPolicy()));
        when(passwordExpirationService.isExpired(1L)).thenReturn(true);
        when(passwordExpirationService.decrementGraceLogin(1L)).thenReturn(2);

        int result = service.consumeGraceLoginOnSuccess(1L);

        assertEquals(2, result);
        verify(passwordExpirationService).decrementGraceLogin(1L);
    }

    @Test
    void markForceChange_delegatesToExpirationService() {
        DefaultCredentialSecurityService service = newService(List.of());

        service.markForceChange(9L, true);

        verify(passwordExpirationService).updateForceChange(9L, true);
    }
}
