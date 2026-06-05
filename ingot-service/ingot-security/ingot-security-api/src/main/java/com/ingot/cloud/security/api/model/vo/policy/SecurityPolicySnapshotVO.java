package com.ingot.cloud.security.api.model.vo.policy;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 安全策略快照 VO，由 InnerSecurityPolicyAPI 一次返回全部域，
 * 网关 / 业务方据此本地编译规则，避免多次 Feign 调用。
 *
 * @author jy
 * @since 2026/5/26
 */
@Data
public class SecurityPolicySnapshotVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private List<EndpointGroupVO> groups;

    private List<RateLimitRuleVO> rateLimitRules;

    private List<IpListItemVO> ipList;

    /**
     * 后续 Phase 加入挑战策略 / VC 配置等。
     */
    private List<ChallengePolicyVO> challengePolicies;

    /**
     * 单调递增版本号。
     */
    private long version;
}
