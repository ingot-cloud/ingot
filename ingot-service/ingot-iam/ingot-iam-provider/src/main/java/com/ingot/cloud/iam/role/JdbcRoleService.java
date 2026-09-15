package com.ingot.cloud.iam.role;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.support.IamAccess;
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
import com.ingot.framework.commons.model.iam.ScopeBindingKind;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.UpgradeInput;
import com.ingot.framework.commons.model.iam.UpgradePreview;
import com.ingot.framework.commons.model.iam.UpgradePreviewInput;
import com.ingot.framework.commons.model.iam.UsageSummary;
import com.ingot.framework.commons.model.iam.ValidationIssue;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
public class JdbcRoleService {
    private static final TypeReference<List<ScopeExpression>> SCOPES = new TypeReference<>() {
    };
    private static final TypeReference<RoleMetadataOverrides> METADATA = new TypeReference<>() {
    };
    private static final String ROLE = "role";
    private final IamAccess access;
    private final IamAuditWriter audits;
    private final AuthorizationChangeNotifier changes;
    private final RoleSynthesisCache synthesis;
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、审计、失效通知、合成缓存与角色表。
     *
     * @param access 当前身份
     * @param audits 同事务审计
     * @param changes 授权热缓存失效
     * @param synthesis 角色合成派生缓存
     * @param dataSource IAM 目标库
     * @param transactionManager 同一数据源事务
     */
    public JdbcRoleService(IamAccess access, IamAuditWriter audits, AuthorizationChangeNotifier changes,
                           RoleSynthesisCache synthesis, DataSource dataSource,
                           PlatformTransactionManager transactionManager) {
        this.access = access;
        this.audits = audits;
        this.changes = changes;
        this.synthesis = synthesis;
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 列出当前路径可见的角色目录。
     *
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param page 页码
     * @param pageSize 页大小
     * @return 角色页
     */
    public PageResponse<ResourceDetail<RoleSummary>> list(AuthorizationDomain domain, boolean shared,
                                                          int page, int pageSize) {
        ActiveIdentity actor = access.require(domain, action(domain, shared, AccessKind.READ));
        IamPages.require(page, pageSize);
        Map<String, Object> parameters = listParameters(domain, shared, actor);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_definition WHERE " + listPredicate(domain, shared),
                parameters, Long.class);
        parameters.put("limit", pageSize);
        parameters.put("offset", IamPages.offset(page, pageSize));
        List<ResourceDetail<RoleSummary>> items = jdbc.query("""
                SELECT id,code,name,description,group_name,kind,enabled,version FROM iam_role_definition
                 WHERE %s ORDER BY id LIMIT :limit OFFSET :offset
                """.formatted(listPredicate(domain, shared)), parameters,
                (row, index) -> IamDetails.of(summary(row), row.getString("version")));
        return IamPages.details(items, total == null ? 0 : total, page, pageSize);
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
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("id", id);
            parameters.put("domain", kind == RoleKind.PLATFORM_CUSTOM
                    ? AuthorizationDomain.PLATFORM.name() : AuthorizationDomain.TENANT.name());
            parameters.put("tenantId", tenantId);
            parameters.put("kind", kind.name());
            parameters.put("code", input.code());
            parameters.put("name", input.name());
            parameters.put("description", input.description());
            parameters.put("groupName", input.groupName());
            try {
                jdbc.update("""
                        INSERT INTO iam_role_definition(id,domain,tenant_id,kind,code,name,description,group_name,enabled)
                        VALUES (:id,:domain,:tenantId,:kind,:code,:name,:description,:groupName,TRUE)
                        """, parameters);
            } catch (DuplicateKeyException exception) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
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
            IamIds.requireVersion(input.expectedVersion(), current.version());
            boolean enabled = input.status() == ConfigurationStatus.ENABLED;
            jdbc.update("UPDATE iam_role_definition SET enabled=:enabled,version=version+1 WHERE id=:id",
                    Map.of("enabled", enabled, "id", roleId));
            String version = Long.toString(Long.parseLong(current.version()) + 1);
            audits.write(actor.context(), access.nextId(), ROLE, id,
                    enabled ? AuditChangeType.ENABLE : AuditChangeType.DISABLE,
                    Map.of(AuditField.STATUS, statusOf(current.enabled()).name()),
                    Map.of(AuditField.STATUS, input.status().name()), Map.of(ROLE, version));
            changes.markAll();
            return new CreatedResource(id, version);
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
            Long used = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM iam_role_assignment a JOIN iam_role_revision r ON r.id=a.revision_id
                     WHERE r.role_id=:id
                    """, Map.of("id", roleId), Long.class);
            if (used != null && used > 0) {
                throw new BizException(IamReasonCode.OBJECT_IN_USE);
            }
            jdbc.update("DELETE FROM iam_role_parameter WHERE revision_id IN (SELECT id FROM iam_role_revision WHERE role_id=:id)",
                    Map.of("id", roleId));
            jdbc.update("DELETE FROM iam_role_grant WHERE revision_id IN (SELECT id FROM iam_role_revision WHERE role_id=:id)",
                    Map.of("id", roleId));
            jdbc.update("DELETE FROM iam_role_delta WHERE revision_id IN (SELECT id FROM iam_role_revision WHERE role_id=:id)",
                    Map.of("id", roleId));
            jdbc.update("DELETE FROM iam_role_revision WHERE role_id=:id", Map.of("id", roleId));
            jdbc.update("DELETE FROM iam_role_definition WHERE id=:id", Map.of("id", roleId));
            audits.write(actor.context(), access.nextId(), ROLE, id, AuditChangeType.REMOVE,
                    Map.of(AuditField.NAME, current.name()), Map.of(), Map.of(ROLE, current.version()));
            changes.markAll();
            return new CreatedResource(id, current.version());
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
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_revision WHERE role_id=:id",
                Map.of("id", roleId), Long.class);
        List<ResourceDetail<RoleRevision>> items = jdbc.query("""
                SELECT id,role_id,kind,revision,base_revision_id,metadata_overrides
                  FROM iam_role_revision WHERE role_id=:id ORDER BY revision DESC,id DESC
                 LIMIT :limit OFFSET :offset
                """, Map.of("id", roleId, "limit", pageSize, "offset", IamPages.offset(page, pageSize)),
                (row, index) -> IamDetails.of(revision(row), row.getString("revision")));
        return IamPages.details(items, total == null ? 0 : total, page, pageSize);
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
            IamIds.requireVersion(input.expectedVersion(), current.version());
            Long last = jdbc.queryForObject(
                    "SELECT COALESCE(MAX(revision),0) FROM iam_role_revision WHERE role_id=:id",
                    Map.of("id", roleId), Long.class);
            String baseId = current.kind() == RoleKind.TENANT_CUSTOM ? latestBase(roleId) : null;
            long revisionId = publishRevision(roleId, current.kind(), (last == null ? 0 : last) + 1, baseId, input.definition());
            jdbc.update("UPDATE iam_role_definition SET version=version+1 WHERE id=:id", Map.of("id", roleId));
            audits.write(actor.context(), access.nextId(), ROLE, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.ROLE_REVISION, String.valueOf(last)),
                    Map.of(AuditField.ROLE_REVISION, IamIds.text(revisionId)),
                    Map.of(ROLE, Long.toString(Long.parseLong(current.version()) + 1)));
            return new CreatedResource(IamIds.text(revisionId), Long.toString((last == null ? 0 : last) + 1));
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
            effective = effective(role.record(), input);
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
        RevisionData oldBase = loadRevision(IamIds.require(current.baseRevisionId()));
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
            IamIds.requireVersion(input.expectedVersion(), current.version());
            RevisionData latest = latestRevision(roleId);
            RevisionData oldBase = loadRevision(IamIds.require(latest.baseRevisionId()));
            RevisionData newBase = loadRevision(IamIds.require(input.newBaseRevisionId()));
            RoleSynthesis.UpgradePlan plan = RoleSynthesis.upgrade(oldBase.grants(), newBase.grants(), latest.deltas(),
                    input.resolutions());
            if (!plan.conflicts().isEmpty()) {
                throw new BizException(IamReasonCode.POLICY_CONFLICT);
            }
            Long last = jdbc.queryForObject(
                    "SELECT COALESCE(MAX(revision),0) FROM iam_role_revision WHERE role_id=:id",
                    Map.of("id", roleId), Long.class);
            RoleDefinitionDraft definition = new RoleDefinitionDraft(List.of(), plan.nextDeltas(),
                    latest.parameters(), latest.metadata());
            long revisionId = publishRevision(roleId, RoleKind.TENANT_CUSTOM, (last == null ? 0 : last) + 1,
                    input.newBaseRevisionId(), definition);
            for (String assignmentId : input.assignmentIds()) {
                jdbc.update("UPDATE iam_role_assignment SET revision_id=:revisionId,version=version+1 WHERE id=:id",
                        Map.of("revisionId", revisionId, "id", IamIds.require(assignmentId)));
            }
            jdbc.update("UPDATE iam_role_definition SET version=version+1 WHERE id=:id", Map.of("id", roleId));
            audits.write(actor.context(), access.nextId(), ROLE, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.ROLE_REVISION, latest.id()), Map.of(AuditField.ROLE_REVISION, IamIds.text(revisionId)),
                    Map.of(ROLE, Long.toString(Long.parseLong(current.version()) + 1)));
            changes.markAll();
            return new CreatedResource(IamIds.text(revisionId), Long.toString((last == null ? 0 : last) + 1));
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
        if (kind == RoleKind.TENANT_CUSTOM && (baseRevisionId == null || baseRevisionId.isBlank()
                || definition.grants() == null || !definition.grants().isEmpty())) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        if (kind != RoleKind.TENANT_CUSTOM && definition.deltas() != null && !definition.deltas().isEmpty()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        if (kind != RoleKind.TENANT_CUSTOM) {
            RoleSynthesis.synthesize(definition.grants(), List.of());
        } else {
            RevisionData base = loadRevision(IamIds.require(baseRevisionId));
            RoleSynthesis.synthesize(base.grants(), definition.deltas());
        }
        long id = access.nextId();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", id);
        parameters.put("roleId", roleId);
        parameters.put("kind", kind.name());
        parameters.put("revision", revision);
        parameters.put("baseRevisionId", baseRevisionId == null || baseRevisionId.isBlank()
                ? null : IamIds.require(baseRevisionId));
        parameters.put("metadata", IamJson.object(definition.metadataOverrides() == null
                ? Map.of() : definition.metadataOverrides()));
        parameters.put("publishedAt", Timestamp.from(Instant.now()));
        jdbc.update("""
                INSERT INTO iam_role_revision(id,role_id,kind,revision,base_revision_id,metadata_overrides,published_at)
                VALUES (:id,:roleId,:kind,:revision,:baseRevisionId,:metadata,:publishedAt)
                """, parameters);
        if (definition.parameterDefinitions() != null) {
            for (RoleParameterDefinition parameter : definition.parameterDefinitions()) {
                jdbc.update("""
                        INSERT INTO iam_role_parameter(revision_id,parameter_key,binding_kind)
                        VALUES (:revisionId,:key,:kind)
                        """, Map.of("revisionId", id, "key", parameter.key(), "kind", parameter.kind().name()));
            }
        }
        if (kind == RoleKind.TENANT_CUSTOM) {
            for (RoleDelta delta : definition.deltas()) {
                jdbc.update("""
                        INSERT INTO iam_role_delta(revision_id,action_id,operation,scopes)
                        VALUES (:revisionId,:actionId,:operation,:scopes)
                        """, Map.of("revisionId", id, "actionId", IamIds.require(delta.actionId()),
                        "operation", delta.operation().name(), "scopes", IamJson.array(delta.scopes())));
            }
        } else {
            for (ActionGrant grant : definition.grants()) {
                jdbc.update("""
                        INSERT INTO iam_role_grant(revision_id,action_id,scopes)
                        VALUES (:revisionId,:actionId,:scopes)
                        """, Map.of("revisionId", id, "actionId", IamIds.require(grant.actionId()),
                        "scopes", IamJson.array(grant.scopes())));
            }
        }
        return id;
    }

    private EffectiveRole effective(RoleSummary role, RoleDefinitionDraft input) {
        RoleSynthesis.Result synthesized;
        String baseId = null;
        if (role.kind() == RoleKind.TENANT_CUSTOM) {
            String latest = latestBase(IamIds.require(role.id()));
            baseId = latest;
            synthesized = RoleSynthesis.synthesize(loadRevision(IamIds.require(latest)).grants(), input.deltas());
        } else {
            synthesized = RoleSynthesis.synthesize(input.grants(), List.of());
        }
        return new EffectiveRole(role, new RoleRevisionRef(role.kind(), role.id()), synthesized.grants(),
                synthesized.origins(), input.parameterDefinitions() == null ? List.of() : input.parameterDefinitions(),
                new UsageSummary(0L, 0L, false));
    }

    private ResourceDetail<RoleSummary> loadSummary(AuthorizationDomain domain, boolean shared, ActiveIdentity actor, long id) {
        Map<String, Object> parameters = listParameters(domain, shared, actor);
        parameters.put("id", id);
        List<ResourceDetail<RoleSummary>> rows = jdbc.query("""
                SELECT id,code,name,description,group_name,kind,enabled,version FROM iam_role_definition
                 WHERE id=:id AND %s
                """.formatted(listPredicate(domain, shared)), parameters,
                (row, index) -> IamDetails.of(summary(row), row.getString("version")));
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return rows.getFirst();
    }

    private RoleRow lock(AuthorizationDomain domain, boolean shared, ActiveIdentity actor, long id) {
        Map<String, Object> parameters = listParameters(domain, shared, actor);
        parameters.put("id", id);
        List<RoleRow> rows = jdbc.query("""
                SELECT id,name,kind,enabled,version FROM iam_role_definition
                 WHERE id=:id AND %s FOR UPDATE
                """.formatted(listPredicate(domain, shared)), parameters,
                (row, index) -> new RoleRow(row.getString("name"), RoleKind.valueOf(row.getString("kind")),
                        row.getBoolean("enabled"), row.getString("version")));
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return rows.getFirst();
    }

    private RevisionData latestRevision(long roleId) {
        List<Long> ids = jdbc.queryForList(
                "SELECT id FROM iam_role_revision WHERE role_id=:id ORDER BY revision DESC,id DESC",
                Map.of("id", roleId), Long.class);
        if (ids.isEmpty()) {
            throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
        }
        return loadRevision(ids.getFirst());
    }

    private String latestBase(long roleId) {
        List<String> bases = jdbc.query("""
                SELECT base_revision_id FROM iam_role_revision
                 WHERE role_id=:id AND base_revision_id IS NOT NULL ORDER BY revision DESC,id DESC
                """, Map.of("id", roleId), (row, index) -> row.getString("base_revision_id"));
        if (bases.isEmpty() || bases.getFirst() == null) {
            throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
        }
        return bases.getFirst();
    }

    private RevisionData loadRevision(long revisionId) {
        List<RevisionData> rows = jdbc.query("""
                SELECT id,role_id,kind,revision,base_revision_id,metadata_overrides FROM iam_role_revision WHERE id=:id
                """, Map.of("id", revisionId), (row, index) -> {
            List<ActionGrant> grants = jdbc.query(
                    "SELECT action_id,scopes FROM iam_role_grant WHERE revision_id=:id", Map.of("id", revisionId),
                    (item, i) -> new ActionGrant(item.getString("action_id"),
                            IamJson.read(item.getString("scopes"), SCOPES)));
            List<RoleDelta> deltas = jdbc.query(
                    "SELECT action_id,operation,scopes FROM iam_role_delta WHERE revision_id=:id", Map.of("id", revisionId),
                    (item, i) -> new RoleDelta(item.getString("action_id"),
                            com.ingot.framework.commons.model.iam.RoleDeltaOperation.valueOf(item.getString("operation")),
                            IamJson.read(item.getString("scopes"), SCOPES)));
            List<RoleParameterDefinition> parameters = jdbc.query(
                    "SELECT parameter_key,binding_kind FROM iam_role_parameter WHERE revision_id=:id",
                    Map.of("id", revisionId),
                    (item, i) -> new RoleParameterDefinition(item.getString("parameter_key"),
                            ScopeBindingKind.valueOf(item.getString("binding_kind"))));
            RoleMetadataOverrides metadata = IamJson.read(row.getString("metadata_overrides"), METADATA);
            return new RevisionData(row.getString("id"), row.getString("base_revision_id"), grants, deltas,
                    parameters, metadata);
        });
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
        }
        return rows.getFirst();
    }

    private RoleRevision revision(java.sql.ResultSet row) throws java.sql.SQLException {
        RevisionData data = loadRevision(row.getLong("id"));
        return new RoleRevision(row.getString("id"), row.getString("role_id"), row.getString("revision"),
                RoleKind.valueOf(row.getString("kind")), row.getString("base_revision_id"),
                data.grants(), data.deltas(), data.parameters(), data.metadata());
    }

    private static RoleSummary summary(java.sql.ResultSet row) throws java.sql.SQLException {
        return new RoleSummary(row.getString("id"), row.getString("code"), row.getString("name"),
                row.getString("description"), row.getString("group_name"),
                RoleKind.valueOf(row.getString("kind")), statusOf(row.getBoolean("enabled")));
    }

    private Map<String, Object> listParameters(AuthorizationDomain domain, boolean shared, ActiveIdentity actor) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("domain", domain.name());
        if (domain == AuthorizationDomain.TENANT && actor != null) {
            parameters.put("tenantId", IamIds.require(actor.context().tenantId()));
        } else if (domain == AuthorizationDomain.TENANT) {
            parameters.put("tenantId", 0L);
        }
        parameters.put("shared", RoleKind.SHARED.name());
        parameters.put("system", RoleKind.SYSTEM.name());
        parameters.put("platformCustom", RoleKind.PLATFORM_CUSTOM.name());
        parameters.put("tenantCustom", RoleKind.TENANT_CUSTOM.name());
        return parameters;
    }

    private String listPredicate(AuthorizationDomain domain, boolean shared) {
        if (shared) {
            return "kind=:shared AND tenant_id IS NULL";
        }
        if (domain == AuthorizationDomain.PLATFORM) {
            return "((kind=:system AND domain=:domain) OR kind=:platformCustom)";
        }
        return "((kind=:system AND domain=:domain AND tenant_id IS NULL) OR kind=:shared OR (kind=:tenantCustom AND tenant_id=:tenantId))";
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

    private enum AccessKind {
        READ, CREATE, STATUS, DELETE, PUBLISH, PREVIEW
    }

    private record RoleRow(String name, RoleKind kind, boolean enabled, String version) {
    }

    private record RevisionData(String id, String baseRevisionId, List<ActionGrant> grants, List<RoleDelta> deltas,
                                List<RoleParameterDefinition> parameters, RoleMetadataOverrides metadata) {
    }
}
