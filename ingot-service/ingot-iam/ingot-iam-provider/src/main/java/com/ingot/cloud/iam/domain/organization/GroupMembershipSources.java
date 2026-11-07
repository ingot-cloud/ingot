package com.ingot.cloud.iam.domain.organization;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ingot.cloud.iam.domain.group.GroupMembership;
import com.ingot.cloud.iam.domain.group.GroupMembershipDepartment;

/**
 * <p>按当前页成员解析直接绑定与部门规则来源，不查询数据库。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class GroupMembershipSources {
    private GroupMembershipSources() {
    }

    /**
     * 已展开的组部门规则。
     *
     * @param departmentId 规则部门
     * @param includeDescendants 是否连带下级
     * @param reachable 规则覆盖的部门
     */
    public record Rule(BigInteger departmentId, boolean includeDescendants, Set<BigInteger> reachable) {
    }

    /**
     * 为当前页生成来源；部门名称缺失时省略，由前端显示「部门范围」。
     *
     * @param memberIds 当前页成员，保持原顺序
     * @param directIds 其中的显式绑定
     * @param rules 已展开的部门规则
     * @param memberDepartments 成员真实任职
     * @param visibleDepartmentNames 操作者可见的规则部门名称
     * @return 成员到来源
     */
    public static Map<BigInteger, GroupMembership> resolve(Collection<BigInteger> memberIds, Set<BigInteger> directIds,
            List<Rule> rules, Map<BigInteger, ? extends Collection<BigInteger>> memberDepartments,
            Map<BigInteger, String> visibleDepartmentNames) {
        Map<BigInteger, GroupMembership> result = new LinkedHashMap<>();
        List<Rule> ordered = rules.stream()
                .sorted(Comparator.comparing(Rule::departmentId))
                .toList();
        for (BigInteger memberId : memberIds) {
            Collection<BigInteger> appointments = memberDepartments.get(memberId);
            if (appointments == null) {
                appointments = List.of();
            }
            List<GroupMembershipDepartment> matched = new ArrayList<>();
            for (Rule rule : ordered) {
                if (appointments.stream().noneMatch(rule.reachable()::contains)) {
                    continue;
                }
                matched.add(new GroupMembershipDepartment(rule.departmentId().toString(),
                        visibleDepartmentNames.get(rule.departmentId()), rule.includeDescendants()));
            }
            result.put(memberId, new GroupMembership(directIds.contains(memberId), matched));
        }
        return result;
    }
}
