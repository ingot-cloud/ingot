package com.ingot.framework.gateway.rule.client.ratelimit;

import com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitSnapshot;

/**
 * 限流规则查询 SPI。
 *
 * <p>调用方（网关 {@code SentinelGatewayConfiguration} / 业务侧）通过本 SPI
 * 获取当前生效的规则快照，用于编译 Sentinel Gateway 参数流控规则。</p>
 *
 * <h3>实现与装配</h3>
 * <ul>
 *     <li>{@code policy.mode=local} — {@link com.ingot.framework.gateway.rule.client.ratelimit.internal.LocalRateLimitRuleService}</li>
 *     <li>{@code policy.mode=remote} — {@link com.ingot.framework.gateway.rule.client.ratelimit.internal.RemoteRateLimitRuleService}</li>
 * </ul>
 *
 * <p>SPI 同步返回；实现内部使用 {@link com.ingot.framework.gateway.rule.client.internal.LocalCompiledCache}
 * 维护 L1 编译缓存，仅在 cache miss 时从 yaml 或 Feign 重新加载。
 * 跨节点变更通过 {@link com.ingot.framework.gateway.rule.client.internal.SecurityPolicyCacheCoordinator}
 * 触发 {@link #evictAll()}。</p>
 *
 * <h3>配置开关</h3>
 * <p>需 {@code ingot.security.ratelimit.enabled=true} 才会装配实现类。
 * yaml 示例见 {@link com.ingot.framework.gateway.rule.client.ratelimit.config.RateLimitProperties}。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
public interface RateLimitRuleService {

    /**
     * 获取当前生效的限流规则快照（含规则列表、分组列表、版本号）。
     * <p>首次调用触发编译 / 拉取；后续命中 L1 缓存直接返回。</p>
     *
     * @return 不可为 null；remote 拉取失败时返回空快照（规则数为 0）
     */
    RateLimitSnapshot getSnapshot();

    /**
     * 失效本地 L1 编译缓存，下次 {@link #getSnapshot()} 将重新从 yaml 或远端加载。
     * <p>由 {@link com.ingot.framework.gateway.rule.client.internal.SecurityPolicyCacheCoordinator}
     * 在收到 {@code RATE_LIMIT_RULE} / {@code ENDPOINT_GROUP} / {@code ALL} 失效事件时调用。</p>
     */
    void evictAll();
}
