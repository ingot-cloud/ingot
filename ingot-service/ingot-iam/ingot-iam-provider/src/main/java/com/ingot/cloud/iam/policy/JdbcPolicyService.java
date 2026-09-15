package com.ingot.cloud.iam.policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.evaluation.DepartmentClosure;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamDetails;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.cloud.iam.support.IamSelections;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.DefaultPolicyKind;
import com.ingot.framework.commons.model.iam.DepartmentRecord;
import com.ingot.framework.commons.model.iam.DepartmentSelection;
import com.ingot.framework.commons.model.iam.DirectoryDefault;
import com.ingot.framework.commons.model.iam.DirectoryDefaultScope;
import com.ingot.framework.commons.model.iam.DirectoryPolicyDraft;
import com.ingot.framework.commons.model.iam.DirectoryPolicyInput;
import com.ingot.framework.commons.model.iam.DirectoryRule;
import com.ingot.framework.commons.model.iam.FieldPolicyDraft;
import com.ingot.framework.commons.model.iam.FieldPolicyInput;
import com.ingot.framework.commons.model.iam.FieldRule;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ImpactSummary;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.PolicyDraft;
import com.ingot.framework.commons.model.iam.PolicyEffect;
import com.ingot.framework.commons.model.iam.PolicyPreviewInput;
import com.ingot.framework.commons.model.iam.PolicyPreviewResult;
import com.ingot.framework.commons.model.iam.PolicyScenario;
import com.ingot.framework.commons.model.iam.Preview;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.Selection;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>维护租户通讯录与字段策略，并按策略投影普通通讯录，不产生业务授权。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class JdbcPolicyService {
    private static final TypeReference<List<ScopeExpression>> SCOPES = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, ScopeBinding>> BINDINGS = new TypeReference<>() {
    };
    private static final String DIRECTORY_POLICY = "directory-policy";
    private static final String FIELD_POLICY = "field-policy";
    private final IamAccess access;
    private final IamAuditWriter audits;
    private final FieldAccessEvaluator fields;
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、审计、字段求值与策略表。
     *
     * @param access 当前身份
     * @param audits 同事务审计
     * @param fields 字段访问
     * @param dataSource IAM 目标库
     * @param transactionManager 同一数据源事务
     */
    public JdbcPolicyService(IamAccess access, IamAuditWriter audits, FieldAccessEvaluator fields,
                             DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.access = access;
        this.audits = audits;
        this.fields = fields;
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 读取通讯录策略。
     *
     * @return 策略详情
     */
    public ResourceDetail<DirectoryPolicyDraft> getDirectoryPolicy() {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DIRECTORY_POLICY_READ);
        return loadDirectory(IamIds.require(actor.context().tenantId()));
    }

    /**
     * 整体替换通讯录策略。
     *
     * @param input 完整配置
     * @return 提交后详情
     */
    public ResourceDetail<DirectoryPolicyDraft> putDirectoryPolicy(DirectoryPolicyInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DIRECTORY_POLICY_UPDATE);
        long tenantId = IamIds.require(actor.context().tenantId());
        return transaction.execute(status -> {
            ResourceDetail<DirectoryPolicyDraft> current = loadDirectory(tenantId);
            IamIds.requireExpected(input.expectedVersion(), current.version());
            requireDefault(input.policy().defaultRevisionId(), DefaultPolicyKind.DIRECTORY);
            replaceDirectory(actor, tenantId, input.policy());
            audits.write(actor.context(), access.nextId(), DIRECTORY_POLICY, IamIds.text(tenantId),
                    AuditChangeType.UPDATE, Map.of(AuditField.POLICY_VERSION, current.version()),
                    Map.of(AuditField.POLICY_VERSION, Long.toString(Long.parseLong(current.version()) + 1)),
                    Map.of(DIRECTORY_POLICY, Long.toString(Long.parseLong(current.version()) + 1)));
            return loadDirectory(tenantId);
        });
    }

    /**
     * 读取字段策略。
     *
     * @return 策略详情
     */
    public ResourceDetail<FieldPolicyDraft> getFieldPolicy() {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_FIELD_POLICY_READ);
        return loadField(IamIds.require(actor.context().tenantId()));
    }

    /**
     * 整体替换字段策略。
     *
     * @param input 完整配置
     * @return 提交后详情
     */
    public ResourceDetail<FieldPolicyDraft> putFieldPolicy(FieldPolicyInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_FIELD_POLICY_UPDATE);
        long tenantId = IamIds.require(actor.context().tenantId());
        return transaction.execute(status -> {
            ResourceDetail<FieldPolicyDraft> current = loadField(tenantId);
            IamIds.requireExpected(input.expectedVersion(), current.version());
            requireDefault(input.policy().defaultRevisionId(), DefaultPolicyKind.FIELD);
            replaceField(actor, tenantId, input.policy());
            audits.write(actor.context(), access.nextId(), FIELD_POLICY, IamIds.text(tenantId),
                    AuditChangeType.UPDATE, Map.of(AuditField.POLICY_VERSION, current.version()),
                    Map.of(AuditField.POLICY_VERSION, Long.toString(Long.parseLong(current.version()) + 1)),
                    Map.of(FIELD_POLICY, Long.toString(Long.parseLong(current.version()) + 1)));
            return loadField(tenantId);
        });
    }

    /**
     * 只读策略预览，结果与操作者可见范围求交。
     *
     * @param input 草稿与查看者
     * @return 可见样例
     */
    public Preview<PolicyPreviewResult> preview(PolicyPreviewInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_POLICY_PREVIEW);
        long tenantId = IamIds.require(actor.context().tenantId());
        DirectoryPolicyDraft directory = input.policyDraft().kind() == DefaultPolicyKind.DIRECTORY
                ? input.policyDraft().directory() : loadDirectory(tenantId).record();
        Set<Long> visible = visibleMembers(tenantId, IamIds.require(input.viewerMemberId()), directory);
        Set<Long> operator = visibleMembers(tenantId, IamIds.require(actor.context().memberId()),
                loadDirectory(tenantId).record());
        visible.retainAll(operator);
        List<ResourceDetail<MemberRecord>> members = new ArrayList<>();
        for (Long memberId : visible) {
            members.add(IamDetails.of(member(tenantId, memberId, IamIds.require(input.viewerMemberId()),
                    PolicyScenario.DIRECTORY), "0"));
            if (members.size() >= IamPages.DEFAULT_SIZE) {
                break;
            }
        }
        PolicyPreviewResult result = new PolicyPreviewResult(input.policyDraft().kind(), members, List.of(), false);
        return new Preview<>("0", true, List.of(), List.of(), new ImpactSummary((long) visible.size(), null, null, false),
                result);
    }

    /**
     * 分页列出当前查看者可见的普通通讯录成员。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @return 成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> listDirectoryMembers(int page, int pageSize) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DIRECTORY_READ);
        IamPages.require(page, pageSize);
        long tenantId = IamIds.require(actor.context().tenantId());
        long viewerId = IamIds.require(actor.context().memberId());
        List<Long> visible = new ArrayList<>(visibleMembers(tenantId, viewerId,
                loadDirectory(tenantId).record()));
        visible.sort(Long::compareTo);
        int from = IamPages.offset(page, pageSize);
        List<ResourceDetail<MemberRecord>> items = new ArrayList<>();
        for (int index = from; index < visible.size() && items.size() < pageSize; index++) {
            items.add(IamDetails.of(member(tenantId, visible.get(index), viewerId, PolicyScenario.DIRECTORY), "0"));
        }
        return IamPages.details(items, visible.size(), page, pageSize);
    }

    /**
     * 读取普通通讯录成员详情。
     *
     * @param id 成员 ID
     * @return 投影后的详情
     */
    public ResourceDetail<MemberRecord> getDirectoryMember(String id) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DIRECTORY_READ);
        long tenantId = IamIds.require(actor.context().tenantId());
        long memberId = IamIds.require(id);
        long viewerId = IamIds.require(actor.context().memberId());
        if (!visibleMembers(tenantId, viewerId, loadDirectory(tenantId).record()).contains(memberId)) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return IamDetails.of(member(tenantId, memberId, viewerId, PolicyScenario.DIRECTORY), "0");
    }

    /**
     * 列出普通通讯录可见部门树。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @return 部门页
     */
    public PageResponse<ResourceDetail<DepartmentRecord>> listDirectoryDepartments(int page, int pageSize) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DIRECTORY_READ);
        IamPages.require(page, pageSize);
        long tenantId = IamIds.require(actor.context().tenantId());
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM iam_department WHERE tenant_id=:tenantId",
                Map.of("tenantId", tenantId), Long.class);
        List<ResourceDetail<DepartmentRecord>> items = jdbc.query("""
                SELECT id,parent_id,name,sort_order,version FROM iam_department
                 WHERE tenant_id=:tenantId ORDER BY sort_order,id LIMIT :limit OFFSET :offset
                """, Map.of("tenantId", tenantId, "limit", pageSize, "offset", IamPages.offset(page, pageSize)),
                (row, index) -> IamDetails.of(new DepartmentRecord(row.getString("id"),
                        row.getObject("parent_id") == null ? null : row.getString("parent_id"),
                        row.getString("name"), row.getInt("sort_order"), false), row.getString("version")));
        return IamPages.details(items, total == null ? 0 : total, page, pageSize);
    }

    private ResourceDetail<DirectoryPolicyDraft> loadDirectory(long tenantId) {
        List<ResourceDetail<DirectoryPolicyDraft>> rows = jdbc.query("""
                SELECT default_revision_id,default_scope,default_selector_id,version
                  FROM iam_directory_policy WHERE tenant_id=:tenantId
                """, Map.of("tenantId", tenantId), (row, index) -> {
            DirectoryDefault override = null;
            if (row.getString("default_scope") != null) {
                DirectoryDefaultScope scope = DirectoryDefaultScope.valueOf(row.getString("default_scope"));
                Selection selection = scope == DirectoryDefaultScope.SELECTED
                        ? selector(tenantId, row.getLong("default_selector_id")) : null;
                override = new DirectoryDefault(scope, selection);
            }
            List<DirectoryRule> rules = jdbc.query("""
                    SELECT effect,viewer_selector_id,target_selector_id FROM iam_directory_rule
                     WHERE tenant_id=:tenantId ORDER BY id
                    """, Map.of("tenantId", tenantId), (item, i) -> new DirectoryRule(
                    PolicyEffect.valueOf(item.getString("effect")),
                    selector(tenantId, item.getLong("viewer_selector_id")),
                    selector(tenantId, item.getLong("target_selector_id"))));
            return IamDetails.of(new DirectoryPolicyDraft(row.getString("default_revision_id"), override, rules),
                    row.getString("version"));
        });
        if (rows.size() == 1) {
            return rows.getFirst();
        }
        String defaultId = latestDefault(DefaultPolicyKind.DIRECTORY);
        return IamDetails.of(new DirectoryPolicyDraft(defaultId, null, List.of()), "0");
    }

    private ResourceDetail<FieldPolicyDraft> loadField(long tenantId) {
        List<ResourceDetail<FieldPolicyDraft>> rows = jdbc.query("""
                SELECT default_revision_id,version FROM iam_field_policy WHERE tenant_id=:tenantId
                """, Map.of("tenantId", tenantId), (row, index) -> {
            List<FieldRule> rules = jdbc.query("""
                    SELECT scenario,field_key,viewer_selector_id,target_scope,scope_bindings,visibility,editable
                      FROM iam_field_rule WHERE tenant_id=:tenantId ORDER BY id
                    """, Map.of("tenantId", tenantId), (item, i) -> new FieldRule(
                    PolicyScenario.valueOf(item.getString("scenario")), item.getString("field_key"),
                    selector(tenantId, item.getLong("viewer_selector_id")),
                    IamJson.read(item.getString("target_scope"), SCOPES),
                    IamJson.read(item.getString("scope_bindings"), BINDINGS),
                    FieldVisibility.valueOf(item.getString("visibility")), item.getBoolean("editable")));
            return IamDetails.of(new FieldPolicyDraft(row.getString("default_revision_id"), rules),
                    row.getString("version"));
        });
        if (rows.size() == 1) {
            return rows.getFirst();
        }
        return IamDetails.of(new FieldPolicyDraft(latestDefault(DefaultPolicyKind.FIELD), List.of()), "0");
    }

    private void replaceDirectory(ActiveIdentity actor, long tenantId, DirectoryPolicyDraft policy) {
        jdbc.update("DELETE FROM iam_directory_rule WHERE tenant_id=:tenantId", Map.of("tenantId", tenantId));
        Long selectorId = null;
        String scope = null;
        if (policy.defaultOverride() != null) {
            scope = policy.defaultOverride().scope().name();
            if (policy.defaultOverride().scope() == DirectoryDefaultScope.SELECTED) {
                selectorId = writeSelector(actor, tenantId, policy.defaultOverride().selection());
            }
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("tenantId", tenantId);
        parameters.put("revisionId", IamIds.require(policy.defaultRevisionId()));
        parameters.put("kind", DefaultPolicyKind.DIRECTORY.name());
        parameters.put("scope", scope);
        parameters.put("selectorId", selectorId);
        jdbc.update("""
                INSERT INTO iam_directory_policy(tenant_id,default_revision_id,default_kind,default_scope,default_selector_id)
                VALUES (:tenantId,:revisionId,:kind,:scope,:selectorId)
                ON DUPLICATE KEY UPDATE default_revision_id=:revisionId,default_scope=:scope,
                  default_selector_id=:selectorId,version=version+1
                """, parameters);
        for (DirectoryRule rule : policy.rules()) {
            long viewer = writeSelector(actor, tenantId, rule.viewerSelection());
            long target = writeSelector(actor, tenantId, rule.targetSelection());
            jdbc.update("""
                    INSERT INTO iam_directory_rule(id,tenant_id,effect,viewer_selector_id,target_selector_id)
                    VALUES (:id,:tenantId,:effect,:viewer,:target)
                    """, Map.of("id", access.nextId(), "tenantId", tenantId, "effect", rule.effect().name(),
                    "viewer", viewer, "target", target));
        }
    }

    private void replaceField(ActiveIdentity actor, long tenantId, FieldPolicyDraft policy) {
        jdbc.update("DELETE FROM iam_field_rule WHERE tenant_id=:tenantId", Map.of("tenantId", tenantId));
        jdbc.update("""
                INSERT INTO iam_field_policy(tenant_id,default_revision_id,default_kind)
                VALUES (:tenantId,:revisionId,:kind)
                ON DUPLICATE KEY UPDATE default_revision_id=:revisionId,version=version+1
                """, Map.of("tenantId", tenantId, "revisionId", IamIds.require(policy.defaultRevisionId()),
                "kind", DefaultPolicyKind.FIELD.name()));
        for (FieldRule rule : policy.rules()) {
            long viewer = writeSelector(actor, tenantId, rule.viewerSelection());
            jdbc.update("""
                    INSERT INTO iam_field_rule(id,tenant_id,scenario,field_key,viewer_selector_id,target_scope,
                      scope_bindings,visibility,editable)
                    VALUES (:id,:tenantId,:scenario,:fieldKey,:viewer,:scope,:bindings,:visibility,:editable)
                    """, Map.of("id", access.nextId(), "tenantId", tenantId, "scenario", rule.scenario().name(),
                    "fieldKey", rule.fieldKey(), "viewer", viewer, "scope", IamJson.array(rule.targetScope()),
                    "bindings", IamJson.object(rule.scopeBindings()), "visibility", rule.visibility().name(),
                    "editable", rule.editable()));
        }
    }

    private long writeSelector(ActiveIdentity actor, long tenantId, Selection selection) {
        IamSelections.requireCompatible(AuthorizationDomain.TENANT, selection);
        long id = access.nextId();
        jdbc.update("INSERT INTO iam_policy_selector(id,tenant_id) VALUES (:id,:tenantId)",
                Map.of("id", id, "tenantId", tenantId));
        for (String memberId : selection.members()) {
            jdbc.update("""
                    INSERT INTO iam_policy_selector_member(tenant_id,selector_id,member_id)
                    VALUES (:tenantId,:id,:memberId)
                    """, Map.of("tenantId", tenantId, "id", id, "memberId", IamIds.require(memberId)));
        }
        for (DepartmentSelection department : selection.departments()) {
            jdbc.update("""
                    INSERT INTO iam_policy_selector_department(tenant_id,selector_id,department_id,include_descendants)
                    VALUES (:tenantId,:id,:departmentId,:descendants)
                    """, Map.of("tenantId", tenantId, "id", id, "departmentId", IamIds.require(department.id()),
                    "descendants", department.includeDescendants()));
        }
        return id;
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

    private Set<Long> visibleMembers(long tenantId, long viewerId, DirectoryPolicyDraft policy) {
        Set<Long> allowed = new HashSet<>();
        DirectoryDefaultScope scope = policy.defaultOverride() == null
                ? DirectoryDefaultScope.SELF : policy.defaultOverride().scope();
        if (scope == DirectoryDefaultScope.ALL) {
            allowed.addAll(jdbc.queryForList("""
                    SELECT id FROM iam_tenant_member WHERE tenant_id=:tenantId AND status<>'REMOVED'
                    """, Map.of("tenantId", tenantId), Long.class));
        } else if (scope == DirectoryDefaultScope.SELF) {
            allowed.add(viewerId);
        } else {
            allowed.addAll(expand(tenantId, policy.defaultOverride().selection()));
        }
        for (DirectoryRule rule : policy.rules()) {
            if (!matches(tenantId, viewerId, rule.viewerSelection())) {
                continue;
            }
            Set<Long> targets = expand(tenantId, rule.targetSelection());
            if (rule.effect() == PolicyEffect.ALLOW) {
                allowed.addAll(targets);
            } else {
                allowed.removeAll(targets);
            }
        }
        return allowed;
    }

    private boolean matches(long tenantId, long memberId, Selection selection) {
        return expand(tenantId, selection).contains(memberId);
    }

    private Set<Long> expand(long tenantId, Selection selection) {
        Set<Long> ids = new HashSet<>();
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

    private MemberRecord member(long tenantId, long memberId, long viewerId, PolicyScenario scenario) {
        List<MemberRecord> rows = jdbc.query("""
                SELECT id,display_name,avatar,phone,email,status FROM iam_tenant_member
                 WHERE tenant_id=:tenantId AND id=:id AND status<>'REMOVED'
                """, Map.of("tenantId", tenantId, "id", memberId), (row, index) -> {
            MemberRecord raw = new MemberRecord(row.getString("id"), row.getString("display_name"),
                    row.getString("avatar"), row.getString("phone"), row.getString("email"),
                    MemberStatus.valueOf(row.getString("status")), List.of());
            return fields.project(raw, fields.memberAccess(tenantId, viewerId, memberId, scenario));
        });
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return rows.getFirst();
    }

    private void requireDefault(String revisionId, DefaultPolicyKind kind) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM iam_default_policy_revision WHERE id=:id AND kind=:kind",
                Map.of("id", IamIds.require(revisionId), "kind", kind.name()), Long.class);
        if (count == null || count == 0) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
    }

    private String latestDefault(DefaultPolicyKind kind) {
        List<String> ids = jdbc.queryForList("""
                SELECT id FROM iam_default_policy_revision WHERE kind=:kind ORDER BY revision DESC,id DESC
                """, Map.of("kind", kind.name()), String.class);
        if (ids.isEmpty()) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return ids.getFirst();
    }
}
