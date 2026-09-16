package com.ingot.cloud.iam.policy;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamDirectoryPolicyEntity;
import com.ingot.cloud.iam.persistence.entity.IamDirectoryRuleEntity;
import com.ingot.cloud.iam.persistence.entity.IamFieldPolicyEntity;
import com.ingot.cloud.iam.persistence.entity.IamFieldRuleEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
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
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ImpactSummary;
import com.ingot.framework.commons.model.iam.MemberFieldKey;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.PolicyPreviewInput;
import com.ingot.framework.commons.model.iam.PolicyPreviewResult;
import com.ingot.framework.commons.model.iam.PolicyScenario;
import com.ingot.framework.commons.model.iam.Preview;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.Selection;
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
public class PolicyService {
    private static final TypeReference<List<ScopeExpression>> SCOPES = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, ScopeBinding>> BINDINGS = new TypeReference<>() {
    };
    private static final String DIRECTORY_POLICY = "directory-policy";
    private static final String FIELD_POLICY = "field-policy";
    private final IamAccess access;
    private final IamAuditWriter audits;
    private final FieldAccessEvaluator fields;
    private final DirectoryVisibilityEvaluator directory;
    private final PolicyWriteRepository policies;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、审计、字段求值、通讯录可见性与策略表。
     * <p>TransactionTemplate 无法由 Lombok 从 PlatformTransactionManager 直接生成，保留显式构造器。</p>
     *
     * @param access 当前身份
     * @param audits 同事务审计
     * @param fields 字段访问
     * @param directory 通讯录可见性
     * @param policies 策略读写
     * @param transactionManager 同一数据源事务
     */
    public PolicyService(IamAccess access, IamAuditWriter audits, FieldAccessEvaluator fields,
                             DirectoryVisibilityEvaluator directory, PolicyWriteRepository policies,
                             PlatformTransactionManager transactionManager) {
        this.access = access;
        this.audits = audits;
        this.fields = fields;
        this.directory = directory;
        this.policies = policies;
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
        long viewerId = IamIds.require(input.viewerMemberId());
        long operatorId = IamIds.require(actor.context().memberId());
        ResourceDetail<DirectoryPolicyDraft> savedDirectory = loadDirectory(tenantId);
        ResourceDetail<FieldPolicyDraft> savedField = loadField(tenantId);
        DirectoryPolicyDraft directoryDraft = input.policyDraft().kind() == DefaultPolicyKind.DIRECTORY
                ? input.policyDraft().directory() : savedDirectory.record();
        FieldPolicySnapshot snapshot = input.policyDraft().kind() == DefaultPolicyKind.FIELD
                ? fields.snapshot(tenantId, PolicyScenario.DIRECTORY, input.policyDraft().field())
                : fields.snapshot(tenantId, PolicyScenario.DIRECTORY);
        DirectoryVisibility preview = directory.evaluate(tenantId, viewerId, directoryDraft);
        DirectoryVisibility operator = directory.evaluate(tenantId, operatorId, savedDirectory.record());
        if (input.target() != null && !input.target().isBlank()) {
            long targetId = IamIds.require(input.target());
            if (!operator.contains(targetId)) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            if (!preview.contains(targetId)) {
                PolicyPreviewResult result = new PolicyPreviewResult(input.policyDraft().kind(), List.of(), List.of(),
                        operator.concealsAny(preview));
                return new Preview<>(previewVersion(input.policyDraft().kind(), savedDirectory, savedField), true,
                        List.of(), List.of(), new ImpactSummary(null, null, null, result.restricted()), result);
            }
        }
        List<DirectoryVisibility> visibilities = List.of(preview, operator);
        var membersPage = policies.pageVisibleMembers(tenantId, visibilities, null, null, IamPages.DEFAULT_PAGE,
                IamPages.DEFAULT_SIZE);
        List<ResourceDetail<MemberRecord>> members = new ArrayList<>();
        for (IamTenantMemberEntity row : membersPage.getRecords()) {
            if (input.target() != null && !input.target().isBlank()
                    && !input.target().equals(row.getId().toString())) {
                continue;
            }
            members.add(member(row, viewerId, snapshot));
        }
        var departmentsPage = policies.pageVisibleDepartments(tenantId, visibilities, IamPages.DEFAULT_PAGE,
                IamPages.DEFAULT_SIZE);
        List<DepartmentRecord> departments = new ArrayList<>();
        for (DirectoryDepartmentNode node : departmentsPage.getRecords()) {
            departments.add(departmentRecord(node));
        }
        boolean restricted = operator.concealsAny(preview);
        PolicyPreviewResult result = new PolicyPreviewResult(input.policyDraft().kind(), members, departments,
                restricted);
        return new Preview<>(previewVersion(input.policyDraft().kind(), savedDirectory, savedField), true, List.of(),
                List.of(), new ImpactSummary(null, null, null, restricted), result);
    }

