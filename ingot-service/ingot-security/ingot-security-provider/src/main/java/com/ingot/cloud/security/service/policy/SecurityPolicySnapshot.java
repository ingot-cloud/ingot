package com.ingot.cloud.security.service.policy;

import com.ingot.cloud.security.model.domain.GatewayEndpointGroup;
import com.ingot.cloud.security.model.domain.GatewayIpList;
import com.ingot.cloud.security.model.domain.GatewayRateLimitRule;
import com.ingot.cloud.security.model.domain.SecurityChallengePolicy;

import java.util.List;

/**
 * 内部快照容器，给 Inner API 转 VO 用。
 *
 * @author jy
 * @since 2026/5/26
 */
public record SecurityPolicySnapshot(
        List<GatewayEndpointGroup> groups,
        List<GatewayRateLimitRule> rules,
        List<GatewayIpList> ipList,
        List<SecurityChallengePolicy> challengePolicies,
        long version
) {
}
