package com.ingot.cloud.gateway.security;

import com.ingot.framework.gateway.rule.client.blacklist.BlacklistService;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpListSnapshot;
import com.ingot.framework.gateway.rule.client.ratelimit.RateLimitRuleService;
import com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关启动期打印安全策略快照日志，便于运维与 E2E 测试确认 SDK 装配是否正确。
 *
 * <p>通过 {@link ApplicationRunner} 在应用就绪后执行一次：</p>
 * <ul>
 *     <li>限流 — 输出规则数、分组数、版本号（{@link RateLimitRuleService#getSnapshot}）</li>
 *     <li>黑白名单 — 输出条目数、版本号（{@link BlacklistService#getSnapshot}）</li>
 * </ul>
 *
 * <h3>相关配置</h3>
 * <pre>{@code
 * ingot:
 *   security:
 *     ratelimit:
 *       enabled: true          # 关闭时日志：[SecurityPolicy] ratelimit disabled
 *     blacklist:
 *       enabled: true          # 关闭时日志：[SecurityPolicy] blacklist disabled
 * # SDK mode（local / remote）由 ingot-gateway-rule-client 自身配置决定
 * }</pre>
 *
 * <h3>典型日志</h3>
 * <pre>
 * [SecurityPolicy] ratelimit snapshot: rules=12, groups=3, version=20260606120000
 * [SecurityPolicy] blacklist snapshot: items=8, version=20260606120000
 * </pre>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class SecurityPolicyBootstrapLogger {

    @Bean
    public ApplicationRunner securityPolicyBootstrapRunner(
            ObjectProvider<RateLimitRuleService> rateLimitProvider,
            ObjectProvider<BlacklistService> blacklistProvider) {
        return args -> {
            RateLimitRuleService rl = rateLimitProvider.getIfAvailable();
            if (rl == null) {
                log.info("[SecurityPolicy] ratelimit disabled (no RateLimitRuleService bean)");
            } else {
                try {
                    RateLimitSnapshot snap = rl.getSnapshot();
                    log.info("[SecurityPolicy] ratelimit snapshot: rules={}, groups={}, version={}",
                            snap.getRules().size(), snap.getGroups().size(), snap.getVersion());
                } catch (Exception e) {
                    log.warn("[SecurityPolicy] ratelimit snapshot load failed", e);
                }
            }

            BlacklistService bl = blacklistProvider.getIfAvailable();
            if (bl == null) {
                log.info("[SecurityPolicy] blacklist disabled (no BlacklistService bean)");
            } else {
                try {
                    IpListSnapshot snap = bl.getSnapshot();
                    log.info("[SecurityPolicy] blacklist snapshot: items={}, version={}",
                            snap.getItems().size(), snap.getVersion());
                } catch (Exception e) {
                    log.warn("[SecurityPolicy] blacklist snapshot load failed", e);
                }
            }
        };
    }
}
