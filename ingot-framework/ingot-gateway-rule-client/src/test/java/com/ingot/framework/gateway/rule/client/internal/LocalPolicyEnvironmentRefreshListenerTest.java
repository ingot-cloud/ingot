package com.ingot.framework.gateway.rule.client.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;

/**
 * {@link LocalPolicyEnvironmentRefreshListener} 行为验证。
 */
class LocalPolicyEnvironmentRefreshListenerTest {

    @Test
    void invokesRefresherWhenKeyMatchesPrefix() {
        LocalPolicyEnvironmentRefreshListener listener = new LocalPolicyEnvironmentRefreshListener();
        AtomicInteger count = new AtomicInteger();
        listener.register("ingot.security.ratelimit.", count::incrementAndGet);

        listener.onEnvironmentChange(new EnvironmentChangeEvent(
                Set.of("ingot.security.ratelimit.policy.rules[0].burst")));

        assertThat(count.get()).isEqualTo(1);
    }

    @Test
    void ignoresUnrelatedKeys() {
        LocalPolicyEnvironmentRefreshListener listener = new LocalPolicyEnvironmentRefreshListener();
        AtomicInteger count = new AtomicInteger();
        listener.register("ingot.security.ratelimit.", count::incrementAndGet);

        listener.onEnvironmentChange(new EnvironmentChangeEvent(
                Set.of("ingot.security.blacklist.policy.items[0].value")));

        assertThat(count.get()).isZero();
    }

    @Test
    void localRateLimitSnapshotRecompilesAfterRefreshCallback() {
        var properties = new com.ingot.framework.gateway.rule.client.ratelimit.config.RateLimitProperties();
        var rule = new com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitRule();
        rule.setCode("test");
        rule.setQps(2);
        rule.setBurst(1);
        rule.setEnabled(true);
        properties.getPolicy().getRules().add(rule);

        var service = new com.ingot.framework.gateway.rule.client.ratelimit.internal.LocalRateLimitRuleService(
                properties);
        var first = service.getSnapshot();
        assertThat(first.getRules().getFirst().getBurst()).isEqualTo(1);

        properties.getPolicy().getRules().getFirst().setBurst(2);
        service.evictAll();

        var second = service.getSnapshot();
        assertThat(second.getRules().getFirst().getBurst()).isEqualTo(2);
        assertThat(second.getVersion()).isGreaterThan(first.getVersion());
    }

    @Test
    void invokesRefresherWhenChallengeKeyMatchesPrefix() {
        LocalPolicyEnvironmentRefreshListener listener = new LocalPolicyEnvironmentRefreshListener();
        AtomicInteger count = new AtomicInteger();
        listener.register("ingot.security.challenge.", count::incrementAndGet);

        listener.onEnvironmentChange(new EnvironmentChangeEvent(
                Set.of("ingot.security.challenge.policy.policies[0].enabled")));

        assertThat(count.get()).isEqualTo(1);
    }

    @Test
    void localChallengeMatchRecompilesAfterRefreshCallback() {
        var properties = new com.ingot.framework.gateway.rule.client.challenge.config.ChallengeProperties();
        var group = com.ingot.framework.gateway.rule.client.ratelimit.model.EndpointGroup.builder()
                .code("login-auth-floor")
                .enabled(true)
                .patternList(java.util.List.of(
                        com.ingot.framework.gateway.rule.client.model.EndpointPattern.of(
                                "/bff/auth/login", "POST")))
                .build();
        var policy = com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy.builder()
                .code("login-always-floor")
                .groupCode("login-auth-floor")
                .trigger(com.ingot.framework.gateway.rule.client.challenge.model.ChallengeTrigger.ALWAYS)
                .challengeType(com.ingot.cloud.security.api.model.enums.ChallengeCaptchaType.VALUE_SLIDER)
                .scope("login")
                .enabled(true)
                .priority(0)
                .build();
        properties.getPolicy().getGroups().add(group);
        properties.getPolicy().getPolicies().add(policy);

        var service = new com.ingot.framework.gateway.rule.client.challenge.internal.LocalChallengePolicyService(
                properties);
        assertThat(service.match("/bff/auth/login", org.springframework.http.HttpMethod.POST,
                com.ingot.framework.gateway.rule.client.challenge.model.ChallengeTrigger.ALWAYS)).isNotNull();

        properties.getPolicy().getPolicies().getFirst().setEnabled(false);
        service.evictAll();

        assertThat(service.match("/bff/auth/login", org.springframework.http.HttpMethod.POST,
                com.ingot.framework.gateway.rule.client.challenge.model.ChallengeTrigger.ALWAYS)).isNull();
    }
}
