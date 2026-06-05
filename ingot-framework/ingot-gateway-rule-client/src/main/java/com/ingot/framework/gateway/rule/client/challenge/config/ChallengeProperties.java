package com.ingot.framework.gateway.rule.client.challenge.config;

import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.gateway.rule.client.ratelimit.model.EndpointGroup;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

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
 *         enabled: true
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
     * 挑战域开关；关闭时网关不进行挑战、直接走 Sentinel 拒绝。
     */
    private boolean enabled = false;

    private Policy policy = new Policy();

    @Getter
    @Setter
    public static class Policy {
        private Mode mode = Mode.LOCAL;
        private List<EndpointGroup> groups = new ArrayList<>();
        private List<ChallengePolicy> policies = new ArrayList<>();
    }

    public enum Mode {
        LOCAL, REMOTE
    }
}
