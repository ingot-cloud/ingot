package com.ingot.framework.gateway.rule.client.challenge.config;

import java.util.ArrayList;
import java.util.List;

import com.ingot.framework.commons.model.security.PolicySourceMode;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.gateway.rule.client.ratelimit.model.EndpointGroup;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 挑战策略配置（前缀 {@code ingot.security.challenge}）。
 *
 * <h3>典型配置示例 — local 模式</h3>
 *
 * <pre>{@code
 * ingot:
 *   security:
 *     challenge:
 *       enabled: true
 *       policy:
 *         mode: local
 *         groups:
 *           - code: login-flow
 *             name: 登录相关接口
 *             enabled: true
 *             pattern-list:
 *               - path: /auth/token
 *                 method: POST
 *         policies:
 *           - code: login-always
 *             group-code: login-flow
 *             trigger: ALWAYS
 *             challenge-type: SLIDER          # 映射为 VC 路由 /vc/image/check
 *             scope: login
 *             pass-token-ttl-sec: 300
 *             pass-token-remaining: 3
 *             enabled: true
 *             priority: 0
 *           - code: anon-rate-limit
 *             pattern-list:
 *               - path: /anonymous/**
 *                 method: ANY
 *             trigger: ON_RATE_LIMIT
 *             challenge-type: SLIDER
 *             scope: anon
 *             pass-token-ttl-sec: 120
 *             pass-token-remaining: 1
 *             enabled: true
 *             priority: 10
 * }</pre>
 *
 * <h3>典型配置示例 — remote 模式</h3>
 *
 * <pre>{@code
 * ingot:
 *   security:
 *     policy:
 *       client:
 *         invalidation-enabled: true
 *     challenge:
 *       enabled: true
 *       policy:
 *         mode: remote
 * }</pre>
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.security.challenge")
public class ChallengeProperties {

    /**
     * 挑战域总开关。
     * <ul>
     *     <li>{@code false}（默认）：不装配 {@link com.ingot.framework.gateway.rule.client.challenge.ChallengePolicyService}；
     *         网关限流触发后不弹出验证码，直接返回 Sentinel 429。</li>
     *     <li>{@code true}：装配 SDK + 网关挑战过滤器，按策略触发 IMAGE / SLIDER 验证码。</li>
     * </ul>
     * <p>本字段由 {@link ChallengeAutoConfiguration} 上的 {@code @ConditionalOnProperty} 按属性键
     * {@code ingot.security.challenge.enabled} 消费，是挑战域生效的<b>唯一</b>门控，
     * 与 {@code ingot.security.policy.client.*} 互不级联；关闭时本类的 Properties Bean 不装配，
     * Nacos 地板中的挑战片段随之为空。</p>
     */
    private boolean enabled = false;

    /** 挑战策略加载配置：模式 + local 模式下的分组与策略列表。 */
    private Policy policy = new Policy();

    /**
     * 挑战策略加载配置。
     */
    @Getter
    @Setter
    public static class Policy {
        /**
         * 加载模式：
         * <ul>
         *     <li>{@link PolicySourceMode#LOCAL}（默认）— 读下方 {@link #groups} / {@link #policies} yaml 配置，
         *         适合本机调试 / 单实例</li>
         *     <li>{@link PolicySourceMode#REMOTE} — Feign 拉 ingot-service-security 快照中的
         *         {@code challengePolicies}，适合生产 / 多节点热更新</li>
         * </ul>
         */
        private PolicySourceMode mode = PolicySourceMode.LOCAL;

        /**
         * local 模式下的 API 路径分组；策略通过 {@link ChallengePolicy#getGroupCode()} 引用。
         * remote 模式下被忽略（分组来自快照中的 {@code groups}）。
         */
        private List<EndpointGroup> groups = new ArrayList<>();

        /**
         * local 模式下的挑战策略列表；remote 模式下被忽略。
         * 每条策略定义触发条件（{@link ChallengePolicy#getTrigger()}）、验证码类型、
         * PassToken 有效期等，详见 {@link ChallengePolicy}。
         */
        private List<ChallengePolicy> policies = new ArrayList<>();
    }
}
