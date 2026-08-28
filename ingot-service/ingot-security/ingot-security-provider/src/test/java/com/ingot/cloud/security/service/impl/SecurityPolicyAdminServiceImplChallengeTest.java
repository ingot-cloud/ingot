package com.ingot.cloud.security.service.impl;

import java.util.List;

import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.cloud.security.api.model.enums.ChallengeCaptchaType;
import com.ingot.cloud.security.api.model.vo.policy.ChallengePolicyVO;
import com.ingot.cloud.security.api.model.vo.policy.EndpointPatternVO;
import com.ingot.cloud.security.mapper.GatewayBlacklistEventMapper;
import com.ingot.cloud.security.mapper.GatewayEndpointGroupMapper;
import com.ingot.cloud.security.mapper.GatewayIpListMapper;
import com.ingot.cloud.security.mapper.GatewayRateLimitRuleMapper;
import com.ingot.cloud.security.mapper.GatewayViolationEscalationMapper;
import com.ingot.cloud.security.mapper.SecurityChallengePolicyMapper;
import com.ingot.cloud.security.model.domain.SecurityChallengePolicy;
import com.ingot.cloud.security.service.policy.SecurityPolicyChangedSpringEvent;
import com.ingot.framework.commons.error.IllegalOperationException;
import com.ingot.framework.core.context.InMessageSource;
import com.ingot.framework.core.utils.validation.DefaultAssertionChecker;
import com.ingot.framework.eventbus.InvalidationBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SecurityPolicyAdminServiceImpl} 挑战策略写入校验。
 *
 * @author jy
 * @since 1.0.0
 */
class SecurityPolicyAdminServiceImplChallengeTest {

    private final SecurityChallengePolicyMapper challengeMapper = mock(SecurityChallengePolicyMapper.class);
    private final GatewayEndpointGroupMapper groupMapper = mock(GatewayEndpointGroupMapper.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private SecurityPolicyAdminServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<InvalidationBus> busProvider = mock(ObjectProvider.class);
        when(busProvider.getIfAvailable()).thenReturn(null);
        service = new SecurityPolicyAdminServiceImpl(
                groupMapper,
                mock(GatewayRateLimitRuleMapper.class),
                mock(GatewayIpListMapper.class),
                mock(GatewayBlacklistEventMapper.class),
                challengeMapper,
                mock(GatewayViolationEscalationMapper.class),
                eventPublisher,
                new DefaultAssertionChecker(messageSource()),
                busProvider);
        when(challengeMapper.insert(any(SecurityChallengePolicy.class))).thenReturn(1);
    }

    @Test
    void save_validLoginPolicy_publishesChallengeDomain() {
        SecurityChallengePolicy policy = validLogin();
        service.saveChallengePolicy(policy);

        ArgumentCaptor<SecurityPolicyChangedSpringEvent> captor =
                ArgumentCaptor.forClass(SecurityPolicyChangedSpringEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(SecurityPolicyDomain.CHALLENGE_POLICY, captor.getValue().getDomain());
    }

    @Test
    void save_smsType_rejected() {
        SecurityChallengePolicy policy = validLogin();
        policy.setChallengeType("SMS");
        assertThrows(IllegalOperationException.class, () -> service.saveChallengePolicy(policy));
    }

    @Test
    void save_blankScope_rejected() {
        SecurityChallengePolicy policy = validLogin();
        policy.setScope(" ");
        assertThrows(IllegalOperationException.class, () -> service.saveChallengePolicy(policy));
    }

    @Test
    void save_vcPath_rejected() {
        SecurityChallengePolicy policy = validLogin();
        policy.setGroupCode(null);
        policy.setPatternList(List.of(new EndpointPatternVO("/vc/**", "POST")));
        assertThrows(IllegalOperationException.class, () -> service.saveChallengePolicy(policy));
    }

    @Test
    void save_noPath_rejected() {
        SecurityChallengePolicy policy = validLogin();
        policy.setGroupCode(null);
        policy.setPatternList(null);
        assertThrows(IllegalOperationException.class, () -> service.saveChallengePolicy(policy));
    }

    private static SecurityChallengePolicy validLogin() {
        SecurityChallengePolicy policy = new SecurityChallengePolicy();
        policy.setCode("login-always");
        policy.setGroupCode("login-auth");
        policy.setTrigger(ChallengePolicyVO.TRIGGER_ALWAYS);
        policy.setChallengeType(ChallengeCaptchaType.VALUE_SLIDER);
        policy.setScope("login");
        policy.setPassTokenTtlSec(300);
        policy.setPassTokenRemaining(3);
        policy.setEnabled(true);
        policy.setPriority(0);
        return policy;
    }

    private static InMessageSource messageSource() {
        InMessageSource messageSource = mock(InMessageSource.class);
        when(messageSource.getMessage(anyString(), anyString())).thenAnswer(inv -> inv.getArgument(0));
        return messageSource;
    }
}
