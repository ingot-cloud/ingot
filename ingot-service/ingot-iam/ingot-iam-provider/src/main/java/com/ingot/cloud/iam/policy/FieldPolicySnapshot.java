package com.ingot.cloud.iam.policy;

import java.util.List;
import java.util.Map;

import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.PolicyScenario;

/**
 * <p>一次请求内复用的字段策略快照，包含租户固定默认、平台上限与本场景规则。</p>
 *
 * @param tenantId 当前租户
 * @param scenario 后台或通讯录
 * @param baseline 租户引用的固定默认版本
 * @param ceiling 最新平台默认版本形成的最终上限
 * @param rules 已展开查看者选择器的本场景规则
 * @author jy
 * @since 1.0.0
 */
public record FieldPolicySnapshot(long tenantId, PolicyScenario scenario, Map<String, FieldAccess> baseline,
                                  Map<String, FieldAccess> ceiling, List<FieldAccessEvaluator.FieldRuleRow> rules) {
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
