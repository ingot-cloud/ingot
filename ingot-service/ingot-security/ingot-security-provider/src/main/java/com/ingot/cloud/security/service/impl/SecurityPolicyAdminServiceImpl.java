package com.ingot.cloud.security.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.cloud.security.api.event.SecurityPolicyInvalidationEvent;
import com.ingot.cloud.security.mapper.GatewayBlacklistEventMapper;
import com.ingot.cloud.security.mapper.GatewayEndpointGroupMapper;
import com.ingot.cloud.security.mapper.GatewayIpListMapper;
import com.ingot.cloud.security.mapper.GatewayRateLimitRuleMapper;
import com.ingot.cloud.security.mapper.GatewayViolationEscalationMapper;
import com.ingot.cloud.security.mapper.SecurityChallengePolicyMapper;
import com.ingot.cloud.security.model.domain.GatewayBlacklistEvent;
import com.ingot.cloud.security.model.domain.GatewayEndpointGroup;
import com.ingot.cloud.security.model.domain.GatewayIpList;
import com.ingot.cloud.security.model.domain.GatewayRateLimitRule;
import com.ingot.cloud.security.model.domain.GatewayViolationEscalation;
import com.ingot.cloud.security.model.domain.SecurityChallengePolicy;
import com.ingot.cloud.security.service.policy.SecurityPolicyAdminService;
import com.ingot.cloud.security.service.policy.SecurityPolicyChangedSpringEvent;
import com.ingot.cloud.security.service.policy.SecurityPolicySnapshot;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.eventbus.InvalidationBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 安全策略管理面 Service 实现。
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityPolicyAdminServiceImpl implements SecurityPolicyAdminService {

    private final GatewayEndpointGroupMapper groupMapper;
    private final GatewayRateLimitRuleMapper ruleMapper;
    private final GatewayIpListMapper ipListMapper;
    private final GatewayBlacklistEventMapper eventMapper;
    private final SecurityChallengePolicyMapper challengeMapper;
    private final GatewayViolationEscalationMapper violationEscalationMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AssertionChecker assertionChecker;
    private final ObjectProvider<InvalidationBus> invalidationBusProvider;

    // ============== endpoint group ==============

    @Override
    public List<GatewayEndpointGroup> listGroups() {
        return groupMapper.selectList(Wrappers.<GatewayEndpointGroup>lambdaQuery()
                .orderByAsc(GatewayEndpointGroup::getCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GatewayEndpointGroup saveGroup(GatewayEndpointGroup group) {
        assertionChecker.checkOperation(group.getCode() != null, "SecurityPolicy.CodeNotNull");
        LocalDateTime now = DateUtil.now();
        group.setCreatedAt(now);
        group.setUpdatedAt(now);
        groupMapper.insert(group);
        publishChanged(SecurityPolicyDomain.ENDPOINT_GROUP);
        return group;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GatewayEndpointGroup updateGroup(GatewayEndpointGroup group) {
        assertionChecker.checkOperation(group.getId() != null, "SecurityPolicy.IdNotNull");
        group.setUpdatedAt(DateUtil.now());
        groupMapper.updateById(group);
        publishChanged(SecurityPolicyDomain.ENDPOINT_GROUP);
        return group;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long id) {
        groupMapper.deleteById(id);
        publishChanged(SecurityPolicyDomain.ENDPOINT_GROUP);
    }

    // ============== rate limit rule ==============

    @Override
    public List<GatewayRateLimitRule> listRules() {
        return ruleMapper.selectList(Wrappers.<GatewayRateLimitRule>lambdaQuery()
                .orderByAsc(GatewayRateLimitRule::getPriority));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GatewayRateLimitRule saveRule(GatewayRateLimitRule rule) {
        assertionChecker.checkOperation(rule.getCode() != null, "SecurityPolicy.CodeNotNull");
        LocalDateTime now = DateUtil.now();
        rule.setCreatedAt(now);
        rule.setUpdatedAt(now);
        ruleMapper.insert(rule);
        publishChanged(SecurityPolicyDomain.RATE_LIMIT_RULE);
        return rule;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GatewayRateLimitRule updateRule(GatewayRateLimitRule rule) {
        assertionChecker.checkOperation(rule.getId() != null, "SecurityPolicy.IdNotNull");
        rule.setUpdatedAt(DateUtil.now());
        ruleMapper.updateById(rule);
        publishChanged(SecurityPolicyDomain.RATE_LIMIT_RULE);
        return rule;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id) {
        ruleMapper.deleteById(id);
        publishChanged(SecurityPolicyDomain.RATE_LIMIT_RULE);
    }

    // ============== ip list ==============

    @Override
    public List<GatewayIpList> listIpList() {
        return ipListMapper.selectList(Wrappers.<GatewayIpList>lambdaQuery()
                .orderByDesc(GatewayIpList::getCreatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GatewayIpList saveIpList(GatewayIpList item) {
        assertionChecker.checkOperation(item.getListType() != null
                        && item.getKeyType() != null
                        && item.getKeyValue() != null,
                "SecurityPolicy.IpListFieldNotNull");
        LocalDateTime now = DateUtil.now();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        ipListMapper.insert(item);
        publishChanged(SecurityPolicyDomain.IP_LIST);
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GatewayIpList updateIpList(GatewayIpList item) {
        assertionChecker.checkOperation(item.getId() != null, "SecurityPolicy.IdNotNull");
        item.setUpdatedAt(DateUtil.now());
        ipListMapper.updateById(item);
        publishChanged(SecurityPolicyDomain.IP_LIST);
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteIpList(Long id) {
        ipListMapper.deleteById(id);
        publishChanged(SecurityPolicyDomain.IP_LIST);
    }

    // ============== blacklist event ==============

    @Override
    public List<GatewayBlacklistEvent> listEvents(int limit) {
        int effective = limit <= 0 ? 100 : Math.min(limit, 1000);
        return eventMapper.selectList(Wrappers.<GatewayBlacklistEvent>lambdaQuery()
                .orderByDesc(GatewayBlacklistEvent::getCreatedAt)
                .last("limit " + effective));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordEvent(GatewayBlacklistEvent event) {
        event.setCreatedAt(DateUtil.now());
        eventMapper.insert(event);
    }

    // ============== challenge policy ==============

    @Override
    public List<SecurityChallengePolicy> listChallengePolicies() {
        return challengeMapper.selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers.<SecurityChallengePolicy>lambdaQuery()
                .orderByAsc(SecurityChallengePolicy::getPriority));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SecurityChallengePolicy saveChallengePolicy(SecurityChallengePolicy policy) {
        assertionChecker.checkOperation(policy.getCode() != null, "SecurityPolicy.CodeNotNull");
        validateChallengeTrigger(policy);
        LocalDateTime now = DateUtil.now();
        policy.setCreatedAt(now);
        policy.setUpdatedAt(now);
        challengeMapper.insert(policy);
        publishChanged(com.ingot.cloud.security.api.event.SecurityPolicyDomain.CHALLENGE_POLICY);
        return policy;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SecurityChallengePolicy updateChallengePolicy(SecurityChallengePolicy policy) {
        assertionChecker.checkOperation(policy.getId() != null, "SecurityPolicy.IdNotNull");
        validateChallengeTrigger(policy);
        policy.setUpdatedAt(DateUtil.now());
        challengeMapper.updateById(policy);
        publishChanged(com.ingot.cloud.security.api.event.SecurityPolicyDomain.CHALLENGE_POLICY);
        return policy;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChallengePolicy(Long id) {
        challengeMapper.deleteById(id);
        publishChanged(com.ingot.cloud.security.api.event.SecurityPolicyDomain.CHALLENGE_POLICY);
    }

    // ============== violation escalation ==============

    @Override
    public GatewayViolationEscalation getViolationEscalation() {
        GatewayViolationEscalation config = violationEscalationMapper.selectById(
                GatewayViolationEscalation.SINGLETON_ID);
        if (config == null) {
            config = defaultViolationEscalation();
        }
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GatewayViolationEscalation saveViolationEscalation(GatewayViolationEscalation config) {
        validateViolationEscalation(config);
        config.setId(GatewayViolationEscalation.SINGLETON_ID);
        LocalDateTime now = DateUtil.now();
        GatewayViolationEscalation existing = violationEscalationMapper.selectById(config.getId());
        if (existing == null) {
            config.setCreatedAt(now);
            config.setUpdatedAt(now);
            violationEscalationMapper.insert(config);
        } else {
            config.setCreatedAt(existing.getCreatedAt());
            config.setUpdatedAt(now);
            violationEscalationMapper.updateById(config);
        }
        publishChanged(SecurityPolicyDomain.VIOLATION_ESCALATION);
        return config;
    }

    // ============== snapshot ==============

    @Override
    public SecurityPolicySnapshot snapshot() {
        List<GatewayEndpointGroup> groups = listGroups();
        List<GatewayRateLimitRule> rules = listRules();
        List<GatewayIpList> ipList = listIpList();
        List<SecurityChallengePolicy> challengePolicies = listChallengePolicies();
        GatewayViolationEscalation violationEscalation = getViolationEscalation();
        long version = maxTimestamp(groups, rules, ipList, challengePolicies, violationEscalation);
        return new SecurityPolicySnapshot(groups, rules, ipList, challengePolicies, violationEscalation, version);
    }

    @Override
    public void broadcastInvalidationAll() {
        InvalidationBus bus = invalidationBusProvider.getIfAvailable();
        if (bus == null) {
            log.warn("[SecurityPolicy] InvalidationBus not available, cannot force broadcast");
            return;
        }
        bus.publish(SecurityPolicyInvalidationEvent.all());
        log.info("[SecurityPolicy] manual broadcast invalidation ALL");
    }

    private void publishChanged(SecurityPolicyDomain domain) {
        eventPublisher.publishEvent(new SecurityPolicyChangedSpringEvent(this, domain));
    }

    private static long maxTimestamp(List<GatewayEndpointGroup> groups,
                                     List<GatewayRateLimitRule> rules,
                                     List<GatewayIpList> ipList,
                                     List<SecurityChallengePolicy> challenges,
                                     GatewayViolationEscalation violationEscalation) {
        long max = 0L;
        for (GatewayEndpointGroup g : groups) {
            max = Math.max(max, toMillis(g.getUpdatedAt()));
        }
        for (GatewayRateLimitRule r : rules) {
            max = Math.max(max, toMillis(r.getUpdatedAt()));
        }
        for (GatewayIpList i : ipList) {
            max = Math.max(max, toMillis(i.getUpdatedAt()));
        }
        for (SecurityChallengePolicy c : challenges) {
            max = Math.max(max, toMillis(c.getUpdatedAt()));
        }
        if (violationEscalation != null) {
            max = Math.max(max, toMillis(violationEscalation.getUpdatedAt()));
        }
        return max;
    }

    private static GatewayViolationEscalation defaultViolationEscalation() {
        GatewayViolationEscalation config = new GatewayViolationEscalation();
        config.setId(GatewayViolationEscalation.SINGLETON_ID);
        config.setWindowSec(60);
        config.setBlockThreshold(30);
        config.setTempBlockTtlSec(900);
        config.setEnabled(true);
        return config;
    }

    private void validateViolationEscalation(GatewayViolationEscalation config) {
        assertionChecker.checkOperation(config != null, "SecurityPolicy.ViolationEscalationNotNull");
        assertionChecker.checkOperation(config.getWindowSec() != null && config.getWindowSec() >= 1,
                "SecurityPolicy.ViolationWindowInvalid");
        assertionChecker.checkOperation(config.getBlockThreshold() != null && config.getBlockThreshold() >= 1,
                "SecurityPolicy.ViolationThresholdInvalid");
        assertionChecker.checkOperation(config.getTempBlockTtlSec() != null && config.getTempBlockTtlSec() >= 60,
                "SecurityPolicy.ViolationTtlInvalid");
    }

    private static long toMillis(LocalDateTime t) {
        return t == null ? 0L : t.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private void validateChallengeTrigger(SecurityChallengePolicy policy) {
        String trigger = policy.getTrigger();
        assertionChecker.checkOperation(trigger != null, "SecurityPolicy.ChallengeTriggerNotNull");
        String normalized = trigger.trim().toLowerCase();
        assertionChecker.checkOperation(
                "always".equals(normalized) || "on_rate_limit".equals(normalized),
                "SecurityPolicy.ChallengeTriggerInvalid");
    }
}
