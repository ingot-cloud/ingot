package com.ingot.framework.gateway.rule.client.ratelimit.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 限流规则快照：规则 + 分组定义 + 版本号。
 *
 * <p>由 {@link com.ingot.framework.gateway.rule.client.ratelimit.RateLimitPolicyService}
 * 持有并编译为 Sentinel Gateway 规则；{@link #groups} 供 {@link #rules} 通过
 * {@link RateLimitRule#getGroupCode()} 解析共享路径集合。</p>
 *
 * <p>yaml 配置示例见
 * {@link com.ingot.framework.gateway.rule.client.ratelimit.config.RateLimitProperties}。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitSnapshot implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 限流规则定义列表，每条对应一个 Sentinel apiName / resource。
     * 仅 {@link RateLimitRule#isEnabled()} 为 {@code true} 的规则参与编译；
     * 未出现在快照中的路径默认不限流（白名单式限流原则）。
     */
    private List<RateLimitRule> rules;

    /**
     * API 路径分组定义列表，供多条规则复用同一组 {@link com.ingot.framework.gateway.rule.client.model.EndpointPattern}。
     * 规则可通过 {@link RateLimitRule#getGroupCode()} 引用分组，避免重复声明 path；
     * 亦可供挑战策略域复用同一分组编码。
     */
    private List<EndpointGroup> groups;

    /**
     * 单调递增版本号，用于一致性校验。
     */
    private long version;

    public static RateLimitSnapshot empty() {
        return new RateLimitSnapshot(Collections.emptyList(), Collections.emptyList(), 0L);
    }
}
