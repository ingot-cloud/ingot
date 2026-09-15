package com.ingot.cloud.iam.policy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.evaluation.DepartmentClosure;
import com.ingot.cloud.iam.evaluation.ScopeBinder;
import com.ingot.cloud.iam.evaluation.ScopeClause;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.DepartmentSelection;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldProjection;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberFieldKey;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.PolicyScenario;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.Selection;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * <p>按场景、查看者与目标合并字段策略，投影响应并拒绝不可编辑或脱敏占位写入。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class FieldAccessEvaluator {
    private static final TypeReference<List<ScopeExpression>> SCOPES = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, ScopeBinding>> BINDINGS = new TypeReference<>() {
    };
    private static final String FIELD_RULE_SQL = """
            SELECT scenario,field_key,viewer_selector_id,target_scope,scope_bindings,visibility,editable
              FROM iam_field_rule WHERE tenant_id=:tenantId AND scenario=:scenario AND field_key=:fieldKey
             ORDER BY id
            """;
    private final NamedParameterJdbcTemplate jdbc;

    /**
     * 绑定 IAM 目标库。
     *
     * @param dataSource 独立 IAM 数据源
     */
    public FieldAccessEvaluator(DataSource dataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
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
        FieldAccess baseline = baseline(fieldKey);
        FieldVisibility visibility = null;
        Boolean editable = null;
        List<FieldRuleRow> rules = jdbc.query(FIELD_RULE_SQL, Map.of("tenantId", tenantId,
                "scenario", scenario.name(), "fieldKey", fieldKey), (row, index) -> new FieldRuleRow(
                selector(tenantId, row.getLong("viewer_selector_id")),
                IamJson.read(row.getString("target_scope"), SCOPES),
                IamJson.read(row.getString("scope_bindings"), BINDINGS),
                FieldVisibility.valueOf(row.getString("visibility")),
                row.getBoolean("editable")));
        for (FieldRuleRow rule : rules) {
            if (!matchesViewer(tenantId, viewerId, rule.viewer())
                    || !matchesTarget(tenantId, viewerId, targetMemberId, rule.scopes(), rule.bindings())) {
                continue;
            }
            if (visibility == null || rule.visibility().ordinal() < visibility.ordinal()) {
                visibility = rule.visibility();
            }
            editable = editable == null ? rule.editable() : editable && rule.editable();
        }
        if (visibility == null) {
            return baseline;
        }
        if (visibility != FieldVisibility.FULL) {
            editable = false;
        }
        return new FieldAccess(visibility, Boolean.TRUE.equals(editable));
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
        Map<String, FieldAccess> access = new LinkedHashMap<>();
        for (MemberFieldKey field : MemberFieldKey.values()) {
            access.put(field.getValue(), access(tenantId, viewerId, targetMemberId, scenario, field.getValue()));
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
        FieldAccess displayName = access.getOrDefault(MemberFieldKey.VALUE_DISPLAY_NAME, baseline(MemberFieldKey.VALUE_DISPLAY_NAME));
        FieldAccess avatar = access.getOrDefault(MemberFieldKey.VALUE_AVATAR, baseline(MemberFieldKey.VALUE_AVATAR));
        FieldAccess phone = access.getOrDefault(MemberFieldKey.VALUE_PHONE, baseline(MemberFieldKey.VALUE_PHONE));
        FieldAccess email = access.getOrDefault(MemberFieldKey.VALUE_EMAIL, baseline(MemberFieldKey.VALUE_EMAIL));
        return new MemberRecord(raw.id(),
                FieldProjection.project(raw.displayName(), displayName.visibility()),
                FieldProjection.project(raw.avatar(), avatar.visibility()),
                FieldProjection.project(raw.phone(), phone.visibility()),
                FieldProjection.project(raw.email(), email.visibility()),
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
     * 未完整可见的字段禁止按原值筛选。
     *
     * @param tenantId 当前租户
     * @param viewerId 操作者
     * @param fieldKey 字段键
     * @param submitted 筛选原值
     */
    public void requireOriginalLookup(long tenantId, long viewerId, String fieldKey, String submitted) {
        if (submitted == null || submitted.isBlank()) {
            return;
        }
        FieldAccess access = access(tenantId, viewerId, viewerId, PolicyScenario.MANAGEMENT, fieldKey);
        if (access.visibility() != FieldVisibility.FULL) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
    }

    private static FieldAccess baseline(String fieldKey) {
        if (MemberFieldKey.VALUE_PHONE.equals(fieldKey) || MemberFieldKey.VALUE_EMAIL.equals(fieldKey)) {
            return new FieldAccess(FieldVisibility.MASKED, false);
        }
        return new FieldAccess(FieldVisibility.FULL, true);
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
            Set<Long> departments = DepartmentClosure.expand(jdbc, tenantId, memberDepartments(tenantId, viewerId),
                    clause.memberDepartmentDescendants());
            if (!memberInDepartments(tenantId, targetId, departments)) {
                return false;
            }
            matched = true;
        }
        if (!clause.departmentIds().isEmpty()) {
            List<Long> ids = new ArrayList<>();
            for (String id : clause.departmentIds()) {
                ids.add(IamIds.require(id));
            }
            Set<Long> departments = DepartmentClosure.expand(jdbc, tenantId, ids, clause.departmentDescendants());
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
            Set<Long> departments = DepartmentClosure.expand(jdbc, tenantId,
                    List.of(IamIds.require(department.id())), department.includeDescendants());
            if (departments.isEmpty()) {
                continue;
            }
            ids.addAll(jdbc.queryForList("""
                    SELECT member_id FROM iam_member_department
                     WHERE tenant_id=:tenantId AND department_id IN (:ids)
                    """, Map.of("tenantId", tenantId, "ids", departments), Long.class));
        }
        return ids;
    }

    private Selection selector(long tenantId, long selectorId) {
        List<String> members = jdbc.queryForList("""
                SELECT member_id FROM iam_policy_selector_member
                 WHERE tenant_id=:tenantId AND selector_id=:id ORDER BY member_id
                """, Map.of("tenantId", tenantId, "id", selectorId), String.class);
        List<DepartmentSelection> departments = jdbc.query("""
                SELECT department_id,include_descendants FROM iam_policy_selector_department
                 WHERE tenant_id=:tenantId AND selector_id=:id ORDER BY department_id
                """, Map.of("tenantId", tenantId, "id", selectorId),
                (row, index) -> new DepartmentSelection(row.getString("department_id"),
                        row.getBoolean("include_descendants")));
        return new Selection(members, departments);
    }

    private Set<Long> memberDepartments(long tenantId, long memberId) {
        return new java.util.LinkedHashSet<>(jdbc.queryForList("""
                SELECT department_id FROM iam_member_department
                 WHERE tenant_id=:tenantId AND member_id=:memberId
                """, Map.of("tenantId", tenantId, "memberId", memberId), Long.class));
    }

    private boolean memberInDepartments(long tenantId, long memberId, Set<Long> departments) {
        if (departments == null || departments.isEmpty()) {
            return false;
        }
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM iam_member_department
                 WHERE tenant_id=:tenantId AND member_id=:memberId AND department_id IN (:ids)
                """, Map.of("tenantId", tenantId, "memberId", memberId, "ids", departments), Long.class);
        return count != null && count > 0;
    }

    private record FieldRuleRow(Selection viewer, List<ScopeExpression> scopes, Map<String, ScopeBinding> bindings,
                                FieldVisibility visibility, boolean editable) {
    }
}
