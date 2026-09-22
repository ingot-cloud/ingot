package com.ingot.cloud.iam.role;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.delegation.DelegationAdmission;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.RoleRepository;
import com.ingot.cloud.iam.persistence.entity.IamRoleAssignmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleDefinitionEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleDeltaEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleGrantEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleParameterEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleRevisionEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamFilters;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamDetails;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.ConfigurationStatus;
import com.ingot.framework.commons.model.iam.ConfigurationStatusInput;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.GrantStatus;
import com.ingot.framework.commons.model.iam.EffectiveRole;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ImpactSummary;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.Preview;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.RoleCreateInput;
import com.ingot.framework.commons.model.iam.RoleDefinitionDraft;
import com.ingot.framework.commons.model.iam.RoleDelta;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.RoleMetadataOverrides;
import com.ingot.framework.commons.model.iam.RoleParameterDefinition;
import com.ingot.framework.commons.model.iam.RolePublishInput;
import com.ingot.framework.commons.model.iam.RoleRevision;
import com.ingot.framework.commons.model.iam.RoleRevisionRef;
import com.ingot.framework.commons.model.iam.RoleSummary;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeBindingKind;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.ScopeKind;
import com.ingot.framework.commons.model.iam.SubjectRef;
import com.ingot.framework.commons.model.iam.SubjectType;
import com.ingot.framework.commons.model.iam.UpgradeInput;
import com.ingot.framework.commons.model.iam.UpgradePreview;
import com.ingot.framework.commons.model.iam.UpgradePreviewInput;
import com.ingot.framework.commons.model.iam.UsageSummary;
import com.ingot.framework.commons.model.iam.ValidationIssue;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>维护不可变角色版本与租户差异，发布不自动升级既有授权。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class RoleService {
    private static final TypeReference<List<ScopeExpression>> SCOPES = new TypeReference<>() {
    };
    private static final TypeReference<RoleMetadataOverrides> METADATA = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, ScopeBinding>> BINDINGS = new TypeReference<>() {
    };
    private static final String ROLE = "role";
    private final IamAccess access;
    private final IamAuditWriter audits;
    private final AuthorizationChangeNotifier changes;
    private final RoleSynthesisCache synthesis;
    private final RoleGrantValidator validator;
    private final DelegationAdmission delegations;
    private final RoleRepository roles;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、审计、失效通知、合成缓存与角色表。
     * <p>TransactionTemplate 无法由 Lombok 从 PlatformTransactionManager 直接生成，保留显式构造器。</p>
     *
     * @param access 当前身份
     * @param audits 同事务审计
     * @param changes 授权热缓存失效
     * @param synthesis 角色合成派生缓存
     * @param validator 操作与范围能力校验
     * @param delegations 委派派生授权准入
     * @param roles 角色持久化
     * @param transactionManager 同一数据源事务
     */
    public RoleService(IamAccess access, IamAuditWriter audits, AuthorizationChangeNotifier changes,
                           RoleSynthesisCache synthesis, RoleGrantValidator validator,
                           DelegationAdmission delegations, RoleRepository roles,
                           PlatformTransactionManager transactionManager) {
        this.access = access;
        this.audits = audits;
        this.changes = changes;
        this.synthesis = synthesis;
        this.validator = validator;
        this.delegations = delegations;
        this.roles = roles;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 列出当前路径可见的角色目录。
     *
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param page 页码
     * @param pageSize 页大小
     * @return 角色页，不按名称或状态筛选
     */
    public PageResponse<ResourceDetail<RoleSummary>> list(AuthorizationDomain domain, boolean shared,
                                                          int page, int pageSize) {
        return list(domain, shared, page, pageSize, null, null);
    }

    /**
     * 列出当前路径可见的角色目录，可按名称包含匹配和启停状态筛选。
     *
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param page 页码
     * @param pageSize 页大小
     * @param name 角色名称包含匹配，空白表示不限制
     * @param status 启停状态，空白表示不限制；仅接受 ENABLED/DISABLED
     * @return 角色页
     * @throws BizException 状态字面量非法时为 {@link IamReasonCode#INVALID_ARGUMENT}
     */
    public PageResponse<ResourceDetail<RoleSummary>> list(AuthorizationDomain domain, boolean shared,
                                                          int page, int pageSize, String name, String status) {
        ActiveIdentity actor = access.require(domain, action(domain, shared, AccessKind.READ));
        IamPages.require(page, pageSize);
        Page<IamRoleDefinitionEntity> rows = roles.pageDefinitions(domain, shared, tenantId(domain, actor), page,
                pageSize, name, IamFilters.enabledOf(status));
        List<ResourceDetail<RoleSummary>> items = rows.getRecords().stream()
                .map(row -> IamDetails.of(summary(row), version(row.getVersion()))).toList();
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    /**
     * 读取角色元数据。
     *
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @return 角色详情
     */
    public ResourceDetail<RoleSummary> get(AuthorizationDomain domain, boolean shared, String id) {
        ActiveIdentity actor = access.require(domain, action(domain, shared, AccessKind.READ));
        return loadSummary(domain, shared, actor, IamIds.require(id));
    }

    /**
     * 创建角色并发布首个不可变版本，不能创建系统治理角色。
     *
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param input 创建命令
     * @return 新角色 ID
     */
    public CreatedResource create(AuthorizationDomain domain, boolean shared, RoleCreateInput input) {
        ActiveIdentity actor = access.require(domain, action(domain, shared, AccessKind.CREATE));
        RoleKind kind = requireCreatable(domain, shared, input.kind());
        return transaction.execute(status -> {
            long id = access.nextId();
            Long tenantId = kind == RoleKind.TENANT_CUSTOM ? IamIds.require(actor.context().tenantId()) : null;
            IamRoleDefinitionEntity entity = new IamRoleDefinitionEntity();
            entity.setId(BigInteger.valueOf(id));
            entity.setDomain(kind == RoleKind.PLATFORM_CUSTOM
                    ? AuthorizationDomain.PLATFORM : AuthorizationDomain.TENANT);
            entity.setTenantId(tenantId == null ? null : BigInteger.valueOf(tenantId));
            entity.setKind(kind);
            entity.setCode(input.code());
            entity.setName(input.name());
            entity.setDescription(input.description());
            entity.setGroupName(input.groupName());
            entity.setEnabled(true);
            try {
                roles.insertDefinition(entity);
            } catch (DuplicateKeyException exception) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), "角色编码已存在");
            }
            publishRevision(id, kind, 1, input.baseRevisionId(), input.definition());
            audits.write(actor.context(), access.nextId(), ROLE, IamIds.text(id), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.NAME, input.name()), Map.of(ROLE, "0"));
            return new CreatedResource(IamIds.text(id), "0");
        });
    }

    /**
     * 启停角色，停用约束所有旧版本。
     *
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @param input 目标状态
     * @return 提交后版本
     */
    public CreatedResource changeStatus(AuthorizationDomain domain, boolean shared, String id,
                                        ConfigurationStatusInput input) {
        ActiveIdentity actor = access.require(domain, action(domain, shared, AccessKind.STATUS));
        long roleId = IamIds.require(id);
        return transaction.execute(status -> {
            RoleRow current = lock(domain, shared, actor, roleId);
            if (current.kind() == RoleKind.SYSTEM) {
                throw new BizException(IamReasonCode.ACTION_DENIED);
            }
            IamIds.requireVersion(input.expectedVersion(), version(current.version()));
            boolean enabled = input.status() == ConfigurationStatus.ENABLED;
            roles.updateEnabled(roleId, enabled, current.version());
            String next = nextVersion(current.version());
            audits.write(actor.context(), access.nextId(), ROLE, id,
                    enabled ? AuditChangeType.ENABLE : AuditChangeType.DISABLE,
                    Map.of(AuditField.STATUS, statusOf(current.enabled()).name()),
                    Map.of(AuditField.STATUS, input.status().name()), Map.of(ROLE, next));
            changes.markAll();
            return new CreatedResource(id, next);
        });
    }

    /**
     * 删除未被授权引用的非系统角色。
     *
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @return 删除前版本
     */
    public CreatedResource delete(AuthorizationDomain domain, boolean shared, String id) {
        ActiveIdentity actor = access.require(domain, action(domain, shared, AccessKind.DELETE));
        long roleId = IamIds.require(id);
        return transaction.execute(status -> {
            RoleRow current = lock(domain, shared, actor, roleId);
            if (current.kind() == RoleKind.SYSTEM) {
                throw new BizException(IamReasonCode.ACTION_DENIED);
            }
            if (roles.countAssignments(roleId) > 0) {
                throw new BizException(IamReasonCode.OBJECT_IN_USE);
            }
            roles.deleteRevisions(roleId);
            roles.deleteDefinition(roleId);
            audits.write(actor.context(), access.nextId(), ROLE, id, AuditChangeType.REMOVE,
                    Map.of(AuditField.NAME, current.name()), Map.of(), Map.of(ROLE, version(current.version())));
            changes.markAll();
            return new CreatedResource(id, version(current.version()));
        });
    }

    /**
     * 列出角色不可变版本。
     *
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 版本页
     */
    public PageResponse<ResourceDetail<RoleRevision>> listRevisions(AuthorizationDomain domain, boolean shared,
                                                                    String id, int page, int pageSize) {
        ActiveIdentity actor = access.require(domain, action(domain, shared, AccessKind.READ));
        long roleId = IamIds.require(id);
        loadSummary(domain, shared, actor, roleId);
        IamPages.require(page, pageSize);
        Page<IamRoleRevisionEntity> rows = roles.pageRevisions(roleId, page, pageSize);
        List<ResourceDetail<RoleRevision>> items = rows.getRecords().stream()
                .map(row -> IamDetails.of(revision(row), version(row.getRevision()))).toList();
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    /**
     * 发布新版本，不改写既有授权引用。
     *
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @param input 待发布定义
     * @return 新版本 ID
     */
    public CreatedResource publish(AuthorizationDomain domain, boolean shared, String id, RolePublishInput input) {
        ActiveIdentity actor = access.require(domain, action(domain, shared, AccessKind.PUBLISH));
        long roleId = IamIds.require(id);
        return transaction.execute(status -> {
            RoleRow current = lock(domain, shared, actor, roleId);
            if (current.kind() == RoleKind.SYSTEM) {
                throw new BizException(IamReasonCode.ACTION_DENIED);
            }
            IamIds.requireVersion(input.expectedVersion(), version(current.version()));
            long last = roles.maxRevision(roleId);
            String baseId = current.kind() == RoleKind.TENANT_CUSTOM ? currentBase(roleId) : null;
            long revisionId = publishRevision(roleId, current.kind(), last + 1, baseId, input.definition());
            roles.incrementVersion(roleId, current.version());
            audits.write(actor.context(), access.nextId(), ROLE, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.ROLE_REVISION, String.valueOf(last)),
                    Map.of(AuditField.ROLE_REVISION, IamIds.text(revisionId)),
                    Map.of(ROLE, nextVersion(current.version())));
            return new CreatedResource(IamIds.text(revisionId), Long.toString(last + 1));
        });
    }

    /**
     * 预览待发布定义的合成结果，无写入。
     *
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @param input 待发布定义
     * @return 合成预览
     */
    public Preview<EffectiveRole> preview(AuthorizationDomain domain, boolean shared, String id,
                                          RoleDefinitionDraft input) {
        ActiveIdentity actor = access.require(domain, action(domain, shared, AccessKind.PREVIEW));
        ResourceDetail<RoleSummary> role = loadSummary(domain, shared, actor, IamIds.require(id));
        List<ValidationIssue> errors = new ArrayList<>();
        EffectiveRole effective = null;
        try {
            RoleSynthesis.Result synthesized = synthesize(role.record(), input);
            errors.addAll(validator.validate(grantDomain(role.record().kind()), synthesized.grants(),
                    input.parameterDefinitions()));
            effective = new EffectiveRole(role.record(), new RoleRevisionRef(role.record().kind(), role.record().id()),
                    synthesized.grants(), synthesized.origins(),
                    input.parameterDefinitions() == null ? List.of() : input.parameterDefinitions(),
                    new UsageSummary(0L, 0L, false));
        } catch (BizException exception) {
            errors.add(new ValidationIssue("definition", IamReasonCode.POLICY_CONFLICT, exception.getMessage()));
        }
        return new Preview<>(role.version(), errors.isEmpty(), errors, List.of(),
                new ImpactSummary(null, null, null, false), effective);
    }

    /**
     * 预览共享基础升级，不写入引用。
     *
     * @param id 角色 ID
     * @param input 目标基础
     * @return 三方比较
     */
    public Preview<UpgradePreview> previewUpgrade(String id, UpgradePreviewInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_ROLE_UPGRADE);
        long roleId = IamIds.require(id);
        ResourceDetail<RoleSummary> role = loadSummary(AuthorizationDomain.TENANT, false, actor, roleId);
        if (role.record().kind() != RoleKind.TENANT_CUSTOM) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        RevisionData current = latestRevision(roleId);
        RevisionData oldBase = loadRevision(requireBased(current));
        RevisionData newBase = loadRevision(IamIds.require(input.newBaseRevisionId()));
        RoleSynthesis.UpgradePlan plan = RoleSynthesis.upgrade(oldBase.grants(), newBase.grants(), current.deltas(),
                input.resolutions());
        List<ValidationIssue> errors = plan.conflicts().stream()
                .map(conflict -> new ValidationIssue(conflict.key(), conflict.reasonCode(), conflict.message()))
                .toList();
        UpgradePreview preview = new UpgradePreview(role.version(), current.baseRevisionId(), input.newBaseRevisionId(),
                plan.changes(), plan.conflicts(), null, List.of(), new ImpactSummary(null, 0L, null, false));
        return new Preview<>(role.version(), errors.isEmpty(), errors, List.of(), preview.impactSummary(), preview);
    }

    /**
     * 提交共享基础升级；未解决冲突整次回滚。
     *
     * @param id 角色 ID
     * @param input 处置与要改写的授权
     * @return 新版本 ID
     */
    public CreatedResource upgrade(String id, UpgradeInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_ROLE_UPGRADE);
        long roleId = IamIds.require(id);
        return transaction.execute(status -> {
            RoleRow current = lock(AuthorizationDomain.TENANT, false, actor, roleId);
            if (current.kind() != RoleKind.TENANT_CUSTOM) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            IamIds.requireVersion(input.expectedVersion(), version(current.version()));
            RevisionData latest = latestRevision(roleId);
            RevisionData oldBase = loadRevision(requireBased(latest));
            RevisionData newBase = loadRevision(IamIds.require(input.newBaseRevisionId()));
            RoleSynthesis.UpgradePlan plan = RoleSynthesis.upgrade(oldBase.grants(), newBase.grants(), latest.deltas(),
                    input.resolutions());
            if (!plan.conflicts().isEmpty()) {
                throw new BizException(IamReasonCode.POLICY_CONFLICT);
            }
            long last = roles.maxRevision(roleId);
            RoleDefinitionDraft definition = new RoleDefinitionDraft(List.of(), plan.nextDeltas(),
                    latest.parameters(), latest.metadata());
            long revisionId = publishRevision(roleId, RoleKind.TENANT_CUSTOM, last + 1,
                    input.newBaseRevisionId(), definition);
            moveAssignments(actor, roleId, revisionId, input.assignmentIds());
            roles.incrementVersion(roleId, current.version());
            audits.write(actor.context(), access.nextId(), ROLE, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.ROLE_REVISION, latest.id()), Map.of(AuditField.ROLE_REVISION, IamIds.text(revisionId)),
                    Map.of(ROLE, nextVersion(current.version())));
            changes.markAll();
            return new CreatedResource(IamIds.text(revisionId), Long.toString(last + 1));
        });
    }

    /**
     * 合成指定版本的有效授权，供求值引擎复用。
     *
     * @param revisionId 版本 ID
     * @return 合成后的操作范围
     */
    public List<ActionGrant> synthesizedGrants(long revisionId) {
        RevisionData revision = loadRevision(revisionId);
        RoleRevisionSnapshot snapshot;
        if (revision.baseRevisionId() == null) {
            snapshot = new RoleRevisionSnapshot(revisionId, null, revision.grants(), List.of());
        } else {
            RevisionData base = loadRevision(IamIds.require(revision.baseRevisionId()));
            snapshot = new RoleRevisionSnapshot(revisionId, IamIds.require(revision.baseRevisionId()),
                    base.grants(), revision.deltas());
        }
        return synthesis.grants(snapshot);
    }

    private long publishRevision(long roleId, RoleKind kind, long revision, String baseRevisionId,
                                 RoleDefinitionDraft definition) {
        boolean based = based(baseRevisionId);
        requireShape(kind, based, definition);
        List<ActionGrant> synthesized = based
                ? RoleSynthesis.synthesize(baseGrants(baseRevisionId), definition.deltas()).grants()
                : RoleSynthesis.synthesize(definition.grants(), List.of()).grants();
        List<ValidationIssue> errors = validator.validate(grantDomain(kind), synthesized,
                definition.parameterDefinitions());
        if (!errors.isEmpty()) {
            ValidationIssue first = errors.getFirst();
            throw new BizException(first.code().getCode(), first.message());
        }
        long id = access.nextId();
        IamRoleRevisionEntity entity = new IamRoleRevisionEntity();
        entity.setId(BigInteger.valueOf(id));
        entity.setRoleId(BigInteger.valueOf(roleId));
        entity.setKind(kind);
        entity.setRevision(BigInteger.valueOf(revision));
        entity.setBaseRevisionId(baseRevisionId == null || baseRevisionId.isBlank()
                ? null : BigInteger.valueOf(IamIds.require(baseRevisionId)));
        entity.setMetadataOverrides(IamJson.object(definition.metadataOverrides() == null
                ? Map.of() : definition.metadataOverrides()));
        entity.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
        roles.insertRevision(entity);
        if (definition.parameterDefinitions() != null) {
            for (RoleParameterDefinition parameter : definition.parameterDefinitions()) {
                IamRoleParameterEntity row = new IamRoleParameterEntity();
                row.setRevisionId(BigInteger.valueOf(id));
                row.setParameterKey(parameter.key());
                row.setBindingKind(parameter.kind());
                roles.insertParameter(row);
            }
        }
        if (based) {
            for (RoleDelta delta : definition.deltas()) {
                IamRoleDeltaEntity row = new IamRoleDeltaEntity();
                row.setRevisionId(BigInteger.valueOf(id));
                row.setActionId(BigInteger.valueOf(IamIds.require(delta.actionId())));
                row.setOperation(delta.operation());
                row.setScopes(IamJson.array(delta.scopes()));
                roles.insertDelta(row);
            }
        } else {
            for (ActionGrant grant : definition.grants()) {
                IamRoleGrantEntity row = new IamRoleGrantEntity();
                row.setRevisionId(BigInteger.valueOf(id));
                row.setActionId(BigInteger.valueOf(IamIds.require(grant.actionId())));
                row.setScopes(IamJson.array(grant.scopes()));
                roles.insertGrant(row);
            }
        }
        return id;
    }

    /**
     * 按角色当前形态合成待发布定义：有共享基础时叠加差异，完整自定义角色直接使用自有授权。
     */
    private RoleSynthesis.Result synthesize(RoleSummary role, RoleDefinitionDraft input) {
        String base = currentBase(IamIds.require(role.id()));
        return based(base)
                ? RoleSynthesis.synthesize(baseGrants(base), input.deltas())
                : RoleSynthesis.synthesize(input.grants(), List.of());
    }

    /**
     * 把选中的授权改指到新版本。授权必须属于当前租户与被升级角色且仍然有效，逐条重验范围参数与来源委派，
     * 按锁定版本条件更新并核对行数；任一条不成立整次升级回滚，不留部分更新。
     */
    private void moveAssignments(ActiveIdentity actor, long roleId, long revisionId, List<String> assignmentIds) {
        if (assignmentIds == null || assignmentIds.isEmpty()) {
            return;
        }
        long tenantId = IamIds.require(actor.context().tenantId());
        List<ActionGrant> grants = synthesizedGrants(revisionId);
        for (String text : assignmentIds.stream().distinct().toList()) {
            long assignmentId = IamIds.require(text);
            IamRoleAssignmentEntity row = roles.lockTenantAssignment(tenantId, assignmentId);
            if (row == null || row.getStatus() != GrantStatus.ACTIVE) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            if (!roles.revisionBelongsToRole(row.getRevisionId().longValue(), roleId)) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            revalidate(row, revisionId, grants, tenantId);
            if (roles.updateAssignmentRevision(assignmentId, revisionId, row.getVersion()) != 1) {
                throw new BizException(IamReasonCode.POLICY_CONFLICT);
            }
        }
    }

    /**
     * 升级不得放大既有授权：新版本引用的管理范围必须仍有绑定，委派派生的授权还要在新版本上重新满足委派全部约束。
     */
    private void revalidate(IamRoleAssignmentEntity row, long revisionId, List<ActionGrant> grants, long tenantId) {
        Map<String, ScopeBinding> bindings = IamJson.read(row.getScopeBindings(), BINDINGS);
        Map<String, ScopeBinding> bound = bindings == null ? Map.of() : bindings;
        for (ActionGrant grant : grants) {
            for (ScopeExpression scope : grant.scopes() == null ? List.<ScopeExpression>of() : grant.scopes()) {
                if (requiredBinding(scope.kind()) != null
                        && (scope.parameterKey() == null || !bound.containsKey(scope.parameterKey()))) {
                    throw new BizException(IamReasonCode.INVALID_ARGUMENT);
                }
            }
        }
        if (row.getDelegationGrantId() == null) {
            return;
        }
        List<ValidationIssue> errors = delegations.check(new DelegationAdmission.Request(AuthorizationDomain.TENANT,
                tenantId, row.getDelegationGrantId().longValue(), null, revisionId, grants, bound, subject(row),
                instant(row.getValidFrom()), instant(row.getValidUntil())));
        if (!errors.isEmpty()) {
            throw new BizException(errors.getFirst().code());
        }
    }

    private static SubjectRef subject(IamRoleAssignmentEntity row) {
        BigInteger id = row.getSubjectType() == SubjectType.MEMBER ? row.getTenantMemberId() : row.getTenantGroupId();
        return new SubjectRef(row.getSubjectType(), text(id));
    }

    private static ScopeBindingKind requiredBinding(ScopeKind kind) {
        if (kind == ScopeKind.MANAGED_DEPARTMENTS) {
            return ScopeBindingKind.DEPARTMENTS;
        }
        return kind == ScopeKind.OBJECT_SET ? ScopeBindingKind.OBJECTS : null;
    }

    private static long requireBased(RevisionData revision) {
        if (!based(revision.baseRevisionId())) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return IamIds.require(revision.baseRevisionId());
    }

    private static Instant instant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    /**
     * 版本形态在角色创建时定型：有共享基础的角色只保存差异，完整自定义角色只保存自有授权。
     */
    private static void requireShape(RoleKind kind, boolean based, RoleDefinitionDraft definition) {
        if (based && kind != RoleKind.TENANT_CUSTOM) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        boolean hasGrants = definition.grants() != null && !definition.grants().isEmpty();
        boolean hasDeltas = definition.deltas() != null && !definition.deltas().isEmpty();
        if (based ? hasGrants : hasDeltas) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
    }

    /**
     * 读取共享基础版本的授权；基础必须是共享角色版本，禁止多层差异继承。
     */
    private List<ActionGrant> baseGrants(String baseRevisionId) {
        long id = IamIds.require(baseRevisionId);
        IamRoleRevisionEntity row = roles.findRevision(id);
        if (row == null || row.getKind() != RoleKind.SHARED) {
            throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
        }
        return loadRevision(id).grants();
    }

    private static boolean based(String baseRevisionId) {
        return baseRevisionId != null && !baseRevisionId.isBlank();
    }

    private static AuthorizationDomain grantDomain(RoleKind kind) {
        return kind == RoleKind.PLATFORM_CUSTOM ? AuthorizationDomain.PLATFORM : AuthorizationDomain.TENANT;
    }

    private ResourceDetail<RoleSummary> loadSummary(AuthorizationDomain domain, boolean shared, ActiveIdentity actor,
                                                    long id) {
        IamRoleDefinitionEntity row = roles.findDefinition(domain, shared, tenantId(domain, actor), id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return IamDetails.of(summary(row), version(row.getVersion()));
    }

    private RoleRow lock(AuthorizationDomain domain, boolean shared, ActiveIdentity actor, long id) {
        IamRoleDefinitionEntity row = roles.lockDefinition(domain, shared, tenantId(domain, actor), id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return new RoleRow(row.getName(), row.getKind(), Boolean.TRUE.equals(row.getEnabled()), row.getVersion());
    }

    private RevisionData latestRevision(long roleId) {
        IamRoleRevisionEntity row = roles.latestRevision(roleId);
        if (row == null || row.getId() == null) {
            throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
        }
        return loadRevision(row.getId().longValue());
    }

    /**
     * 读取角色当前形态的共享基础；完整自定义角色为空。
     */
    private String currentBase(long roleId) {
        return latestRevision(roleId).baseRevisionId();
    }

    private RevisionData loadRevision(long revisionId) {
        IamRoleRevisionEntity row = roles.findRevision(revisionId);
        if (row == null) {
            throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
        }
        List<ActionGrant> grantRows = roles.listGrants(revisionId).stream()
                .map(item -> new ActionGrant(text(item.getActionId()), IamJson.read(item.getScopes(), SCOPES)))
                .toList();
        List<RoleDelta> deltaRows = roles.listDeltas(revisionId).stream()
                .map(item -> new RoleDelta(text(item.getActionId()), item.getOperation(),
                        IamJson.read(item.getScopes(), SCOPES)))
                .toList();
        List<RoleParameterDefinition> parameterRows = roles.listParameters(revisionId).stream()
                .map(item -> new RoleParameterDefinition(item.getParameterKey(), item.getBindingKind()))
                .toList();
        return new RevisionData(text(row.getId()), text(row.getBaseRevisionId()), grantRows, deltaRows,
                parameterRows, IamJson.read(row.getMetadataOverrides(), METADATA));
    }

    private RoleRevision revision(IamRoleRevisionEntity row) {
        RevisionData data = loadRevision(row.getId().longValue());
        return new RoleRevision(text(row.getId()), text(row.getRoleId()), version(row.getRevision()),
                row.getKind(), text(row.getBaseRevisionId()), data.grants(), data.deltas(), data.parameters(),
                data.metadata());
    }

    private static RoleSummary summary(IamRoleDefinitionEntity row) {
        return new RoleSummary(text(row.getId()), row.getCode(), row.getName(), row.getDescription(),
                row.getGroupName(), row.getKind(), statusOf(Boolean.TRUE.equals(row.getEnabled())));
    }

    private static Long tenantId(AuthorizationDomain domain, ActiveIdentity actor) {
        if (domain != AuthorizationDomain.TENANT) {
            return null;
        }
        return actor == null ? 0L : IamIds.require(actor.context().tenantId());
    }

    private static RoleKind requireCreatable(AuthorizationDomain domain, boolean shared, RoleKind kind) {
        if (kind == RoleKind.SYSTEM) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        if (shared && kind != RoleKind.SHARED) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        if (!shared && domain == AuthorizationDomain.PLATFORM && kind != RoleKind.PLATFORM_CUSTOM) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        if (!shared && domain == AuthorizationDomain.TENANT && kind != RoleKind.TENANT_CUSTOM) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return kind;
    }

    private static IamAction action(AuthorizationDomain domain, boolean shared, AccessKind kind) {
        return switch (kind) {
            case READ -> shared ? IamAction.PLATFORM_SHARED_ROLE_READ
                    : domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_ROLE_READ : IamAction.TENANT_ROLE_READ;
            case CREATE -> shared ? IamAction.PLATFORM_SHARED_ROLE_CREATE
                    : domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_ROLE_CREATE : IamAction.TENANT_ROLE_CREATE;
            case STATUS -> shared ? IamAction.PLATFORM_SHARED_ROLE_STATUS
                    : domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_ROLE_STATUS : IamAction.TENANT_ROLE_STATUS;
            case DELETE -> shared ? IamAction.PLATFORM_SHARED_ROLE_DELETE
                    : domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_ROLE_DELETE : IamAction.TENANT_ROLE_DELETE;
            case PUBLISH -> shared ? IamAction.PLATFORM_SHARED_ROLE_PUBLISH
                    : domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_ROLE_PUBLISH : IamAction.TENANT_ROLE_PUBLISH;
            case PREVIEW -> shared ? IamAction.PLATFORM_SHARED_ROLE_PREVIEW
                    : domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_ROLE_PREVIEW : IamAction.TENANT_ROLE_PREVIEW;
        };
    }

    private static ConfigurationStatus statusOf(boolean enabled) {
        return enabled ? ConfigurationStatus.ENABLED : ConfigurationStatus.DISABLED;
    }

    private static String nextVersion(BigInteger current) {
        return current.add(BigInteger.ONE).toString();
    }

    private static String version(BigInteger value) {
        return value == null ? "0" : value.toString();
    }

    private static String text(BigInteger id) {
        return id == null ? null : IamIds.text(id.longValue());
    }

    private enum AccessKind {
        READ, CREATE, STATUS, DELETE, PUBLISH, PREVIEW
    }

    private record RoleRow(String name, RoleKind kind, boolean enabled, BigInteger version) {
    }

    private record RevisionData(String id, String baseRevisionId, List<ActionGrant> grants, List<RoleDelta> deltas,
                                List<RoleParameterDefinition> parameters, RoleMetadataOverrides metadata) {
    }
}
