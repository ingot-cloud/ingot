package com.ingot.framework.gateway.rule.client.config;

import java.util.ArrayList;
import java.util.List;

import com.ingot.framework.gateway.rule.client.blacklist.BlacklistService;
import com.ingot.framework.gateway.rule.client.blacklist.config.BlacklistProperties;
import com.ingot.framework.gateway.rule.client.challenge.ChallengePolicyService;
import com.ingot.framework.gateway.rule.client.challenge.config.ChallengeProperties;
import com.ingot.framework.gateway.rule.client.internal.RemoteSnapshotFetcher;
import com.ingot.framework.gateway.rule.client.internal.SecurityPolicyCacheCoordinator;
import com.ingot.framework.gateway.rule.client.ratelimit.RateLimitRuleService;
import com.ingot.framework.gateway.rule.client.ratelimit.config.RateLimitProperties;
import com.ingot.framework.gateway.rule.client.violation.ViolationEscalationService;
import com.ingot.framework.gateway.rule.client.violation.config.ViolationEscalationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>启动期汇总打印安全策略 SDK 各域开关与快照链的实际装配结果，供逐项验证配置是否生效。</p>
 *
 * <p>在所有单例就绪后执行一次（{@link SmartInitializingSingleton}），因此看到的是各域
 * AutoConfiguration 条件评估后的<b>最终状态</b>，而不是 yaml 的字面值。典型输出：</p>
 *
 * <pre>{@code
 * [SecurityPolicy] SDK wiring: coordinator=ON, snapshotFetcher=ON, resilience=true, localFloor=true
 * [SecurityPolicy] domain ratelimit: enabled=true, mode=REMOTE, bean=RemoteRateLimitRuleService
 * [SecurityPolicy] domain blacklist: enabled=true, mode=REMOTE, bean=RemoteBlacklistService
 * [SecurityPolicy] domain challenge: enabled=true, mode=REMOTE, bean=RemoteChallengePolicyService
 * [SecurityPolicy] domain violation-escalation: enabled=true, mode=REMOTE, bean=RemoteViolationEscalationService
 * [SecurityPolicy] local floor contributors: [ratelimit, blacklist, challenge, violation-escalation]
 * }</pre>
 *
 * <p>地板贡献者一行用于核对正交性：仅已启用的域会出现，因为域关闭时其 {@code *Properties}
 * Bean 不装配，{@link com.ingot.framework.gateway.rule.client.internal.LocalPolicyFloorSupplier}
 * 解析到空即跳过该域片段。</p>
 *
 * <p>当某域声明 {@code enabled=true} 却没有对应 Service bean，或 {@code mode=remote}
 * 但快照链缺失时会打 WARN，提示排查 Feign 客户端 {@code RemoteSecurityPolicyService} 的注册情况。</p>
 *
 * @author jy
 * @since 2026/7/30
 * @see GatewayRuleClientAutoConfiguration
 * @implNote 所有依赖均为 {@code @Autowired(required = false)}，任一域关闭时本类仍可正常工作。
 */
@Slf4j
public class GatewayRuleClientWiringReporter implements SmartInitializingSingleton {

    @Autowired(required = false)
    private GatewayRuleClientProperties clientProperties;

    @Autowired(required = false)
    private SecurityPolicyCacheCoordinator coordinator;

    @Autowired(required = false)
    private RemoteSnapshotFetcher snapshotFetcher;

    @Autowired(required = false)
    private RateLimitProperties rateLimitProperties;

    @Autowired(required = false)
    private RateLimitRuleService rateLimitRuleService;

    @Autowired(required = false)
    private BlacklistProperties blacklistProperties;

    @Autowired(required = false)
    private BlacklistService blacklistService;

    @Autowired(required = false)
    private ViolationEscalationProperties violationEscalationProperties;

    @Autowired(required = false)
    private ViolationEscalationService violationEscalationService;

    @Autowired(required = false)
    private ChallengeProperties challengeProperties;

    @Autowired(required = false)
    private ChallengePolicyService challengePolicyService;

    @Override
    public void afterSingletonsInstantiated() {
        log.info("[SecurityPolicy] SDK wiring: coordinator={}, snapshotFetcher={}, resilience={}, localFloor={}",
                onOff(coordinator != null), onOff(snapshotFetcher != null),
                clientProperties != null && clientProperties.isResilienceEnabled(),
                clientProperties != null && clientProperties.isLocalFloorEnabled());

        reportDomain("ratelimit",
                rateLimitProperties != null && rateLimitProperties.isEnabled(),
                rateLimitProperties == null ? null : rateLimitProperties.getPolicy().getMode().name(),
                rateLimitRuleService);
        reportDomain("blacklist",
                blacklistProperties != null && blacklistProperties.isEnabled(),
                blacklistProperties == null ? null : blacklistProperties.getPolicy().getMode().name(),
                blacklistService);
        reportDomain("challenge",
                challengeProperties != null && challengeProperties.isEnabled(),
                challengeProperties == null ? null : challengeProperties.getPolicy().getMode().name(),
                challengePolicyService);
        reportDomain("violation-escalation",
                violationEscalationProperties != null && violationEscalationProperties.isEnabled(),
                violationEscalationProperties == null ? null : violationEscalationProperties.getPolicy().getMode().name(),
                violationEscalationService);

        reportFloorContributors();
    }

    /**
     * 汇总哪些域会向 Nacos 地板贡献片段：域的 {@code *Properties} Bean 存在即代表该域已启用。
     */
    private void reportFloorContributors() {
        List<String> contributors = new ArrayList<>();
        if (rateLimitProperties != null) {
            contributors.add("ratelimit");
        }
        if (blacklistProperties != null) {
            contributors.add("blacklist");
        }
        if (challengeProperties != null) {
            contributors.add("challenge");
        }
        if (violationEscalationProperties != null) {
            contributors.add("violation-escalation");
        }
        log.info("[SecurityPolicy] local floor contributors: {}", contributors);
    }

    private void reportDomain(String domain, boolean enabled, String mode, Object service) {
        if (!enabled && service == null) {
            log.info("[SecurityPolicy] domain {}: enabled=false (set ingot.security.{}.enabled=true to activate)",
                    domain, domain);
            return;
        }
        if (service == null) {
            log.warn("[SecurityPolicy] domain {}: enabled=true but no service bean, "
                            + "check ingot.security.{}.enabled and auto-configuration conditions",
                    domain, domain);
            return;
        }
        log.info("[SecurityPolicy] domain {}: enabled={}, mode={}, bean={}",
                domain, enabled, mode, service.getClass().getSimpleName());
        if ("REMOTE".equals(mode) && snapshotFetcher == null) {
            log.warn("[SecurityPolicy] domain {} is remote but RemoteSnapshotFetcher is absent, "
                            + "check RemoteSecurityPolicyService Feign client registration",
                    domain);
        }
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
