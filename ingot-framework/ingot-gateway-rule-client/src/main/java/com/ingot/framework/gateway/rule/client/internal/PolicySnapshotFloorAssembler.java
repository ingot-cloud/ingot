package com.ingot.framework.gateway.rule.client.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ingot.cloud.security.api.model.vo.policy.ChallengePolicyVO;
import com.ingot.cloud.security.api.model.vo.policy.EndpointGroupVO;
import com.ingot.cloud.security.api.model.vo.policy.EndpointPatternVO;
import com.ingot.cloud.security.api.model.vo.policy.IpListItemVO;
import com.ingot.cloud.security.api.model.vo.policy.RateLimitRuleVO;
import com.ingot.cloud.security.api.model.vo.policy.SecurityPolicySnapshotVO;
import com.ingot.cloud.security.api.model.vo.policy.ViolationEscalationVO;
import com.ingot.framework.gateway.rule.client.blacklist.config.BlacklistProperties;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpListItem;
import com.ingot.framework.gateway.rule.client.challenge.config.ChallengeProperties;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.gateway.rule.client.model.EndpointPattern;
import com.ingot.framework.gateway.rule.client.ratelimit.config.RateLimitProperties;
import com.ingot.framework.gateway.rule.client.ratelimit.model.EndpointGroup;
import com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitRule;
import com.ingot.framework.gateway.rule.client.violation.config.ViolationEscalationProperties;
import com.ingot.framework.gateway.rule.client.violation.model.ViolationEscalationConfig;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>将各域 local {@code *Properties} 映射为 {@link SecurityPolicySnapshotVO}（Nacos 地板）。</p>
 *
 * <p>每个域<b>独立贡献</b>自己的片段：入参为 {@code null} 表示该域未启用
 * （{@code ingot.security.<domain>.enabled=false} 时其 Properties Bean 不装配），
 * 此时不贡献任何内容也不补基线，因此地板内容与域开关始终一致。</p>
 *
 * <p>最低安全基线（REQUIREMENTS 业务规则 8）按域条件生效：仅当某域<b>已启用但本地无配置</b>时
 * 才补该域基线，例如 ratelimit 域启用却未配置任何规则时补登录入口 IP 限流。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see LocalPolicyFloorSupplier
 * @implNote {@link SecurityPolicySnapshotVO#getGroups()} 为 ratelimit 与 challenge 共用的一份
 *           分组列表（{@code CompiledChallengePolicy} 经 {@code groupResolver} 按 {@code groupCode} 查找），
 *           故 challenge 的分组按 {@code code} 去重合并，同名时保留 ratelimit 的定义。
 */
@UtilityClass
@Slf4j
public class PolicySnapshotFloorAssembler {

    /**
     * 组装地板快照。任一入参为 {@code null} 表示该域未启用，其片段留空。
     */
    public static SecurityPolicySnapshotVO fromLocal(RateLimitProperties rateLimit,
                                                     BlacklistProperties blacklist,
                                                     ChallengeProperties challenge,
                                                     ViolationEscalationProperties violation) {
        List<EndpointGroupVO> groups = new ArrayList<>();
        List<RateLimitRuleVO> rules = new ArrayList<>();
        contributeRateLimit(rateLimit, groups, rules);

        SecurityPolicySnapshotVO vo = new SecurityPolicySnapshotVO();
        vo.setVersion(0L);
        vo.setChallengePolicies(contributeChallenge(challenge, groups));
        vo.setGroups(groups);
        vo.setRateLimitRules(rules);
        vo.setIpList(toIpList(blacklist));
        vo.setViolationEscalation(toViolation(violation));
        return vo;
    }

    /**
     * 限流域片段。域启用但无本地规则时补登录入口 IP 限流基线，避免地板形同 fail-open。
     */
    private static void contributeRateLimit(RateLimitProperties rateLimit,
                                            List<EndpointGroupVO> groups,
                                            List<RateLimitRuleVO> rules) {
        if (rateLimit == null) {
            return;
        }
        groups.addAll(toGroups(rateLimit));
        rules.addAll(toRules(rateLimit));
        if (rules.isEmpty()) {
            log.warn("[SecurityPolicy] ratelimit floor has no local rule, applying login baseline");
            mergeGroup(groups, loginBaselineGroup());
            rules.add(loginBaselineRule());
        }
    }

    /**
     * 挑战域片段。分组合并进共享的 {@code groups}，按 {@code code} 去重。
     */
    private static List<ChallengePolicyVO> contributeChallenge(ChallengeProperties challenge,
                                                              List<EndpointGroupVO> groups) {
        if (challenge == null) {
            return Collections.emptyList();
        }
        for (EndpointGroup group : challenge.getPolicy().getGroups()) {
            mergeGroup(groups, toGroupVO(group));
        }
        List<ChallengePolicyVO> list = new ArrayList<>();
        for (ChallengePolicy policy : challenge.getPolicy().getPolicies()) {
            ChallengePolicyVO vo = new ChallengePolicyVO();
            vo.setId(policy.getId());
            vo.setCode(policy.getCode());
            vo.setGroupCode(policy.getGroupCode());
            vo.setPatternList(toPatterns(policy.getPatternList()));
            vo.setTrigger(policy.getTrigger() != null ? policy.getTrigger().name() : null);
            vo.setChallengeType(policy.getChallengeType());
            vo.setPassTokenTtlSec(policy.getPassTokenTtlSec());
            vo.setPassTokenRemaining(policy.getPassTokenRemaining());
            vo.setScope(policy.getScope());
            vo.setEnabled(policy.isEnabled());
            vo.setPriority(policy.getPriority());
            list.add(vo);
        }
        return list;
    }

    /**
     * 按 {@code code} 去重后追加分组；已存在同名 code 时保留先加入者。
     */
    private static void mergeGroup(List<EndpointGroupVO> groups, EndpointGroupVO candidate) {
        if (candidate == null || candidate.getCode() == null) {
            return;
        }
        boolean exists = groups.stream().anyMatch(g -> candidate.getCode().equals(g.getCode()));
        if (!exists) {
            groups.add(candidate);
        }
    }

    /** 登录入口分组基线：仅在限流域启用却无本地规则时使用。 */
    private static EndpointGroupVO loginBaselineGroup() {
        EndpointGroupVO group = new EndpointGroupVO();
        group.setCode("login-auth-floor");
        group.setName("登录入口（地板）");
        group.setEnabled(true);
        EndpointPatternVO pattern = new EndpointPatternVO();
        // 登录主入口是 BFF；Auth 的 /auth/token/** 已随 TokenEndpoint 摘除，护住它没有意义
        pattern.setPath("/bff/auth/login/**");
        pattern.setMethod("ANY");
        group.setPatternList(List.of(pattern));
        return group;
    }

    /** 登录入口 IP 限流基线规则。 */
    private static RateLimitRuleVO loginBaselineRule() {
        RateLimitRuleVO rule = new RateLimitRuleVO();
        rule.setCode("login-ip-floor");
        rule.setGroupCode("login-auth-floor");
        rule.setDimension("IP");
        rule.setQps(1);
        rule.setBurst(2);
        rule.setIntervalSec(60);
        rule.setControlBehavior("F");
        rule.setEnabled(true);
        rule.setPriority(0);
        return rule;
    }

    private static List<EndpointGroupVO> toGroups(RateLimitProperties rateLimit) {
        if (rateLimit.getPolicy().getGroups().isEmpty()) {
            return Collections.emptyList();
        }
        List<EndpointGroupVO> list = new ArrayList<>();
        for (EndpointGroup g : rateLimit.getPolicy().getGroups()) {
            list.add(toGroupVO(g));
        }
        return list;
    }

    private static EndpointGroupVO toGroupVO(EndpointGroup g) {
        EndpointGroupVO vo = new EndpointGroupVO();
        vo.setCode(g.getCode());
        vo.setName(g.getName());
        vo.setEnabled(g.isEnabled());
        vo.setRemark(g.getRemark());
        vo.setPatternList(toPatterns(g.getPatternList()));
        return vo;
    }

    private static List<RateLimitRuleVO> toRules(RateLimitProperties rateLimit) {
        if (rateLimit.getPolicy().getRules().isEmpty()) {
            return Collections.emptyList();
        }
        List<RateLimitRuleVO> list = new ArrayList<>();
        for (RateLimitRule r : rateLimit.getPolicy().getRules()) {
            RateLimitRuleVO vo = new RateLimitRuleVO();
            vo.setCode(r.getCode());
            vo.setGroupCode(r.getGroupCode());
            vo.setPatternList(toPatterns(r.getPatternList()));
            vo.setDimension(r.getDimension() != null ? r.getDimension().dbCode() : "IP");
            vo.setQps(r.getQps());
            vo.setBurst(r.getBurst());
            vo.setIntervalSec(r.getIntervalSec());
            vo.setControlBehavior(r.getControlBehavior());
            vo.setEnabled(r.isEnabled());
            vo.setPriority(r.getPriority());
            vo.setRemark(r.getRemark());
            list.add(vo);
        }
        return list;
    }

    private static List<IpListItemVO> toIpList(BlacklistProperties blacklist) {
        if (blacklist == null || blacklist.getPolicy().getItems().isEmpty()) {
            return Collections.emptyList();
        }
        List<IpListItemVO> list = new ArrayList<>();
        for (IpListItem item : blacklist.getPolicy().getItems()) {
            IpListItemVO vo = new IpListItemVO();
            vo.setListType(item.getListType() != null ? item.getListType().dbCode() : "B");
            vo.setKeyType(item.getKeyType() != null ? item.getKeyType().dbCode() : "IP");
            vo.setKeyValue(item.getKeyValue());
            vo.setReason(item.getReason());
            vo.setSource(item.getSource());
            vo.setEffectiveAt(item.getEffectiveAt());
            vo.setExpiresAt(item.getExpiresAt());
            vo.setEnabled(item.isEnabled());
            list.add(vo);
        }
        return list;
    }

    /**
     * 违规升级域片段；域未启用时返回 {@code null}，消费侧
     * {@code SnapshotAssembler.toViolationEscalationConfig} 会回落
     * {@link ViolationEscalationConfig#defaults()}。
     */
    private static ViolationEscalationVO toViolation(ViolationEscalationProperties violation) {
        if (violation == null) {
            return null;
        }
        ViolationEscalationProperties.Policy p = violation.getPolicy();
        ViolationEscalationVO vo = new ViolationEscalationVO();
        vo.setWindowSec(Math.max(1, p.getWindowSec()));
        vo.setBlockThreshold(Math.max(1, p.getBlockThreshold()));
        vo.setTempBlockTtlSec(Math.max(60, p.getTempBlockTtlSec()));
        vo.setEnabled(p.isEnabled());
        return vo;
    }

    private static List<EndpointPatternVO> toPatterns(List<EndpointPattern> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return Collections.emptyList();
        }
        return patterns.stream().map(p -> {
            EndpointPatternVO vo = new EndpointPatternVO();
            vo.setPath(p.getPath());
            vo.setMethod(p.getMethod());
            return vo;
        }).toList();
    }
}
