package com.ingot.cloud.security.service.account.impl;

import java.time.LocalDateTime;

import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.cloud.security.mapper.AccountLockoutPolicyConfigMapper;
import com.ingot.cloud.security.model.domain.AccountLockoutPolicyConfig;
import com.ingot.cloud.security.service.policy.SecurityPolicyChangedSpringEvent;
import com.ingot.framework.commons.error.IllegalOperationException;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.core.context.InMessageSource;
import com.ingot.framework.core.utils.validation.DefaultAssertionChecker;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AccountLockoutPolicyAdminServiceImpl} 校验与失效广播。
 *
 * @author jy
 * @since 1.0.0
 */
class AccountLockoutPolicyAdminServiceImplTest {

    private final AccountLockoutPolicyConfigMapper policyMapper = mock(AccountLockoutPolicyConfigMapper.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final AccountLockoutPolicyAdminServiceImpl service = new AccountLockoutPolicyAdminServiceImpl(
            policyMapper, eventPublisher, new DefaultAssertionChecker(messageSource()));

    @Test
    void upsert_appPermanentLock_rejected() {
        AccountLockoutPolicyConfig policy = base(UserTypeEnum.APP);
        policy.setLockDurationMinutes(0);

        assertThrows(IllegalOperationException.class, () -> service.upsert(policy));
    }

    @Test
    void upsert_adminPermanentLock_allowedAndPublishes() {
        AccountLockoutPolicyConfig existing = base(UserTypeEnum.ADMIN);
        existing.setId(1L);
        existing.setCreatedAt(LocalDateTime.of(2026, 8, 1, 10, 0));
        when(policyMapper.selectOne(any())).thenReturn(existing);
        when(policyMapper.updateById(any(AccountLockoutPolicyConfig.class))).thenReturn(1);

        AccountLockoutPolicyConfig updated = base(UserTypeEnum.ADMIN);
        updated.setLockDurationMinutes(0);
        AccountLockoutPolicyConfig result = service.upsert(updated);

        assertEquals(0, result.getLockDurationMinutes());
        ArgumentCaptor<SecurityPolicyChangedSpringEvent> captor =
                ArgumentCaptor.forClass(SecurityPolicyChangedSpringEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(SecurityPolicyDomain.ACCOUNT_LOCKOUT, captor.getValue().getDomain());
    }

    private static AccountLockoutPolicyConfig base(UserTypeEnum userType) {
        AccountLockoutPolicyConfig policy = new AccountLockoutPolicyConfig();
        policy.setUserType(userType);
        policy.setEnabled(true);
        policy.setMaxAttempts(5);
        policy.setLockDurationMinutes(15);
        policy.setAttemptWindowMinutes(15);
        policy.setHintAfterAttempts(3);
        return policy;
    }

    private static InMessageSource messageSource() {
        InMessageSource messageSource = mock(InMessageSource.class);
        when(messageSource.getMessage(anyString(), anyString())).thenAnswer(inv -> inv.getArgument(0));
        return messageSource;
    }
}
