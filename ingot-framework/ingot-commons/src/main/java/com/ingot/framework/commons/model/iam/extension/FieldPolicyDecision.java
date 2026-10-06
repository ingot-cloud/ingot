package com.ingot.framework.commons.model.iam.extension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldMergeMode;

/**
 * <p>
 * 无敏感原值的字段执行策略，业务服务按真实目标匹配。
 * </p>
 *
 * @param defaults 有效默认值
 * @param ceilings 字段能力上限
 * @param filterableFields 可使用原值筛选的字段能力上限
 * @param sortableFields 可使用原值排序的字段能力上限
 * @param rules 匹配当前查看者的规则
 * @param mergeMode 正向授权取并集，历史限制策略取交集
 * @author jy
 * @since 1.0.0
 */
public record FieldPolicyDecision(Map<String, FieldAccess> defaults, Map<String, FieldAccess> ceilings,
        List<ResolvedFieldRule> rules, Set<String> filterableFields, Set<String> sortableFields,
        FieldMergeMode mergeMode) {
    /** 兼容旧限制型字段策略。 */
    public FieldPolicyDecision(Map<String, FieldAccess> defaults, Map<String, FieldAccess> ceilings,
            List<ResolvedFieldRule> rules, Set<String> filterableFields, Set<String> sortableFields) {
        this(defaults, ceilings, rules, filterableFields, sortableFields, FieldMergeMode.RESTRICTIONS);
    }

    /**
     * 未声明筛选及排序能力时拒绝原值查询。
     */
    public FieldPolicyDecision(Map<String, FieldAccess> defaults, Map<String, FieldAccess> ceilings,
            List<ResolvedFieldRule> rules) {
        this(defaults, ceilings, rules, Set.of(), Set.of());
    }

    /**
     * 复制规则快照。
     */
    public FieldPolicyDecision {
        mergeMode = mergeMode == null ? FieldMergeMode.RESTRICTIONS : mergeMode;
        defaults = Map.copyOf(defaults);
        ceilings = Map.copyOf(ceilings);
        rules = List.copyOf(rules);
        filterableFields = Set.copyOf(filterableFields);
        sortableFields = Set.copyOf(sortableFields);
    }
}
