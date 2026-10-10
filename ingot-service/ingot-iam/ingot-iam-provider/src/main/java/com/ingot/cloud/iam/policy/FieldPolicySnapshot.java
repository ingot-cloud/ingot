package com.ingot.cloud.iam.policy;

import java.util.List;
import java.util.Map;

import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.PolicyScenario;
import com.ingot.framework.commons.model.iam.FieldOperationRule;
import com.ingot.framework.commons.model.iam.FieldOperations;
import com.ingot.framework.commons.model.iam.MaskSpec;

/**
 * <p>一次请求内复用的字段策略快照，包含租户固定默认、平台上限与本场景规则。</p>
 *
 * @param tenantId 当前租户
 * @param scenario 后台或通讯录
 * @param baseline 租户引用的固定默认版本
 * @param ceiling 最新平台默认版本形成的最终上限
 * @param rules 已展开查看者选择器的本场景规则
 * @param operationRules 不按目标行匹配的操作规则
 * @param operationDefaults 固定默认操作能力
 * @param operationCeiling 平台操作能力上限
 * @param masks 当前资源脱敏规则
 * @param expiresAt 来源配置的最早绝对到期时刻
 * @author jy
 * @since 1.0.0
 */
public record FieldPolicySnapshot(long tenantId, PolicyScenario scenario, Map<String, FieldAccess> baseline,
                                  Map<String, FieldAccess> ceiling, List<FieldAccessEvaluator.FieldRuleRow> rules,
                                  List<FieldOperationRule> operationRules, Map<String, FieldOperations> operationDefaults,
                                  Map<String, FieldOperations> operationCeiling, Map<String, MaskSpec> masks,
                                  java.time.Instant expiresAt) {
    /** 从本次当前事实构造；缓存派生结果使用显式来源期限的构造器。 */
    public FieldPolicySnapshot(long tenantId, PolicyScenario scenario, Map<String, FieldAccess> baseline,
            Map<String, FieldAccess> ceiling, List<FieldAccessEvaluator.FieldRuleRow> rules,
            List<FieldOperationRule> operationRules, Map<String, FieldOperations> operationDefaults,
            Map<String, FieldOperations> operationCeiling, Map<String, MaskSpec> masks) {
        this(tenantId, scenario, baseline, ceiling, rules, operationRules, operationDefaults, operationCeiling, masks,
                com.ingot.framework.commons.model.iam.extension.FieldPolicyLifetime.deadline());
    }
    /** 复制请求配置快照；不保存目标原值。 */
    public FieldPolicySnapshot {
        java.util.Objects.requireNonNull(expiresAt);
        baseline = Map.copyOf(baseline);
        ceiling = Map.copyOf(ceiling);
        rules = List.copyOf(rules);
        operationRules = List.copyOf(operationRules);
        operationDefaults = Map.copyOf(operationDefaults);
        operationCeiling = Map.copyOf(operationCeiling);
        masks = Map.copyOf(masks);
    }
    /**
     * 读取字段基线；缺键时使用文档化默认。
     *
     * @param fieldKey 字段键
     * @return 基线访问
     */
    public FieldAccess baselineOf(String fieldKey) {
        FieldAccess access = baseline == null ? null : baseline.get(fieldKey);
        return access == null ? DefaultPolicyDefinitions.documented(fieldKey) : access;
    }

    /**
     * 读取平台上限；缺键时使用文档化默认。
     *
     * @param fieldKey 字段键
     * @return 上限访问
     */
    public FieldAccess ceilingOf(String fieldKey) {
        FieldAccess access = ceiling == null ? null : ceiling.get(fieldKey);
        return access == null ? DefaultPolicyDefinitions.openCeiling(fieldKey) : access;
    }
}
