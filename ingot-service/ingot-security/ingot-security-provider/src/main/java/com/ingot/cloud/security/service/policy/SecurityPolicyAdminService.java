package com.ingot.cloud.security.service.policy;

import com.ingot.cloud.security.model.domain.GatewayBlacklistEvent;
import com.ingot.cloud.security.model.domain.GatewayEndpointGroup;
import com.ingot.cloud.security.model.domain.GatewayIpList;
import com.ingot.cloud.security.model.domain.GatewayRateLimitRule;
import com.ingot.cloud.security.model.domain.GatewayViolationEscalation;
import com.ingot.cloud.security.model.domain.SecurityChallengePolicy;

import java.util.List;

/**
 * 安全策略管理面 Service。
 *
 * <p>写操作内均 publish {@link SecurityPolicyChangedSpringEvent}，事务提交后由
 * {@link SecurityPolicyInvalidationPublisher} 广播失效。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
public interface SecurityPolicyAdminService {

    // ====== endpoint group ======
    List<GatewayEndpointGroup> listGroups();

    GatewayEndpointGroup saveGroup(GatewayEndpointGroup group);

    GatewayEndpointGroup updateGroup(GatewayEndpointGroup group);

    void deleteGroup(Long id);

    // ====== rate limit rule ======
    List<GatewayRateLimitRule> listRules();

    GatewayRateLimitRule saveRule(GatewayRateLimitRule rule);

    GatewayRateLimitRule updateRule(GatewayRateLimitRule rule);

    void deleteRule(Long id);

    // ====== ip list ======
    List<GatewayIpList> listIpList();

    GatewayIpList saveIpList(GatewayIpList item);

    GatewayIpList updateIpList(GatewayIpList item);

    void deleteIpList(Long id);

    // ====== blacklist event ======
    List<GatewayBlacklistEvent> listEvents(int limit);

    void recordEvent(GatewayBlacklistEvent event);

    // ====== challenge policy ======
    List<SecurityChallengePolicy> listChallengePolicies();

    SecurityChallengePolicy saveChallengePolicy(SecurityChallengePolicy policy);

    SecurityChallengePolicy updateChallengePolicy(SecurityChallengePolicy policy);

    void deleteChallengePolicy(Long id);

    // ====== violation escalation ======
    GatewayViolationEscalation getViolationEscalation();

    GatewayViolationEscalation saveViolationEscalation(GatewayViolationEscalation config);

    // ====== snapshot ======
    /**
     * 一次性获取四张表的最新数据（限流规则 + 分组 + 黑白名单 [+审计])；
     * 仅供 Inner Feign 使用。
     */
    SecurityPolicySnapshot snapshot();

    /**
     * 直接广播一次"全量失效"，可用于运营强制刷新。
     */
    void broadcastInvalidationAll();
}
