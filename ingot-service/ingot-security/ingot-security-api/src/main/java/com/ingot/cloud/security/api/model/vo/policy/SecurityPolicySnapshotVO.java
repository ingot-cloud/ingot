package com.ingot.cloud.security.api.model.vo.policy;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 安全策略全量快照视图对象。
 *
 * <p>由 {@code InnerSecurityPolicyAPI} 一次返回全部策略域，
 * 网关 / 业务方据此本地编译规则，避免多次 Feign 调用。
 * 客户端通过 {@link #version} 判断是否需要刷新本地缓存。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Data
public class SecurityPolicySnapshotVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * API 路径分组列表，对应 {@code gateway_endpoint_group} 全量启用记录。
     */
    private List<EndpointGroupVO> groups;

    /**
     * 限流规则列表，对应 {@code gateway_rate_limit_rule} 全量启用记录。
     */
    private List<RateLimitRuleVO> rateLimitRules;

    /**
     * 黑白名单条目列表，对应 {@code gateway_ip_list} 当前有效记录。
     */
    private List<IpListItemVO> ipList;

    /**
     * 挑战策略列表，对应 {@code security_challenge_policy}。
     */
    private List<ChallengePolicyVO> challengePolicies;

    /**
     * 限流违规升级全局配置，对应 {@code gateway_violation_escalation} 单行表。
     */
    private ViolationEscalationVO violationEscalation;

    /**
     * 快照单调递增版本号；任意策略变更后递增，供 SDK 增量比对与 InvalidationBus 通知。
     */
    private long version;
}
