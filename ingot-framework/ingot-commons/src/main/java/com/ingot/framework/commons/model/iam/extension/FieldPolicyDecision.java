package com.ingot.framework.commons.model.iam.extension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldMergeMode;
import com.ingot.framework.commons.model.iam.FieldOperations;
import com.ingot.framework.commons.model.iam.MaskSpec;

/**
 * <p>
 * 无敏感原值的字段执行策略，业务服务按真实目标匹配。
 * </p>
 *
 * @param defaults 有效默认值
 * @param ceilings 字段能力上限
 * @param operations 当前身份对精确操作的全局字段能力
 * @param masks 当前资源版本的脱敏规则
 * @param rules 匹配当前查看者的规则
 * @param mergeMode 正向授权取并集，历史限制策略取交集
 * @param expiresAt 所有配置来源中最早的绝对到期时刻
 * @author jy
 * @since 1.0.0
 */
public record FieldPolicyDecision(Map<String, FieldAccess> defaults, Map<String, FieldAccess> ceilings,
        List<ResolvedFieldRule> rules, Map<String, FieldOperations> operations, Map<String, MaskSpec> masks,
        FieldMergeMode mergeMode, java.time.Instant expiresAt) {
    /** 直接从当前事实构造策略；缓存派生结果必须显式传入来源期限。 */
    public FieldPolicyDecision(Map<String, FieldAccess> defaults, Map<String, FieldAccess> ceilings,
            List<ResolvedFieldRule> rules, Map<String, FieldOperations> operations, Map<String, MaskSpec> masks,
            FieldMergeMode mergeMode) {
        this(defaults, ceilings, rules, operations, masks, mergeMode, FieldPolicyLifetime.deadline());
    }
    /**
     * 未声明操作能力时关闭筛选；保留 Java 构造的行级限制定义。
     */
    public FieldPolicyDecision(Map<String, FieldAccess> defaults, Map<String, FieldAccess> ceilings,
            List<ResolvedFieldRule> rules) {
        this(defaults, ceilings, rules, Map.of(), Map.of(), FieldMergeMode.RESTRICTIONS);
    }

    /**
     * 复制规则快照。
     */
    public FieldPolicyDecision {
        java.util.Objects.requireNonNull(expiresAt, "字段策略必须包含来源期限");
        mergeMode = mergeMode == null ? FieldMergeMode.RESTRICTIONS : mergeMode;
        defaults = Map.copyOf(defaults);
        ceilings = Map.copyOf(ceilings);
        rules = List.copyOf(rules);
        operations = Map.copyOf(operations);
        masks = Map.copyOf(masks);
    }

    /** 已授权筛选字段，原值查询仍必须通过整份范围可见性证明。 */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public Set<String> filterableFields() {
        return operations.entrySet().stream().filter(entry -> entry.getValue().filterable())
                .map(Map.Entry::getKey).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
