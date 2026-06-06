package com.ingot.framework.gateway.rule.client.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 安全策略客户端通用配置。
 *
 * <p>各域（限流 / 黑白名单 / 挑战策略 / 验证码）独立的
 * {@code ingot.security.<domain>.policy.mode = local | remote} 仅控制 Loader 装配；
 * 本类承载与域无关的 SDK 通用开关与失效广播配置。</p>
 *
 * <h3>当前实现说明</h3>
 * <p>L1 缓存由各域 Service 内部的
 * {@link com.ingot.framework.gateway.rule.client.internal.LocalCompiledCache}
 * 持有（{@code AtomicReference} 语义：要么有完整快照，要么空，没有 TTL / size 上限）。
 * 跨节点失效统一通过 {@link com.ingot.framework.eventbus.InvalidationBus} 广播事件，
 * 由 {@link com.ingot.framework.gateway.rule.client.internal.SecurityPolicyCacheCoordinator}
 * 派发到各域 evictor + Sentinel reload；不依赖 Caffeine TTL，也无 L2 Redis 缓存。</p>
 *
 * <h3>典型配置示例</h3>
 *
 * <pre>{@code
 * ingot:
 *   security:
 *     policy:
 *       client:
 *         enabled: true                 # SDK 总开关
 *         invalidation-enabled: true    # 跨节点失效订阅（生产建议开）
 * }</pre>
 *
 * <p>单实例 + local 模式且不需要热更新时，{@code invalidation-enabled} 可设为 false
 * 跳过 InvalidationBus 订阅，避免无意义的 Redis Pub/Sub 连接。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.security.policy.client")
public class GatewayRuleClientProperties {

    /**
     * SDK 总开关。
     * <ul>
     *     <li>{@code true}（默认）：装配
     *         {@link com.ingot.framework.gateway.rule.client.config.GatewayRuleClientAutoConfiguration}
     *         及其依赖的 Coordinator / RemoteSnapshotFetcher。</li>
     *     <li>{@code false}：跳过整个 SDK 自动装配；各域子模块（ratelimit / blacklist 等）
     *         仍可独立装配，但无法接收跨节点失效事件。</li>
     * </ul>
     * 对应 yaml 键：{@code ingot.security.policy.client.enabled}。
     */
    private boolean enabled = true;

    /**
     * 跨节点失效订阅开关。
     * <ul>
     *     <li>{@code true}（默认）：装配
     *         {@link com.ingot.framework.gateway.rule.client.internal.SecurityPolicyCacheCoordinator}，
     *         订阅 {@link com.ingot.cloud.security.api.event.SecurityPolicyInvalidationEvent}，
     *         Platform 改规则后各节点自动 evict + reload。</li>
     *     <li>{@code false}：不订阅；规则变更后需要手动重启网关或调用
     *         {@code POST /platform/security/policy/broadcast-invalidation} 触发。
     *         适用于单实例 + 纯 local 调试。</li>
     * </ul>
     * 对应 yaml 键：{@code ingot.security.policy.client.invalidation-enabled}。
     */
    private boolean invalidationEnabled = true;
}
