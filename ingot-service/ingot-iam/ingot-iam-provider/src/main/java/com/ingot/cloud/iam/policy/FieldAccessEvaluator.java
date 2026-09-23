package com.ingot.cloud.iam.policy;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.evaluation.DepartmentClosure;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ObjectScopeClause;
import com.ingot.cloud.iam.evaluation.ScopeBinder;
import com.ingot.cloud.iam.evaluation.ScopeClause;
import com.ingot.cloud.iam.persistence.entity.IamDefaultPolicyRevisionEntity;
import com.ingot.cloud.iam.persistence.entity.IamFieldPolicyEntity;
import com.ingot.cloud.iam.persistence.entity.IamFieldRuleEntity;
import com.ingot.cloud.iam.persistence.entity.IamMemberDepartmentEntity;
import com.ingot.cloud.iam.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.DefaultPolicyKind;
import com.ingot.framework.commons.model.iam.DepartmentSelection;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldPolicyDraft;
import com.ingot.framework.commons.model.iam.FieldProjection;
import com.ingot.framework.commons.model.iam.FieldRule;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberFieldKey;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.PolicyScenario;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.ScopeKind;
import com.ingot.framework.commons.model.iam.Selection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>按场景、查看者与目标合并字段策略，投影响应并拒绝不可编辑或脱敏占位写入。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class FieldAccessEvaluator {
    private static final TypeReference<List<ScopeExpression>> SCOPES = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, ScopeBinding>> BINDINGS = new TypeReference<>() {
    };
    private final PolicyWriteRepository policies;
    private final IamMemberDepartmentMapper memberships;
    private final DepartmentClosure closures;

    /**
     * 加载当前租户在指定场景下的默认版本、平台上限与规则，供一次请求复用。
     *
     * @param tenantId 当前租户
     * @param scenario 后台或通讯录
     * @return 字段策略快照
     */
    public FieldPolicySnapshot snapshot(long tenantId, PolicyScenario scenario) {
        IamFieldPolicyEntity policy = policies.findField(tenantId);
        IamDefaultPolicyRevisionEntity latest = policies.latestRevision(DefaultPolicyKind.FIELD);
        IamDefaultPolicyRevisionEntity baseline = policy == null
                ? latest : policies.findRevision(policy.getDefaultRevisionId().longValueExact(), DefaultPolicyKind.FIELD);
        if (baseline == null) {
            baseline = latest;
        }
        List<FieldRuleRow> rows = new ArrayList<>();
        List<IamFieldRuleEntity> items = policies.listFieldRules(tenantId);
        LinkedHashSet<Long> selectorIds = new LinkedHashSet<>();
        for (IamFieldRuleEntity item : items) {
            if (item.getScenario() == scenario) {
                selectorIds.add(item.getViewerSelectorId().longValueExact());
            }
        }
        Map<Long, Selection> selectors = policies.selectors(tenantId, selectorIds);
        for (IamFieldRuleEntity item : items) {
            if (item.getScenario() != scenario) {
                continue;
            }
            rows.add(new FieldRuleRow(
                    selectors.getOrDefault(item.getViewerSelectorId().longValueExact(),
                            new Selection(List.of(), List.of())),
                    IamJson.read(item.getTargetScope(), SCOPES),
                    IamJson.read(item.getScopeBindings(), BINDINGS),
                    item.getFieldKey(), item.getVisibility(), Boolean.TRUE.equals(item.getEditable())));
        }
        return new FieldPolicySnapshot(tenantId, scenario,
                DefaultPolicyDefinitions.fieldAccess(baseline == null ? null : baseline.getDefinition()),
                DefaultPolicyDefinitions.fieldCeiling(latest == null ? null : latest.getDefinition()),
                List.copyOf(rows));
    }

    /**
     * 按未保存草稿组装字段策略快照，与提交后求值使用同一合并规则。
     *
     * @param tenantId 当前租户
     * @param scenario 后台或通讯录
     * @param draft 字段草稿
     * @return 字段策略快照
     */
    public FieldPolicySnapshot snapshot(long tenantId, PolicyScenario scenario, FieldPolicyDraft draft) {
        IamDefaultPolicyRevisionEntity latest = policies.latestRevision(DefaultPolicyKind.FIELD);
        IamDefaultPolicyRevisionEntity baseline = policies.findRevision(
                IamIds.require(draft.defaultRevisionId()), DefaultPolicyKind.FIELD);
        if (baseline == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        List<FieldRuleRow> rows = new ArrayList<>();
        List<FieldRule> rules = draft.rules() == null ? List.of() : draft.rules();
        for (FieldRule rule : rules) {
            if (rule.scenario() != scenario) {
                continue;
            }
            rows.add(new FieldRuleRow(rule.viewerSelection(), rule.targetScope(), rule.scopeBindings(),
                    rule.fieldKey(), rule.visibility(), rule.editable()));
        }
        return new FieldPolicySnapshot(tenantId, scenario,
                DefaultPolicyDefinitions.fieldAccess(baseline.getDefinition()),
                DefaultPolicyDefinitions.fieldCeiling(latest == null ? null : latest.getDefinition()),
                List.copyOf(rows));
    }

    /**
     * 计算单一字段的可见与编辑上限。
     *
     * @param tenantId 当前租户
     * @param viewerId 查看者成员
     * @param targetMemberId 目标成员
     * @param scenario 后台或通讯录
     * @param fieldKey 字段键
     * @return 合并后的访问结果
     */
    public FieldAccess access(long tenantId, long viewerId, long targetMemberId, PolicyScenario scenario,
                              String fieldKey) {
        return access(snapshot(tenantId, scenario), viewerId, targetMemberId, fieldKey);
    }

    /**
     * 在已加载快照上计算单一字段访问。
     *
     * @param snapshot 字段策略快照
     * @param viewerId 查看者成员
     * @param targetMemberId 目标成员
     * @param fieldKey 字段键
     * @return 合并并受平台上限约束的访问
     */
    public FieldAccess access(FieldPolicySnapshot snapshot, long viewerId, long targetMemberId, String fieldKey) {
        FieldAccess matched = null;
        for (FieldRuleRow rule : snapshot.rules()) {
            if (!fieldKey.equals(rule.fieldKey())
                    || !matchesViewer(snapshot.tenantId(), viewerId, rule.viewer())
                    || !matchesTarget(snapshot.tenantId(), viewerId, targetMemberId, rule.scopes(), rule.bindings())) {
                continue;
            }
            matched = DefaultPolicyDefinitions.stricter(matched,
                    new FieldAccess(rule.visibility(), rule.editable()));
        }
        FieldAccess resolved = matched == null ? snapshot.baselineOf(fieldKey) : matched;
        return DefaultPolicyDefinitions.stricter(resolved, snapshot.ceilingOf(fieldKey));
    }

    /**
     * 计算成员资料全部字段访问说明。
     *
     * @param tenantId 当前租户
     * @param viewerId 查看者成员
     * @param targetMemberId 目标成员
     * @param scenario 后台或通讯录
     * @return 按字段键索引的访问说明
     */
    public Map<String, FieldAccess> memberAccess(long tenantId, long viewerId, long targetMemberId,
                                                 PolicyScenario scenario) {
        return memberAccess(snapshot(tenantId, scenario), viewerId, targetMemberId);
    }

    /**
     * 在已加载快照上计算成员资料全部字段访问。
     *
     * @param snapshot 字段策略快照
     * @param viewerId 查看者成员
     * @param targetMemberId 目标成员
     * @return 按字段键索引的访问说明
     */
    public Map<String, FieldAccess> memberAccess(FieldPolicySnapshot snapshot, long viewerId, long targetMemberId) {
        Map<String, FieldAccess> access = new LinkedHashMap<>();
        for (MemberFieldKey field : MemberFieldKey.values()) {
            access.put(field.getValue(), access(snapshot, viewerId, targetMemberId, field.getValue()));
        }
        return access;
    }

    /**
     * 按访问说明投影成员资料，隐藏字段省略。
     *
     * @param raw 含存储原值的记录
     * @param access 字段访问
     * @return 可输出记录
     */
    public MemberRecord project(MemberRecord raw, Map<String, FieldAccess> access) {
        FieldAccess displayName = access.getOrDefault(MemberFieldKey.VALUE_DISPLAY_NAME,
                DefaultPolicyDefinitions.documented(MemberFieldKey.VALUE_DISPLAY_NAME));
        FieldAccess avatar = access.getOrDefault(MemberFieldKey.VALUE_AVATAR,
                DefaultPolicyDefinitions.documented(MemberFieldKey.VALUE_AVATAR));
        FieldAccess phone = access.getOrDefault(MemberFieldKey.VALUE_PHONE,
                DefaultPolicyDefinitions.documented(MemberFieldKey.VALUE_PHONE));
        FieldAccess email = access.getOrDefault(MemberFieldKey.VALUE_EMAIL,
                DefaultPolicyDefinitions.documented(MemberFieldKey.VALUE_EMAIL));
        return new MemberRecord(raw.id(),
                FieldProjection.project(raw.displayName(), displayName.visibility()),
                FieldProjection.project(raw.avatar(), avatar.visibility()),
                FieldProjection.project(raw.phone(), phone.visibility()),
                FieldProjection.project(raw.email(), email.visibility()),
                raw.username(),
                raw.status(), raw.departments());
    }

    /**
     * 拒绝提交不可编辑字段或脱敏占位符，避免误报成功。
     *
     * @param tenantId 当前租户
     * @param viewerId 操作者
     * @param targetMemberId 目标成员
     * @param fieldKey 字段键
     * @param submitted 提交值；空引用表示不修改
     */
    public void requireWritable(long tenantId, long viewerId, long targetMemberId, String fieldKey, String submitted) {
        if (submitted == null) {
            return;
        }
        if (FieldProjection.maskedPlaceholder(submitted)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        FieldAccess access = access(tenantId, viewerId, targetMemberId, PolicyScenario.MANAGEMENT, fieldKey);
        if (!access.editable() || access.visibility() != FieldVisibility.FULL) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
    }

    /**
     * 未对查询范围内全部可能匹配目标完整可见时，禁止按原值筛选。
     *
     * @param tenantId 当前租户
     * @param viewerId 操作者
     * @param fieldKey 字段键
     * @param submitted 筛选原值
     */
    public void requireOriginalLookup(long tenantId, long viewerId, String fieldKey, String submitted) {
        requireOriginalLookup(snapshot(tenantId, PolicyScenario.MANAGEMENT), viewerId, fieldKey, submitted,
                ObjectScope.all());
    }

    /**
     * 按对象范围内可能匹配的目标执行字段披露边界，不能从隐藏值筛选、排序或计数推断。
     *
     * @param snapshot 字段策略快照
     * @param viewerId 操作者
     * @param fieldKey 字段键
     * @param submitted 筛选原值
     * @param scope 查询对象范围
     */
    public void requireOriginalLookup(FieldPolicySnapshot snapshot, long viewerId, String fieldKey, String submitted,
                                      ObjectScope scope) {
        if (submitted == null || submitted.isBlank() || scope == null || scope.coversNone()) {
            return;
        }
        if (!universallyFull(snapshot, viewerId, fieldKey, scope)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
    }

    private boolean universallyFull(FieldPolicySnapshot snapshot, long viewerId, String fieldKey, ObjectScope scope) {
        if (scope.coversAll() || hasDepartmentSets(scope)) {
            return worstCase(snapshot, viewerId, fieldKey).visibility() == FieldVisibility.FULL;
        }
        for (ObjectScopeClause clause : scope.clauses()) {
            if (clause.requiredIds().isEmpty()) {
                return worstCase(snapshot, viewerId, fieldKey).visibility() == FieldVisibility.FULL;
            }
            for (BigInteger memberId : clause.requiredIds()) {
                if (access(snapshot, viewerId, memberId.longValueExact(), fieldKey).visibility() != FieldVisibility.FULL) {
                    return false;
                }
            }
        }
        return !scope.clauses().isEmpty();
    }

    private FieldAccess worstCase(FieldPolicySnapshot snapshot, long viewerId, String fieldKey) {
        FieldAccess matched = null;
        boolean subset = false;
        boolean any = false;
        for (FieldRuleRow rule : snapshot.rules()) {
            if (!fieldKey.equals(rule.fieldKey()) || !matchesViewer(snapshot.tenantId(), viewerId, rule.viewer())) {
                continue;
            }
            any = true;
            if (!allTargets(rule.scopes())) {
                subset = true;
            }
            matched = DefaultPolicyDefinitions.stricter(matched,
                    new FieldAccess(rule.visibility(), rule.editable()));
        }
        FieldAccess resolved = !any ? snapshot.baselineOf(fieldKey)
                : subset ? DefaultPolicyDefinitions.stricter(snapshot.baselineOf(fieldKey), matched) : matched;
        return DefaultPolicyDefinitions.stricter(resolved, snapshot.ceilingOf(fieldKey));
    }

    private static boolean hasDepartmentSets(ObjectScope scope) {
        for (ObjectScopeClause clause : scope.clauses()) {
            if (!clause.departmentSets().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean allTargets(List<ScopeExpression> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return true;
        }
        for (ScopeExpression scope : scopes) {
            if (scope == null || scope.kind() != ScopeKind.ALL) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesViewer(long tenantId, long viewerId, Selection selection) {
        return expand(tenantId, selection).contains(viewerId);
    }

    private boolean matchesTarget(long tenantId, long viewerId, long targetId, List<ScopeExpression> scopes,
                                  Map<String, ScopeBinding> bindings) {
        if (scopes == null || scopes.isEmpty()) {
            return true;
        }
        List<ScopeClause> clauses = ScopeBinder.bind(new ActionGrant(MemberFieldKey.VALUE_DISPLAY_NAME, scopes),
                bindings == null ? Map.of() : bindings);
        if (clauses.isEmpty()) {
            return false;
        }
        for (ScopeClause clause : clauses) {
            if (matchesClause(tenantId, viewerId, targetId, clause)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesClause(long tenantId, long viewerId, long targetId, ScopeClause clause) {
        if (clause == null || clause.empty()) {
            return false;
        }
        if (clause.all()) {
            return true;
        }
        boolean matched = false;
        if (clause.self()) {
            if (viewerId != targetId) {
                return false;
            }
            matched = true;
        }
        if (!clause.objectIds().isEmpty()) {
            if (!clause.objectIds().contains(IamIds.text(targetId))) {
                return false;
            }
            matched = true;
        }
        if (clause.memberDepartments()) {
            Set<BigInteger> departments = closures.expand(BigInteger.valueOf(tenantId),
                    memberDepartments(tenantId, viewerId), clause.memberDepartmentDescendants());
            if (!memberInDepartments(tenantId, targetId, departments)) {
                return false;
            }
            matched = true;
        }
        if (!clause.departmentIds().isEmpty()) {
            List<BigInteger> ids = new ArrayList<>();
            for (String id : clause.departmentIds()) {
                ids.add(BigInteger.valueOf(IamIds.require(id)));
            }
            Set<BigInteger> departments = closures.expand(BigInteger.valueOf(tenantId), ids,
                    clause.departmentDescendants());
            if (!memberInDepartments(tenantId, targetId, departments)) {
                return false;
            }
            matched = true;
        }
        return matched;
    }

    private Set<Long> expand(long tenantId, Selection selection) {
        java.util.HashSet<Long> ids = new java.util.HashSet<>();
        if (selection == null) {
            return ids;
        }
        for (String memberId : selection.members()) {
            ids.add(IamIds.require(memberId));
        }
        for (DepartmentSelection department : selection.departments()) {
            Set<BigInteger> departments = closures.expand(BigInteger.valueOf(tenantId),
                    List.of(BigInteger.valueOf(IamIds.require(department.id()))), department.includeDescendants());
            if (departments.isEmpty()) {
                continue;
            }
            ids.addAll(memberships.selectList(Wrappers.<IamMemberDepartmentEntity>lambdaQuery()
                            .select(IamMemberDepartmentEntity::getMemberId)
                            .eq(IamMemberDepartmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                            .in(IamMemberDepartmentEntity::getDepartmentId, departments)).stream()
                    .map(row -> row.getMemberId().longValueExact()).collect(Collectors.toSet()));
        }
        return ids;
    }

    private Set<BigInteger> memberDepartments(long tenantId, long memberId) {
        return memberships.selectList(Wrappers.<IamMemberDepartmentEntity>lambdaQuery()
                        .select(IamMemberDepartmentEntity::getDepartmentId)
                        .eq(IamMemberDepartmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                        .eq(IamMemberDepartmentEntity::getMemberId, BigInteger.valueOf(memberId))).stream()
                .map(IamMemberDepartmentEntity::getDepartmentId)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private boolean memberInDepartments(long tenantId, long memberId, Set<BigInteger> departments) {
        if (departments == null || departments.isEmpty()) {
            return false;
        }
        return memberships.selectCount(Wrappers.<IamMemberDepartmentEntity>lambdaQuery()
                .eq(IamMemberDepartmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamMemberDepartmentEntity::getMemberId, BigInteger.valueOf(memberId))
                .in(IamMemberDepartmentEntity::getDepartmentId, departments)) > 0;
    }

    /**
     * <p>快照内一条已展开查看者选择器的字段规则。</p>
     *
     * @param viewer 查看者选择
     * @param scopes 目标范围
     * @param bindings 范围绑定
     * @param fieldKey 字段键
     * @param visibility 可见程度
     * @param editable 是否可编辑
     */
    public record FieldRuleRow(Selection viewer, List<ScopeExpression> scopes, Map<String, ScopeBinding> bindings,
                               String fieldKey, FieldVisibility visibility, boolean editable) {
    }
}
