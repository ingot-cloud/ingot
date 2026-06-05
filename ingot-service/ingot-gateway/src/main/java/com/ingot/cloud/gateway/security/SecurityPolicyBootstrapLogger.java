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
 * 网关启动期把当前生效的限流规则 / 黑白名单 快照打到日志，
 * 便于运维和测试快速确认 SDK 装配与 Mode（local/remote）是否正确。
 *
 * <p>仅在对应域开关打开（{@code ingot.security.ratelimit.enabled=true} /
 * {@code ingot.security.blacklist.enabled=true}）时才会生效，否则 Bean 不存在、
 * ObjectProvider 取空、Runner 静默。</p>
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
