package com.ingot.framework.authorization;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import com.ingot.framework.commons.model.iam.FieldMergeMode;
import com.ingot.framework.commons.model.iam.extension.AuthorizationDecision;
import java.util.function.Function;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldProjection;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.extension.FieldPolicyDecision;
import com.ingot.framework.commons.model.iam.extension.ScopeCondition;
import com.ingot.framework.commons.model.iam.extension.ScopeTarget;

/**
 * <p>
 * 仅投影显式注册字段；业务身份字段使用专用DTO，不序列化原始实体。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class FieldPolicyProcessor {

    private static final FieldAccess HIDDEN = new FieldAccess(FieldVisibility.HIDDEN, false);

    private FieldPolicyProcessor() {
    }

    /**
     * 计算目标字段限制。
     * @param policy 同次字段策略
     * @param target 真实目标
     * @return 字段访问
     */
    public static Map<String, FieldAccess> access(FieldPolicyDecision policy, ScopeTarget target) {
        Map<String, FieldAccess> values = new LinkedHashMap<>();
        policy.defaults().forEach((key, baseline) -> {
            FieldAccess matched = null;
            for (var rule : policy.rules())
                if (key.equals(rule.fieldKey()) && ScopeRules.matches(rule.scope(), target))
                    matched = policy.mergeMode() == FieldMergeMode.GRANTS ? broader(matched, rule.access())
                            : stricter(matched, rule.access());
            values.put(key,
                    policy.mergeMode() == FieldMergeMode.GRANTS && !policy.ceilings().containsKey(key) ? HIDDEN
                            : stricter(matched == null
                                    ? (policy.mergeMode() == FieldMergeMode.GRANTS ? HIDDEN : baseline) : matched,
                                    policy.ceilings().get(key)));
        });
        return Map.copyOf(values);
    }

    /**
     * 查询或排序原值前检查整份查看者策略，不能用当前页样本证明全域可见。
     * @param policy 当前查看者匹配策略
     * @param fieldKey 已注册字段
     */
    public static void requireOriginalLookup(FieldPolicyDecision policy, String fieldKey) {
        if (!policy.filterableFields().contains(fieldKey))
            throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
        requireFullOriginal(policy, fieldKey);
    }

    /**
     * 原值排序同时检查注册/目录排序能力与当前策略。
     * @param policy 当前策略
     * @param fieldKey 注册字段
     */
    public static void requireOriginalSort(FieldPolicyDecision policy, String fieldKey) {
        if (!policy.sortableFields().contains(fieldKey))
            throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
        requireFullOriginal(policy, fieldKey);
    }

    private static void requireFullOriginal(FieldPolicyDecision policy, String fieldKey) {
        if (policy.mergeMode() == FieldMergeMode.GRANTS) {
            if (!policy.ceilings().containsKey(fieldKey))
                throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
            var candidates = policy.rules().stream().filter(r -> fieldKey.equals(r.fieldKey())).toList();
            var full = candidates.stream()
                .filter(r -> stricter(r.access(), policy.ceilings().get(fieldKey)).visibility() == FieldVisibility.FULL)
                .flatMap(r -> r.scope().stream())
                .toList();
            if (candidates.isEmpty() || candidates.stream()
                .flatMap(r -> r.scope().stream())
                .anyMatch(scope -> full.stream().noneMatch(upper -> covers(upper, scope))))
                throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
            return;
        }
        var baseline = stricter(policy.defaults().get(fieldKey), policy.ceilings().get(fieldKey));
        var matched = policy.rules().stream().filter(rule -> fieldKey.equals(rule.fieldKey())).toList();
        boolean fullOverride = matched.stream()
            .anyMatch(rule -> rule.scope().stream().anyMatch(ScopeCondition::all)
                    && stricter(rule.access(), policy.ceilings().get(fieldKey)).visibility() == FieldVisibility.FULL);
        if (baseline == null || baseline.visibility() != FieldVisibility.FULL && !fullOverride
                || matched.stream()
                    .anyMatch(rule -> stricter(rule.access(), policy.ceilings().get(fieldKey))
                        .visibility() != FieldVisibility.FULL)) {
            throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
        }
    }

    /**
     * 读取精确操作的字段结论，新模型缺失结论时拒绝执行。
     * @param decision 同次资源求值
     * @param actionCode 精确操作码
     * @return 操作字段策略
     */
    public static FieldPolicyDecision forAction(AuthorizationDecision decision, String actionCode) {
        var action = decision.actions().get(actionCode);
        if (action == null || action.fields() == null
                || decision.resource().domain() == com.ingot.framework.commons.model.iam.AuthorizationDomain.PLATFORM
                        && action.fields().mergeMode() != FieldMergeMode.GRANTS)
            throw new SdkAuthorizationException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        return action.fields();
    }

    /**
     * 合并同一操作和目标的有效字段授权。
     * @param a 已合并权限
     * @param b 本次贡献
     * @return 可见性并集，编辑仅由完整可见贡献
     */
    public static FieldAccess broader(FieldAccess a, FieldAccess b) {
        if (a == null)
            return b;
        if (b == null)
            return a;
        var visibility = a.visibility().ordinal() > b.visibility().ordinal() ? a.visibility() : b.visibility();
        return new FieldAccess(visibility,
                visibility == FieldVisibility.FULL && (a.visibility() == FieldVisibility.FULL && a.editable()
                        || b.visibility() == FieldVisibility.FULL && b.editable()));
    }

    private static boolean covers(ScopeCondition upper, ScopeCondition lower) {
        if (upper.all())
            return true;
        if (lower.all())
            return false;
        if (!upper.objectIds().isEmpty()
                && (lower.objectIds().isEmpty() || !upper.objectIds().containsAll(lower.objectIds())))
            return false;
        if (upper.ownerMemberId() != null && !upper.ownerMemberId().equals(lower.ownerMemberId()))
            return false;
        return upper.departmentSets()
            .stream()
            .allMatch(set -> lower.departmentSets().stream().anyMatch(restricted -> set.containsAll(restricted)));
    }

    /**
     * 合并收紧字段。
     * @param a 左侧
     * @param b 右侧
     * @return 更严格访问
     */
    public static FieldAccess stricter(FieldAccess a, FieldAccess b) {
        if (a == null)
            return b;
        if (b == null)
            return a;
        var visibility = a.visibility().ordinal() < b.visibility().ordinal() ? a.visibility() : b.visibility();
        return new FieldAccess(visibility, visibility == FieldVisibility.FULL && a.editable() && b.editable());
    }

    /**
     * 投影已注册字段，隐藏直接省略。
     * @param raw 原值
     * @param fields 字段访问
     * @param masks 后端脱敏器
     * @return 可输出值
     */
    public static Map<String, Object> project(Map<String, ?> raw, Map<String, FieldAccess> fields,
            Map<String, Function<Object, Object>> masks) {
        Map<String, Object> output = new LinkedHashMap<>();
        fields.forEach((key, access) -> {
            if (!raw.containsKey(key) || access.visibility() == FieldVisibility.HIDDEN)
                return;
            var value = raw.get(key);
            output.put(key, access.visibility() == FieldVisibility.MASKED && value != null
                    ? masks.getOrDefault(key, ignored -> FieldProjection.MASKED_PLACEHOLDER).apply(value) : value);
        });
        return Collections.unmodifiableMap(output);
    }

    /**
     * 校验显式提交字段，包括清空。
     * @param submitted 实际提交字段
     * @param fields 目标访问
     */
    public static void requireWritable(Map<String, ?> submitted, Map<String, FieldAccess> fields) {
        submitted.forEach((key, value) -> {
            var field = fields.get(key);
            if (field == null || field.visibility() != FieldVisibility.FULL || !field.editable())
                throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
            if (FieldProjection.MASKED_PLACEHOLDER.equals(value))
                throw new SdkAuthorizationException(IamReasonCode.INVALID_ARGUMENT);
        });
    }

}
