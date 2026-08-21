package com.ingot.framework.gateway.rule.client.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.ingot.cloud.security.api.model.vo.policy.EndpointGroupVO;
import com.ingot.cloud.security.api.model.vo.policy.SecurityPolicySnapshotVO;
import com.ingot.framework.gateway.rule.client.blacklist.config.BlacklistProperties;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpKeyType;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpListItem;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpListType;
import com.ingot.framework.gateway.rule.client.challenge.config.ChallengeProperties;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengeTrigger;
import com.ingot.framework.gateway.rule.client.model.EndpointPattern;
import com.ingot.framework.gateway.rule.client.ratelimit.config.RateLimitProperties;
import com.ingot.framework.gateway.rule.client.ratelimit.model.EndpointGroup;
import com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitDimension;
import com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitRule;
import com.ingot.framework.gateway.rule.client.violation.config.ViolationEscalationProperties;
import org.junit.jupiter.api.Test;

/**
 * {@link PolicySnapshotFloorAssembler} 按域片段组装单元测试。
 *
 * <p>入参 {@code null} 模拟域未启用（其 {@code *Properties} Bean 不装配）。</p>
 */
class PolicySnapshotFloorAssemblerTest {

    private static RateLimitProperties rateLimit(String groupCode) {
        RateLimitProperties p = new RateLimitProperties();
        p.setEnabled(true);
        p.getPolicy().setGroups(List.of(EndpointGroup.builder()
                .code(groupCode)
                .name("业务 API")
                .enabled(true)
                .patternList(List.of(EndpointPattern.of("/pms/**", "ANY")))
                .build()));
        p.getPolicy().setRules(List.of(RateLimitRule.builder()
                .code("pms-ip")
                .groupCode(groupCode)
                .dimension(RateLimitDimension.IP)
                .qps(200)
                .burst(300)
                .intervalSec(1)
                .controlBehavior("F")
                .enabled(true)
                .build()));
        return p;
    }

    private static BlacklistProperties blacklist() {
        BlacklistProperties p = new BlacklistProperties();
        p.setEnabled(true);
        p.getPolicy().setItems(List.of(IpListItem.builder()
                .listType(IpListType.BLACK)
                .keyType(IpKeyType.IP)
                .keyValue("1.2.3.4")
                .enabled(true)
                .build()));
        return p;
    }

    private static ChallengeProperties challenge(String groupCode) {
        ChallengeProperties p = new ChallengeProperties();
        p.setEnabled(true);
        p.getPolicy().setGroups(List.of(EndpointGroup.builder()
                .code(groupCode)
                .name("挑战分组")
                .enabled(true)
                .patternList(List.of(EndpointPattern.of("/auth/token", "POST")))
                .build()));
        p.getPolicy().setPolicies(List.of(ChallengePolicy.builder()
                .code("login-always")
                .groupCode(groupCode)
                .trigger(ChallengeTrigger.ALWAYS)
                .challengeType("SLIDER")
                .scope("login")
                .passTokenTtlSec(300)
                .passTokenRemaining(3)
                .enabled(true)
                .build()));
        return p;
    }

    private static ViolationEscalationProperties violation() {
        ViolationEscalationProperties p = new ViolationEscalationProperties();
        p.setEnabled(true);
        p.getPolicy().setWindowSec(60);
        p.getPolicy().setBlockThreshold(30);
        p.getPolicy().setTempBlockTtlSec(900);
        return p;
    }

    private static List<String> groupCodes(SecurityPolicySnapshotVO vo) {
        return vo.getGroups().stream().map(EndpointGroupVO::getCode).toList();
    }

    @Test
    void allDomainsEnabledContributeEveryFragment() {
        SecurityPolicySnapshotVO vo = PolicySnapshotFloorAssembler.fromLocal(
                rateLimit("api-business"), blacklist(), challenge("login-flow"), violation());

        assertEquals(List.of("api-business", "login-flow"), groupCodes(vo));
        assertEquals(1, vo.getRateLimitRules().size());
        assertEquals(1, vo.getIpList().size());
        assertEquals(1, vo.getChallengePolicies().size());
        assertEquals("ALWAYS", vo.getChallengePolicies().get(0).getTrigger());
        assertNotNull(vo.getViolationEscalation());
        assertEquals(60, vo.getViolationEscalation().getWindowSec());
    }

    @Test
    void allDomainsDisabledProduceEmptyFloor() {
        SecurityPolicySnapshotVO vo = PolicySnapshotFloorAssembler.fromLocal(null, null, null, null);

        assertTrue(vo.getGroups().isEmpty());
        assertTrue(vo.getRateLimitRules().isEmpty());
        assertTrue(vo.getIpList().isEmpty());
        assertTrue(vo.getChallengePolicies().isEmpty());
        assertNull(vo.getViolationEscalation());
    }

    @Test
    void disabledRateLimitDoesNotAffectOtherDomains() {
        SecurityPolicySnapshotVO vo = PolicySnapshotFloorAssembler.fromLocal(
                null, blacklist(), challenge("login-flow"), violation());

        assertTrue(vo.getRateLimitRules().isEmpty());
        assertEquals(List.of("login-flow"), groupCodes(vo));
        assertEquals(1, vo.getIpList().size());
        assertEquals(1, vo.getChallengePolicies().size());
        assertNotNull(vo.getViolationEscalation());
    }

    @Test
    void disabledViolationEscalationYieldsNullSoConsumerFallsBackToDefaults() {
        SecurityPolicySnapshotVO vo = PolicySnapshotFloorAssembler.fromLocal(
                rateLimit("api-business"), null, null, null);

        assertNull(vo.getViolationEscalation());
        assertEquals(1, vo.getRateLimitRules().size());
    }

    @Test
    void enabledRateLimitWithoutRulesGetsLoginBaseline() {
        RateLimitProperties empty = new RateLimitProperties();
        empty.setEnabled(true);

        SecurityPolicySnapshotVO vo = PolicySnapshotFloorAssembler.fromLocal(empty, null, null, null);

        assertEquals(List.of("login-auth-floor"), groupCodes(vo));
        // 地板必须护住实际登录入口（BFF），而不是已摘除的 /auth/token/**
        assertEquals("/bff/auth/login/**",
                vo.getGroups().get(0).getPatternList().get(0).getPath());
        assertEquals(1, vo.getRateLimitRules().size());
        assertEquals("login-ip-floor", vo.getRateLimitRules().get(0).getCode());
        assertEquals("IP", vo.getRateLimitRules().get(0).getDimension());
    }

    @Test
    void disabledRateLimitGetsNoBaseline() {
        SecurityPolicySnapshotVO vo = PolicySnapshotFloorAssembler.fromLocal(null, null, null, violation());

        assertTrue(vo.getGroups().isEmpty());
        assertTrue(vo.getRateLimitRules().isEmpty());
    }

    @Test
    void challengeGroupSharingRateLimitCodeIsDeduplicated() {
        SecurityPolicySnapshotVO vo = PolicySnapshotFloorAssembler.fromLocal(
                rateLimit("shared-group"), null, challenge("shared-group"), null);

        assertEquals(List.of("shared-group"), groupCodes(vo));
        // 同名 code 保留 ratelimit 的定义
        assertEquals("业务 API", vo.getGroups().get(0).getName());
    }
}