    private static String previewVersion(DefaultPolicyKind kind, ResourceDetail<DirectoryPolicyDraft> directory,
                                         ResourceDetail<FieldPolicyDraft> field) {
        return kind == DefaultPolicyKind.DIRECTORY ? directory.version() : field.version();
    }

    /**
     * 分页列出当前查看者可见的普通通讯录成员。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @return 成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> listDirectoryMembers(int page, int pageSize, String phone,
                                                                           String email) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DIRECTORY_READ);
        IamPages.require(page, pageSize);
        long tenantId = IamIds.require(actor.context().tenantId());
        long viewerId = IamIds.require(actor.context().memberId());
        DirectoryVisibility visibility = directory.evaluate(tenantId, viewerId, loadDirectory(tenantId).record());
        FieldPolicySnapshot snapshot = fields.snapshot(tenantId, PolicyScenario.DIRECTORY);
        fields.requireOriginalLookup(snapshot, viewerId, MemberFieldKey.VALUE_PHONE, phone,
                visibility.toLookupScope());
        fields.requireOriginalLookup(snapshot, viewerId, MemberFieldKey.VALUE_EMAIL, email,
                visibility.toLookupScope());
        var result = policies.pageVisibleMembers(tenantId, List.of(visibility), phone, email, page, pageSize);
        List<ResourceDetail<MemberRecord>> items = new ArrayList<>();
        for (IamTenantMemberEntity row : result.getRecords()) {
            items.add(member(row, viewerId, snapshot));
        }
        return IamPages.details(items, result.getTotal(), page, pageSize);
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
        DirectoryVisibility visibility = directory.evaluate(tenantId, viewerId, loadDirectory(tenantId).record());
        if (!policies.visibleMember(tenantId, visibility, memberId)) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        IamTenantMemberEntity row = policies.findActiveMember(tenantId, memberId);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return member(row, viewerId, fields.snapshot(tenantId, PolicyScenario.DIRECTORY));
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
        long viewerId = IamIds.require(actor.context().memberId());
        DirectoryVisibility visibility = directory.evaluate(tenantId, viewerId, loadDirectory(tenantId).record());
        var result = policies.pageVisibleDepartments(tenantId, List.of(visibility), page, pageSize);
        List<ResourceDetail<DepartmentRecord>> items = result.getRecords().stream()
                .map(PolicyService::departmentDetail)
                .toList();
        return IamPages.details(items, result.getTotal(), page, pageSize);
    }

    private ResourceDetail<DirectoryPolicyDraft> loadDirectory(long tenantId) {
        IamDirectoryPolicyEntity row = policies.findDirectory(tenantId);
        if (row == null) {
            return IamDetails.of(new DirectoryPolicyDraft(latestDefault(DefaultPolicyKind.DIRECTORY), null, List.of()),
                    "0");
        }
        DirectoryDefault override = null;
        LinkedHashSet<Long> selectorIds = new LinkedHashSet<>();
        if (row.getDefaultScope() != null && row.getDefaultScope() == DirectoryDefaultScope.SELECTED) {
            selectorIds.add(selectorId(row.getDefaultSelectorId()));
        }
        List<IamDirectoryRuleEntity> items = policies.listDirectoryRules(tenantId);
        for (IamDirectoryRuleEntity item : items) {
            selectorIds.add(selectorId(item.getViewerSelectorId()));
            selectorIds.add(selectorId(item.getTargetSelectorId()));
        }
        Map<Long, Selection> selectors = policies.selectors(tenantId, selectorIds);
        if (row.getDefaultScope() != null) {
            Selection selection = row.getDefaultScope() == DirectoryDefaultScope.SELECTED
                    ? selection(selectors, selectorId(row.getDefaultSelectorId())) : null;
            override = new DirectoryDefault(row.getDefaultScope(), selection);
        }
        List<DirectoryRule> rules = new ArrayList<>();
        for (IamDirectoryRuleEntity item : items) {
            rules.add(new DirectoryRule(item.getEffect(),
                    selection(selectors, selectorId(item.getViewerSelectorId())),
                    selection(selectors, selectorId(item.getTargetSelectorId()))));
        }
        return IamDetails.of(new DirectoryPolicyDraft(row.getDefaultRevisionId().toString(), override, rules),
                row.getVersion().toString());
    }

    private ResourceDetail<FieldPolicyDraft> loadField(long tenantId) {
        IamFieldPolicyEntity row = policies.findField(tenantId);
        if (row == null) {
            return IamDetails.of(new FieldPolicyDraft(latestDefault(DefaultPolicyKind.FIELD), List.of()), "0");
        }
        List<IamFieldRuleEntity> items = policies.listFieldRules(tenantId);
        LinkedHashSet<Long> selectorIds = new LinkedHashSet<>();
        for (IamFieldRuleEntity item : items) {
            selectorIds.add(selectorId(item.getViewerSelectorId()));
        }
        Map<Long, Selection> selectors = policies.selectors(tenantId, selectorIds);
        List<FieldRule> rules = new ArrayList<>();
        for (IamFieldRuleEntity item : items) {
            rules.add(new FieldRule(item.getScenario(), item.getFieldKey(),
                    selection(selectors, selectorId(item.getViewerSelectorId())),
                    IamJson.read(item.getTargetScope(), SCOPES),
                    IamJson.read(item.getScopeBindings(), BINDINGS),
                    item.getVisibility(), Boolean.TRUE.equals(item.getEditable())));
        }
        return IamDetails.of(new FieldPolicyDraft(row.getDefaultRevisionId().toString(), rules),
                row.getVersion().toString());
    }

    private void replaceDirectory(ActiveIdentity actor, long tenantId, DirectoryPolicyDraft policy) {
        policies.deleteDirectoryRules(tenantId);
        Long selectorId = null;
        DirectoryDefaultScope scope = null;
        if (policy.defaultOverride() != null) {
            scope = policy.defaultOverride().scope();
            if (scope == DirectoryDefaultScope.SELECTED) {
                selectorId = writeSelector(actor, tenantId, policy.defaultOverride().selection());
            }
        }
        policies.upsertDirectory(tenantId, IamIds.require(policy.defaultRevisionId()), scope, selectorId);
        for (DirectoryRule rule : policy.rules()) {
            long viewer = writeSelector(actor, tenantId, rule.viewerSelection());
            long target = writeSelector(actor, tenantId, rule.targetSelection());
            policies.insertDirectoryRule(access.nextId(), tenantId, rule.effect(), viewer, target);
        }
    }

    private void replaceField(ActiveIdentity actor, long tenantId, FieldPolicyDraft policy) {
        policies.deleteFieldRules(tenantId);
        policies.upsertField(tenantId, IamIds.require(policy.defaultRevisionId()));
        for (FieldRule rule : policy.rules()) {
            long viewer = writeSelector(actor, tenantId, rule.viewerSelection());
            policies.insertFieldRule(access.nextId(), tenantId, rule.scenario(), rule.fieldKey(), viewer,
                    IamJson.array(rule.targetScope()), IamJson.object(rule.scopeBindings()), rule.visibility(),
                    rule.editable());
        }
    }

    private long writeSelector(ActiveIdentity actor, long tenantId, Selection selection) {
        IamSelections.requireCompatible(AuthorizationDomain.TENANT, selection);
        long id = access.nextId();
        policies.insertSelector(id, tenantId);
        for (String memberId : selection.members()) {
            policies.insertSelectorMember(tenantId, id, IamIds.require(memberId));
        }
        for (DepartmentSelection department : selection.departments()) {
            policies.insertSelectorDepartment(tenantId, id, IamIds.require(department.id()),
                    department.includeDescendants());
        }
        return id;
    }

    private ResourceDetail<MemberRecord> member(IamTenantMemberEntity row, long viewerId,
                                                FieldPolicySnapshot snapshot) {
        MemberRecord raw = new MemberRecord(row.getId().toString(), row.getDisplayName(), row.getAvatar(),
                row.getPhone(), row.getEmail(), row.getStatus(), List.of());
        Map<String, FieldAccess> access = fields.memberAccess(snapshot, viewerId, row.getId().longValueExact());
        String version = row.getVersion() == null ? "0" : row.getVersion().toString();
        return IamDetails.of(fields.project(raw, access), access, Map.of(), version);
    }

    private void requireDefault(String revisionId, DefaultPolicyKind kind) {
        if (!policies.hasDefaultRevision(IamIds.require(revisionId), kind)) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
    }

    private String latestDefault(DefaultPolicyKind kind) {
        String id = policies.latestDefaultId(kind);
        if (id == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return id;
    }

    private static ResourceDetail<DepartmentRecord> departmentDetail(DirectoryDepartmentNode node) {
        return IamDetails.of(departmentRecord(node),
                node.row().getVersion() == null ? "0" : node.row().getVersion().toString());
    }

    private static DepartmentRecord departmentRecord(DirectoryDepartmentNode node) {
        IamDepartmentEntity row = node.row();
        return new DepartmentRecord(row.getId().toString(),
                row.getParentId() == null ? null : row.getParentId().toString(),
                row.getName(), row.getSortOrder() == null ? 0 : row.getSortOrder(), node.navigationOnly());
    }

    private static Selection selection(Map<Long, Selection> selectors, long selectorId) {
        return selectors.getOrDefault(selectorId, new Selection(List.of(), List.of()));
    }

    private static long selectorId(BigInteger value) {
        return value == null ? 0L : value.longValueExact();
    }
}
