package com.ingot.cloud.gateway.security;

import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 安全策略相关的 Sentinel Gateway 装配。
 *
 * <p>替换 Spring Cloud Alibaba 默认的 {@link SentinelGatewayFilter}，解决两个问题：</p>
 * <ol>
 *     <li><b>执行顺序</b>：默认 order 为 {@code HIGHEST_PRECEDENCE}，早于
 *         {@link BlacklistFilter}（+30），白名单标记尚未写入；此处改为
 *         {@link SecurityPolicyFilterOrder#SENTINEL}（+50）。</li>
 *     <li><b>白名单跳过限流</b>：{@link WhitelistAwareSentinelGatewayFilter} 在
 *         {@link BlacklistFilter#ATTR_WHITELISTED} 为 true 时直接放行。</li>
 * </ol>
 *
 * <p>本配置在用户 {@code @Configuration} 中注册，优先于
 * {@code SentinelSCGAutoConfiguration} 的同名 Bean（后者带
 * {@code @ConditionalOnMissingBean}，检测到已存在则跳过）。</p>
 *
 * <h3>等效 yaml（仅供参考，通常无需手写）</h3>
 * <pre>{@code
 * spring:
 *   cloud:
 *     sentinel:
 *       scg:
 *         enabled: true
 *         order: -2147483598   # HIGHEST_PRECEDENCE + 50，与 SecurityPolicyFilterOrder.SENTINEL 相同
 * }</pre>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(SentinelGatewayFilter.class)
@ConditionalOnProperty(prefix = "spring.cloud.sentinel.scg", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class SecurityPolicySentinelConfiguration {

    @Bean
    @ConditionalOnMissingBean(SentinelGatewayFilter.class)
    public SentinelGatewayFilter sentinelGatewayFilter() {
        log.info("[SecurityPolicy] register WhitelistAwareSentinelGatewayFilter, order={}",
                SecurityPolicyFilterOrder.SENTINEL);
        return new WhitelistAwareSentinelGatewayFilter(SecurityPolicyFilterOrder.SENTINEL);
    }
}
