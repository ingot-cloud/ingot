package com.ingot.framework.gateway.rule.client.config;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.ingot.cloud.security.api.model.vo.policy.IpListItemVO;
import com.ingot.cloud.security.api.model.vo.policy.RateLimitRuleVO;
import com.ingot.cloud.security.api.model.vo.policy.SecurityPolicySnapshotVO;
import com.ingot.cloud.security.api.rpc.RemoteSecurityPolicyService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.gateway.rule.client.blacklist.BlacklistService;
import com.ingot.framework.gateway.rule.client.blacklist.config.BlacklistAutoConfiguration;
import com.ingot.framework.gateway.rule.client.challenge.ChallengePolicyService;
import com.ingot.framework.gateway.rule.client.challenge.config.ChallengeAutoConfiguration;
import com.ingot.framework.gateway.rule.client.ratelimit.RateLimitRuleService;
import com.ingot.framework.gateway.rule.client.ratelimit.config.RateLimitAutoConfiguration;
import com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitSnapshot;
import com.ingot.framework.gateway.rule.client.violation.ViolationEscalationService;
import com.ingot.framework.gateway.rule.client.violation.config.ViolationEscalationAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * <p>四域共享快照缓存的行为验证：远端调用去重、版本驱动的派生缓存复用、失效后重新拉取。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class SharedSnapshotCacheTest {

    private static final AtomicInteger FEIGN_CALLS = new AtomicInteger();
    private static final AtomicLong VERSION = new AtomicLong(1);

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    GatewayRuleClientAutoConfiguration.class,
                    RateLimitAutoConfiguration.class,
                    BlacklistAutoConfiguration.class,
                    ChallengeAutoConfiguration.class,
                    ViolationEscalationAutoConfiguration.class))
            .withUserConfiguration(FeignStubConfiguration.class)
            .withPropertyValues(
                    "ingot.security.ratelimit.enabled=true",
                    "ingot.security.ratelimit.policy.mode=remote",
                    "ingot.security.blacklist.enabled=true",
                    "ingot.security.blacklist.policy.mode=remote",
                    "ingot.security.challenge.enabled=true",
                    "ingot.security.challenge.policy.mode=remote",
                    "ingot.security.violation-escalation.enabled=true",
                    "ingot.security.violation-escalation.policy.mode=remote");

    @Test
    @DisplayName("四个域冷启动只产生一次远端调用")
    void fourDomainsShareSingleFeignCall() {
        FEIGN_CALLS.set(0);
        VERSION.set(1);

        runner.run(context -> {
            context.getBean(RateLimitRuleService.class).getSnapshot();
            context.getBean(BlacklistService.class).getSnapshot();
            context.getBean(ChallengePolicyService.class).getSnapshot();
            context.getBean(ViolationEscalationService.class).getConfig();

            assertThat(FEIGN_CALLS.get()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("重复读取命中 L1，不再访问远端")
    void repeatedReadsHitL1() {
        FEIGN_CALLS.set(0);
        VERSION.set(1);

        runner.run(context -> {
            RateLimitRuleService service = context.getBean(RateLimitRuleService.class);
            service.getSnapshot();
            service.getSnapshot();
            service.getSnapshot();

            assertThat(FEIGN_CALLS.get()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("版本未变化时派生缓存复用同一编译产物")
    void derivedCacheReusesOnSameVersion() {
        FEIGN_CALLS.set(0);
        VERSION.set(1);

        runner.run(context -> {
            RateLimitRuleService service = context.getBean(RateLimitRuleService.class);
            RateLimitSnapshot first = service.getSnapshot();
            RateLimitSnapshot second = service.getSnapshot();

            assertThat(second).isSameAs(first);
        });
    }

    @Test
    @DisplayName("失效后重新拉取远端并产出新的编译产物")
    void evictTriggersRefetchAndRecompile() {
        FEIGN_CALLS.set(0);
        VERSION.set(1);

        runner.run(context -> {
            RateLimitRuleService service = context.getBean(RateLimitRuleService.class);
            RateLimitSnapshot first = service.getSnapshot();
            assertThat(first.getVersion()).isEqualTo(1);

            VERSION.set(2);
            service.evictAll();
            RateLimitSnapshot second = service.getSnapshot();

            assertThat(FEIGN_CALLS.get()).isEqualTo(2);
            assertThat(second.getVersion()).isEqualTo(2);
            assertThat(second).isNotSameAs(first);
        });
    }

    @Test
    @DisplayName("一个域触发失效后，其他域也读到新版本")
    void evictFromOneDomainRefreshesShared() {
        FEIGN_CALLS.set(0);
        VERSION.set(1);

        runner.run(context -> {
            RateLimitRuleService rateLimit = context.getBean(RateLimitRuleService.class);
            BlacklistService blacklist = context.getBean(BlacklistService.class);
            rateLimit.getSnapshot();
            blacklist.getSnapshot();

            VERSION.set(5);
            rateLimit.evictAll();

            assertThat(rateLimit.getSnapshot().getVersion()).isEqualTo(5);
            assertThat(blacklist.getSnapshot().getVersion()).isEqualTo(5);
            assertThat(FEIGN_CALLS.get()).isEqualTo(2);
        });
    }

    private static SecurityPolicySnapshotVO snapshot() {
        SecurityPolicySnapshotVO vo = new SecurityPolicySnapshotVO();
        vo.setVersion(VERSION.get());
        RateLimitRuleVO rule = new RateLimitRuleVO();
        rule.setCode("login");
        rule.setQps(10);
        rule.setEnabled(true);
        vo.setRateLimitRules(List.of(rule));
        vo.setGroups(List.of());
        vo.setIpList(List.<IpListItemVO>of());
        vo.setChallengePolicies(List.of());
        return vo;
    }

    @Configuration(proxyBeanMethods = false)
    static class FeignStubConfiguration {

        @Bean
        RemoteSecurityPolicyService remoteSecurityPolicyService() {
            RemoteSecurityPolicyService mock = Mockito.mock(RemoteSecurityPolicyService.class);
            when(mock.snapshot()).thenAnswer(inv -> {
                FEIGN_CALLS.incrementAndGet();
                return R.ok(snapshot());
            });
            return mock;
        }
    }
}
