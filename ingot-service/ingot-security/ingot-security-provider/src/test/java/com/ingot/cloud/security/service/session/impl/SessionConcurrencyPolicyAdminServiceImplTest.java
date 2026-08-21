package com.ingot.cloud.security.service.session.impl;

import java.time.LocalDateTime;

import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.cloud.security.mapper.SessionConcurrencyPolicyMapper;
import com.ingot.cloud.security.model.domain.SessionConcurrencyPolicy;
import com.ingot.cloud.security.service.policy.SecurityPolicyChangedSpringEvent;
import com.ingot.framework.commons.error.IllegalOperationException;
import com.ingot.framework.commons.model.security.SessionConcurrencyDimension;
import com.ingot.framework.commons.model.security.SessionOverflowStrategy;
import com.ingot.framework.commons.model.security.SessionPolicyScope;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.core.context.InMessageSource;
import com.ingot.framework.core.utils.validation.DefaultAssertionChecker;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SessionConcurrencyPolicyAdminServiceImpl} 策略校验、归一与失效广播。
 *
 * @author jy
 * @since 1.0.0
 */
class SessionConcurrencyPolicyAdminServiceImplTest {

    private final SessionConcurrencyPolicyMapper policyMapper = mock(SessionConcurrencyPolicyMapper.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private final SessionConcurrencyPolicyAdminServiceImpl service = new SessionConcurrencyPolicyAdminServiceImpl(
            policyMapper, eventPublisher, new DefaultAssertionChecker(messageSource()));

    @Test
    void create_fillsDefaultsAndPublishesInvalidation() {
        givenNoConflict();
        SessionConcurrencyPolicy policy = new SessionConcurrencyPolicy();
        policy.setScope(SessionPolicyScope.GLOBAL);
        policy.setMaxSessions(0);

        SessionConcurrencyPolicy created = service.create(policy);

        assertEquals(SessionConcurrencyDimension.USER_CLIENT, created.getDimension());
        assertEquals(SessionOverflowStrategy.KICK_OLDEST, created.getOverflow());
        assertEquals(Boolean.FALSE, created.getAdminForbidConcurrent());
        assertEquals(Boolean.TRUE, created.getEnabled());
        verify(policyMapper).insert(any(SessionConcurrencyPolicy.class));

        ArgumentCaptor<SecurityPolicyChangedSpringEvent> captor =
                ArgumentCaptor.forClass(SecurityPolicyChangedSpringEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(SecurityPolicyDomain.SESSION_CONCURRENCY, captor.getValue().getDomain());
    }

    @Test
    void create_globalScope_clearsScopeKeys() {
        givenNoConflict();
        SessionConcurrencyPolicy policy = new SessionConcurrencyPolicy();
        policy.setScope(SessionPolicyScope.GLOBAL);
        policy.setMaxSessions(1);
        policy.setClientId("web");
        policy.setUserType(UserTypeEnum.ADMIN.getValue());

        SessionConcurrencyPolicy created = service.create(policy);

        assertEquals("", created.getClientId());
        assertEquals("", created.getUserType());
    }

    @Test
    void create_clientScopeWithoutClientId_isRejected() {
        SessionConcurrencyPolicy policy = new SessionConcurrencyPolicy();
        policy.setScope(SessionPolicyScope.CLIENT);
        policy.setMaxSessions(1);

        assertThrows(IllegalOperationException.class, () -> service.create(policy));
        verify(policyMapper, never()).insert(any(SessionConcurrencyPolicy.class));
    }

    @Test
    void create_userTypeScopeWithUnknownUserType_isRejected() {
        SessionConcurrencyPolicy policy = new SessionConcurrencyPolicy();
        policy.setScope(SessionPolicyScope.USER_TYPE);
        policy.setUserType("NOT_A_USER_TYPE");
        policy.setMaxSessions(1);

        assertThrows(IllegalOperationException.class, () -> service.create(policy));
    }

    @Test
    void create_negativeMaxSessions_isRejected() {
        SessionConcurrencyPolicy policy = new SessionConcurrencyPolicy();
        policy.setScope(SessionPolicyScope.GLOBAL);
        policy.setMaxSessions(-1);

        assertThrows(IllegalOperationException.class, () -> service.create(policy));
    }

    @Test
    void create_duplicatedScope_isRejected() {
        when(policyMapper.selectCount(any())).thenReturn(1L);
        SessionConcurrencyPolicy policy = new SessionConcurrencyPolicy();
        policy.setScope(SessionPolicyScope.CLIENT);
        policy.setClientId("web");
        policy.setMaxSessions(1);

        assertThrows(IllegalOperationException.class, () -> service.create(policy));
        verify(policyMapper, never()).insert(any(SessionConcurrencyPolicy.class));
    }

    @Test
    void update_keepsCreatedAtAndPublishesInvalidation() {
        SessionConcurrencyPolicy existing = existing(SessionPolicyScope.CLIENT);
        when(policyMapper.selectById(existing.getId())).thenReturn(existing);
        givenNoConflict();

        SessionConcurrencyPolicy policy = new SessionConcurrencyPolicy();
        policy.setId(existing.getId());
        policy.setScope(SessionPolicyScope.CLIENT);
        policy.setClientId("web");
        policy.setMaxSessions(2);

        SessionConcurrencyPolicy updated = service.update(policy);

        assertEquals(existing.getCreatedAt(), updated.getCreatedAt());
        verify(policyMapper).updateById(any(SessionConcurrencyPolicy.class));
        verify(eventPublisher).publishEvent(any(SecurityPolicyChangedSpringEvent.class));
    }

    @Test
    void delete_globalPolicy_isRejected() {
        SessionConcurrencyPolicy existing = existing(SessionPolicyScope.GLOBAL);
        when(policyMapper.selectById(existing.getId())).thenReturn(existing);

        assertThrows(IllegalOperationException.class, () -> service.delete(existing.getId()));
        verify(policyMapper, never()).deleteById(any(Long.class));
        verify(eventPublisher, never()).publishEvent(any(SecurityPolicyChangedSpringEvent.class));
    }

    @Test
    void delete_clientPolicy_publishesInvalidation() {
        SessionConcurrencyPolicy existing = existing(SessionPolicyScope.CLIENT);
        when(policyMapper.selectById(existing.getId())).thenReturn(existing);

        service.delete(existing.getId());

        verify(policyMapper).deleteById(existing.getId());
        verify(eventPublisher).publishEvent(any(SecurityPolicyChangedSpringEvent.class));
    }

    @Test
    void getById_missingPolicy_isRejected() {
        when(policyMapper.selectById(404L)).thenReturn(null);

        IllegalOperationException e = assertThrows(IllegalOperationException.class, () -> service.getById(404L));
        assertTrue(e.getMessage().contains("SessionPolicyNotFound"));
    }

    private void givenNoConflict() {
        when(policyMapper.selectCount(any())).thenReturn(0L);
    }

    private static SessionConcurrencyPolicy existing(SessionPolicyScope scope) {
        SessionConcurrencyPolicy policy = new SessionConcurrencyPolicy();
        policy.setId(1L);
        policy.setScope(scope);
        policy.setClientId(scope == SessionPolicyScope.CLIENT ? "web" : "");
        policy.setUserType("");
        policy.setMaxSessions(0);
        policy.setDimension(SessionConcurrencyDimension.USER_CLIENT);
        policy.setOverflow(SessionOverflowStrategy.KICK_OLDEST);
        policy.setAdminForbidConcurrent(Boolean.FALSE);
        policy.setEnabled(Boolean.TRUE);
        policy.setCreatedAt(LocalDateTime.of(2026, 8, 1, 10, 0));
        return policy;
    }

    /**
     * 无国际化资源时回落 message code，断言只关心是否拒绝而非文案内容。
     */
    private static InMessageSource messageSource() {
        InMessageSource messageSource = mock(InMessageSource.class);
        when(messageSource.getMessage(anyString(), anyString())).thenAnswer(inv -> inv.getArgument(0));
        return messageSource;
    }
}
