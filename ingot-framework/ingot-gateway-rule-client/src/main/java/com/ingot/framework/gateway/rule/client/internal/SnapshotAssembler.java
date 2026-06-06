package com.ingot.framework.gateway.rule.client.internal;

import com.ingot.cloud.security.api.model.vo.policy.EndpointGroupVO;
import com.ingot.cloud.security.api.model.vo.policy.EndpointPatternVO;
import com.ingot.cloud.security.api.model.vo.policy.IpListItemVO;
import com.ingot.cloud.security.api.model.vo.policy.RateLimitRuleVO;
import com.ingot.cloud.security.api.model.vo.policy.SecurityPolicySnapshotVO;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpKeyType;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpListItem;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpListSnapshot;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpListType;
import com.ingot.framework.gateway.rule.client.model.EndpointPattern;
import com.ingot.framework.gateway.rule.client.ratelimit.model.EndpointGroup;
import com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitDimension;
import com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitRule;
import com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitSnapshot;

import java.util.Collections;
import java.util.List;

import lombok.experimental.UtilityClass;

/**
 * 远端快照 VO → SDK 内部模型的转换器。
 *
 * <p>仅在 {@code policy.mode=remote} 时由 {@link RemoteSnapshotFetcher} 拉取全量快照后调用；
 * 负责把 ingot-security-api 的 {@link SecurityPolicySnapshotVO} 拆分为各域可用的强类型模型，
 * 并统一解析 DB 短码（维度、名单类型、键类型等）。</p>
 *
 * <p>本类无状态、线程安全，所有方法均为静态。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@UtilityClass
public class SnapshotAssembler {

    /**
     * 从全量快照中提取限流域数据。
     *
     * @param vo 远端快照；为 null 时返回 {@link RateLimitSnapshot#empty()}
     */
    public static RateLimitSnapshot toRateLimitSnapshot(SecurityPolicySnapshotVO vo) {
        if (vo == null) {
            return RateLimitSnapshot.empty();
        }
        List<RateLimitRule> rules = vo.getRateLimitRules() == null ? Collections.emptyList()
                : vo.getRateLimitRules().stream().map(SnapshotAssembler::toRule).toList();
        List<EndpointGroup> groups = vo.getGroups() == null ? Collections.emptyList()
                : vo.getGroups().stream().map(SnapshotAssembler::toGroup).toList();
        return new RateLimitSnapshot(rules, groups, vo.getVersion());
    }

    /**
     * 从全量快照中提取黑白名单域数据。
     *
     * @param vo 远端快照；为 null 或 {@code ipList} 为空时返回 {@link IpListSnapshot#empty()}
     */
    public static IpListSnapshot toIpListSnapshot(SecurityPolicySnapshotVO vo) {
        if (vo == null || vo.getIpList() == null) {
            return IpListSnapshot.empty();
        }
        List<IpListItem> items = vo.getIpList().stream().map(SnapshotAssembler::toIpListItem).toList();
        return new IpListSnapshot(items, vo.getVersion());
    }

    private static RateLimitRule toRule(RateLimitRuleVO v) {
        return RateLimitRule.builder()
                .id(v.getId())
                .code(v.getCode())
                .groupCode(v.getGroupCode())
                .patternList(toPatternList(v.getPatternList()))
                .dimension(RateLimitDimension.fromCode(v.getDimension()))
                .qps(v.getQps())
                .burst(v.getBurst())
                .intervalSec(v.getIntervalSec())
                .controlBehavior(v.getControlBehavior())
                .enabled(v.isEnabled())
                .priority(v.getPriority())
                .remark(v.getRemark())
                .build();
    }

    /**
     * 转换 API 路径分组 VO（限流 / 挑战域共用）。
     */
    public static EndpointGroup toGroup(EndpointGroupVO v) {
        return EndpointGroup.builder()
                .id(v.getId())
                .code(v.getCode())
                .name(v.getName())
                .patternList(toPatternList(v.getPatternList()))
                .enabled(v.isEnabled())
                .remark(v.getRemark())
                .build();
    }

    private static IpListItem toIpListItem(IpListItemVO v) {
        return IpListItem.builder()
                .id(v.getId())
                .listType(IpListType.fromCode(v.getListType()))
                .keyType(IpKeyType.fromCode(v.getKeyType()))
                .keyValue(v.getKeyValue())
                .reason(v.getReason())
                .source(v.getSource())
                .effectiveAt(v.getEffectiveAt())
                .expiresAt(v.getExpiresAt())
                .enabled(v.isEnabled())
                .operatorId(v.getOperatorId())
                .operatorName(v.getOperatorName())
                .build();
    }

    private static List<EndpointPattern> toPatternList(List<EndpointPatternVO> raw) {
        if (raw == null) return Collections.emptyList();
        return raw.stream()
                .map(p -> EndpointPattern.of(p.getPath(), p.getMethod()))
                .toList();
    }
}
