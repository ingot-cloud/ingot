package com.ingot.framework.gateway.rule.client.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ingot.cloud.security.api.rpc.RemoteSecurityPolicyService;
import com.ingot.framework.gateway.rule.client.blacklist.BlacklistService;
import com.ingot.framework.gateway.rule.client.blacklist.config.BlacklistAutoConfiguration;
import com.ingot.framework.gateway.rule.client.blacklist.config.BlacklistProperties;
import com.ingot.framework.gateway.rule.client.internal.LocalPolicyFloorSupplier;
import com.ingot.framework.gateway.rule.client.internal.RemoteSnapshotFetcher;
import com.ingot.framework.gateway.rule.client.ratelimit.RateLimitRuleService;
import com.ingot.framework.gateway.rule.client.ratelimit.config.RateLimitAutoConfiguration;
import com.ingot.framework.gateway.rule.client.ratelimit.config.RateLimitProperties;
import com.ingot.framework.gateway.rule.client.violation.ViolationEscalationService;
import com.ingot.framework.gateway.rule.client.violation.config.ViolationEscalationAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SDK 装配正交性测试：各域开关与 {@code ingot.security.policy.client.*} 互不级联。
 */
class GatewayRuleClientWiringTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    GatewayRuleClientAutoConfiguration.class,
                    RateLimitAutoConfiguration.class,
                    BlacklistAutoConfiguration.class,
                    ViolationEscalationAutoConfiguration.class))
            .withUserConfiguration(FeignStubConfiguration.class);

    /**
     * 核心回归：不设置任何 {@code ingot.security.policy.client.*} 键时，
     * remote 模式的域依然能拿到 {@link RemoteSnapshotFetcher}（原先依赖已删除的 {@code client.enabled}）。
     */
    @Test
    void remoteDomainWiresWithoutAnyPolicyClientProperty() {
        runner.withPropertyValues(
                        "ingot.security.ratelimit.enabled=true",
                        "ingot.security.ratelimit.policy.mode=remote")
                .run(context -> {
                    assertThat(context).hasSingleBean(RemoteSnapshotFetcher.class);
                    assertThat(context).hasSingleBean(RateLimitRuleService.class);
                    assertThat(context.getBean(RateLimitRuleService.class).getClass().getSimpleName())
                            .isEqualTo("RemoteRateLimitRuleService");
                });
    }

    /**
     * 快照链是能力层：所有域均关闭时也照常装配，供后续按需启用。
     */
    @Test
    void snapshotChainWiresEvenWithAllDomainsDisabled() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(RemoteSnapshotFetcher.class);
            assertThat(context).hasSingleBean(LocalPolicyFloorSupplier.class);
            assertThat(context).doesNotHaveBean(RateLimitRuleService.class);
            assertThat(context).doesNotHaveBean(BlacklistService.class);
            assertThat(context).doesNotHaveBean(ViolationEscalationService.class);
        });
    }

    /**
     * 关闭单个域不影响其他域的 remote 装配，且被关域的 Properties 不参与地板。
     */
    @Test
    void disablingOneDomainLeavesOthersIntact() {
        runner.withPropertyValues(
                        "ingot.security.ratelimit.enabled=false",
                        "ingot.security.blacklist.enabled=true",
                        "ingot.security.blacklist.policy.mode=remote",
                        "ingot.security.violation-escalation.enabled=true",
                        "ingot.security.violation-escalation.policy.mode=remote")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RateLimitRuleService.class);
                    assertThat(context).doesNotHaveBean(RateLimitProperties.class);
                    assertThat(context).hasSingleBean(BlacklistProperties.class);
                    assertThat(context).hasSingleBean(BlacklistService.class);
                    assertThat(context).hasSingleBean(ViolationEscalationService.class);
                    assertThat(context.getBean(LocalPolicyFloorSupplier.class).get()
                            .getRateLimitRules()).isEmpty();
                });
    }

    /**
     * {@code resilience-enabled=false} 只改变拉取行为，不影响链上 Bean 的存在。
     */
    @Test
    void resilienceDisabledStillWiresFetcher() {
        runner.withPropertyValues(
                        "ingot.security.policy.client.resilience-enabled=false",
                        "ingot.security.ratelimit.enabled=true",
                        "ingot.security.ratelimit.policy.mode=remote")
                .run(context -> {
                    assertThat(context).hasSingleBean(RemoteSnapshotFetcher.class);
                    assertThat(context).hasSingleBean(RateLimitRuleService.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class FeignStubConfiguration {

        @Bean
        RemoteSecurityPolicyService remoteSecurityPolicyService() {
            return Mockito.mock(RemoteSecurityPolicyService.class);
        }
    }
}
