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

/**
 * 把 ingot-security-api 的 VO 转换成 SDK 内部模型。
 *
 * @author jy
 * @since 2026/5/26
 */
public final class SnapshotAssembler {

    private SnapshotAssembler() {
    }

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
                .dryRun(v.isDryRun())
                .priority(v.getPriority())
                .remark(v.getRemark())
                .build();
    }

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
        IpListItem item = new IpListItem();
        item.setId(v.getId());
        item.setListType(IpListType.fromCode(v.getListType()));
        item.setKeyType(IpKeyType.fromCode(v.getKeyType()));
        item.setKeyValue(v.getKeyValue());
        item.setReason(v.getReason());
        item.setSource(v.getSource());
        item.setEffectiveAt(v.getEffectiveAt());
        item.setExpiresAt(v.getExpiresAt());
        item.setEnabled(v.isEnabled());
        item.setOperatorId(v.getOperatorId());
        item.setOperatorName(v.getOperatorName());
        return item;
    }

    private static List<EndpointPattern> toPatternList(List<EndpointPatternVO> raw) {
        if (raw == null) return Collections.emptyList();
        return raw.stream()
                .map(p -> EndpointPattern.of(p.getPath(), p.getMethod()))
                .toList();
    }
}
