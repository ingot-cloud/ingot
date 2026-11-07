package com.ingot.cloud.iam.application.role;

import com.ingot.cloud.iam.domain.role.RoleSynthesis;
import com.ingot.cloud.iam.infrastructure.cache.RoleSynthesisCache;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.application.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.application.catalog.CatalogFacade;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.RoleRepository;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleDefinitionEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleDeltaEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleGrantEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleParameterEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleRevisionEntity;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.authorization.ActionCapabilitySnapshot;
import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import com.ingot.cloud.iam.application.shared.ResourceDetailFactory;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.cloud.iam.application.catalog.model.ActionLookupRecord;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.domain.shared.ConfigurationStatus;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.cloud.iam.application.role.model.RoleDefinitionDraft;
import com.ingot.cloud.iam.domain.role.RoleDelta;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.cloud.iam.application.role.model.RoleMetadataOverrides;
import com.ingot.framework.commons.model.iam.RoleParameterDefinition;
import com.ingot.cloud.iam.application.role.model.RoleSummary;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.cloud.iam.application.assignment.model.ValidationIssue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>提供role用例共享的持久化装配与内部规则，事务边界由各用例保持。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
public class PlatformRoleOperations {

    static final TypeReference<List<ScopeExpression>> SCOPES = new TypeReference<>() {
    };

    static final TypeReference<RoleMetadataOverrides> METADATA = new TypeReference<>() {
    };

    static final String ROLE = "role";

    private final IamAccess access;

    private final AuthorizationAuditWriter audits;

    private final AuthorizationChangeNotifier changes;

    private final RoleSynthesisCache synthesis;

    private final RoleGrantValidator validator;

    private final RoleRepository roles;

    private final CatalogFacade catalog;

    private final TransactionTemplate transaction;

    private final com.ingot.cloud.iam.application.authorization.extension.RoleFieldPermissionService roleFields;

    private final com.ingot.cloud.iam.application.authorization.extension.ResourceFieldMetadata fieldMetadata;

    static final String REVISION_NAME_PREFIX = "v";

    enum AccessKind {

        READ, CREATE, STATUS, DELETE, PUBLISH, PREVIEW

    }

    record RoleRow(String name, RoleKind kind, boolean enabled, BigInteger version) {
    }

    record RevisionData(String id, String baseRevisionId, List<ActionGrant> grants, List<RoleDelta> deltas,
            List<RoleParameterDefinition> parameters, RoleMetadataOverrides metadata,
            Map<String, com.ingot.framework.commons.model.iam.ResourceFieldDefinition> fields) {
    }

    /**
     * 绑定身份、审计、失效通知、合成缓存、目录解析与角色表。
     * <p>
     * TransactionTemplate 无法由 Lombok 从 PlatformTransactionManager 直接生成，保留显式构造器。
     * </p>
     * @param access 当前身份
     * @param audits 同事务审计
     * @param changes 授权热缓存失效
     * @param synthesis 角色合成派生缓存
     * @param validator 操作与范围能力校验
     * @param roles 角色持久化
     * @param catalog 操作目录解析
     * @param transactionManager 同一数据源事务
     */
    public PlatformRoleOperations(IamAccess access, AuthorizationAuditWriter audits, AuthorizationChangeNotifier changes,
            RoleSynthesisCache synthesis, RoleGrantValidator validator,
            RoleRepository roles, CatalogFacade catalog,
            com.ingot.cloud.iam.application.authorization.extension.RoleFieldPermissionService roleFields,
            com.ingot.cloud.iam.application.authorization.extension.ResourceFieldMetadata fieldMetadata,
            PlatformTransactionManager transactionManager) {
        this.roleFields = roleFields;
        this.fieldMetadata = fieldMetadata;
        this.access = access;
        this.audits = audits;
        this.changes = changes;
        this.synthesis = synthesis;
        this.validator = validator;
        this.roles = roles;
        this.catalog = catalog;
        this.transaction = new TransactionTemplate(transactionManager);
    }
    IamAccess access() { return access; }

    AuthorizationAuditWriter audits() { return audits; }

    AuthorizationChangeNotifier changes() { return changes; }

    RoleSynthesisCache synthesis() { return synthesis; }

    RoleGrantValidator validator() { return validator; }

    RoleRepository roles() { return roles; }

    CatalogFacade catalog() { return catalog; }

    TransactionTemplate transaction() { return transaction; }

    com.ingot.cloud.iam.application.authorization.extension.RoleFieldPermissionService roleFields() { return roleFields; }

    com.ingot.cloud.iam.application.authorization.extension.ResourceFieldMetadata fieldMetadata() { return fieldMetadata; }

    static String normalizeName(String name, long revision) {
        return name == null || name.isBlank() ? REVISION_NAME_PREFIX + revision : name.strip();
    }

    long publishRevision(long roleId, RoleKind kind, long revision, String baseRevisionId,
            RoleDefinitionDraft definition, String versionName, String remark) {
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
        var fieldSnapshot = roleFields.freeze(kind, synthesized, definition.resourceFieldPermissions());
        long id = access.nextId();
        IamRoleRevisionEntity entity = new IamRoleRevisionEntity();
        entity.setId(BigInteger.valueOf(id));
        entity.setRoleId(BigInteger.valueOf(roleId));
        entity.setKind(kind);
        entity.setRevision(BigInteger.valueOf(revision));
        entity.setBaseRevisionId(baseRevisionId == null || baseRevisionId.isBlank() ? null
                : BigInteger.valueOf(IamIds.require(baseRevisionId)));
        entity.setMetadataOverrides(
                IamJsonCodec.object(definition.metadataOverrides() == null ? Map.of() : definition.metadataOverrides()));
        entity.setResourceFieldPermissions(IamJsonCodec.object(fieldSnapshot));
        entity.setVersionName(normalizeName(versionName, revision));
        entity.setRemark(remark);
        entity.setMetadataVersion(BigInteger.ZERO);
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
                row.setScopes(IamJsonCodec.array(delta.scopes()));
                roles.insertDelta(row);
            }
        }
        else {
            for (ActionGrant grant : definition.grants()) {
                IamRoleGrantEntity row = new IamRoleGrantEntity();
                row.setRevisionId(BigInteger.valueOf(id));
                row.setActionId(BigInteger.valueOf(IamIds.require(grant.actionId())));
                row.setScopes(IamJsonCodec.array(grant.scopes()));
                roles.insertGrant(row);
            }
        }
        return id;
    }

    /**
     * 按角色当前形态合成待发布定义：有共享基础时叠加差异，完整自定义角色直接使用自有授权。
     */
    RoleSynthesis.Result synthesize(RoleSummary role, RoleDefinitionDraft input) {
        String base = currentBase(IamIds.require(role.id()));
        return based(base) ? RoleSynthesis.synthesize(baseGrants(base), input.deltas())
                : RoleSynthesis.synthesize(input.grants(), List.of());
    }

    /**
     * 版本形态在角色创建时定型：有共享基础的角色只保存差异，完整自定义角色只保存自有授权。
     */
    static void requireShape(RoleKind kind, boolean based, RoleDefinitionDraft definition) {
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
    List<ActionGrant> baseGrants(String baseRevisionId) {
        long id = IamIds.require(baseRevisionId);
        IamRoleRevisionEntity row = roles.findRevision(id);
        if (row == null || row.getKind() != RoleKind.SHARED) {
            throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
        }
        return loadRevision(id).grants();
    }

    static boolean based(String baseRevisionId) {
        return baseRevisionId != null && !baseRevisionId.isBlank();
    }

    static AuthorizationDomain grantDomain(RoleKind kind) {
        return kind == RoleKind.PLATFORM_CUSTOM ? AuthorizationDomain.PLATFORM : AuthorizationDomain.TENANT;
    }

    ResourceDetail<RoleSummary> loadSummary(AuthorizationDomain domain, boolean shared, ActiveIdentity actor,
            long id) {
        return summaryDetail(domain, shared, requireDefinition(domain, shared, actor, id),
                summaryCapabilities(domain, shared, actor));
    }

    ActionCapabilitySnapshot summaryCapabilities(AuthorizationDomain domain, boolean shared, ActiveIdentity actor) {
        return domain == AuthorizationDomain.PLATFORM && !shared ? access.capabilities(actor,
                List.of(IamAction.PLATFORM_ROLE_READ, IamAction.PLATFORM_ROLE_STATUS, IamAction.PLATFORM_ROLE_DELETE,
                        IamAction.PLATFORM_ROLE_PUBLISH, IamAction.PLATFORM_ROLE_PREVIEW))
                : new ActionCapabilitySnapshot(Map.of());
    }

    ResourceDetail<RoleSummary> summaryDetail(AuthorizationDomain domain, boolean shared,
            IamRoleDefinitionEntity row, ActionCapabilitySnapshot permissions) {
        if (domain != AuthorizationDomain.PLATFORM || shared) {
            return ResourceDetailFactory.of(summary(row), version(row.getVersion()));
        }
        Map<String, com.ingot.cloud.iam.application.shared.model.ObjectCapability> capabilities = new LinkedHashMap<>();
        for (AccessKind kind : List.of(AccessKind.READ, AccessKind.STATUS, AccessKind.DELETE, AccessKind.PUBLISH,
                AccessKind.PREVIEW)) {
            IamAction operation = action(domain, false, kind);
            boolean allowed = (kind == AccessKind.READ || row.getKind() != RoleKind.SYSTEM)
                    && permissions.allows(operation, false);
            capabilities.put(operation.getCode(), new com.ingot.cloud.iam.application.shared.model.ObjectCapability(allowed,
                    allowed ? null : IamReasonCode.ACTION_DENIED, allowed ? null : "当前角色不可执行此操作"));
        }
        return ResourceDetailFactory.of(summary(row), capabilities, version(row.getVersion()));
    }

    RoleRow lock(AuthorizationDomain domain, boolean shared, ActiveIdentity actor, long id) {
        IamRoleDefinitionEntity row = roles.lockDefinition(domain, shared, tenantId(domain, actor), id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return new RoleRow(row.getName(), row.getKind(), Boolean.TRUE.equals(row.getEnabled()), row.getVersion());
    }

    RevisionData latestRevision(long roleId) {
        IamRoleRevisionEntity row = roles.latestRevision(roleId);
        if (row == null || row.getId() == null) {
            throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
        }
        return loadRevision(row.getId().longValue());
    }

    /**
     * 读取角色当前形态的共享基础；完整自定义角色为空。
     */
    String currentBase(long roleId) {
        return latestRevision(roleId).baseRevisionId();
    }

    RevisionData loadRevision(long revisionId) {
        IamRoleRevisionEntity row = roles.findRevision(revisionId);
        if (row == null) {
            throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
        }
        return loadRevision(row);
    }

    RevisionData loadRevision(IamRoleRevisionEntity row) {
        long id = row.getId().longValueExact();
        return revisionData(row, roles.listGrants(id), roles.listDeltas(id), roles.listParameters(id));
    }

    RevisionData revisionData(IamRoleRevisionEntity row,
            List<IamRoleGrantEntity> grants,
            List<IamRoleDeltaEntity> deltas,
            List<IamRoleParameterEntity> parameters) {
        var parameterRows = parameters.stream()
                .map(item -> new RoleParameterDefinition(item.getParameterKey(), item.getBindingKind())).toList();
        return new RevisionData(text(row.getId()), text(row.getBaseRevisionId()), grantRows(grants), deltaRows(deltas),
                parameterRows, IamJsonCodec.read(row.getMetadataOverrides(), METADATA),
                com.ingot.cloud.iam.application.authorization.extension.RoleFieldPermissionService.snapshot(row.getResourceFieldPermissions()));
    }

    List<ActionGrant> grantRows(List<IamRoleGrantEntity> rows) {
        return rows.stream().map(item -> new ActionGrant(text(item.getActionId()),
                IamJsonCodec.read(item.getScopes(), SCOPES))).toList();
    }

    List<RoleDelta> deltaRows(List<IamRoleDeltaEntity> rows) {
        return rows.stream().map(item -> new RoleDelta(text(item.getActionId()), item.getOperation(),
                IamJsonCodec.read(item.getScopes(), SCOPES))).toList();
    }

    static Map<String, ActionLookupRecord> indexActions(List<ActionLookupRecord> records) {
        Map<String, ActionLookupRecord> result = new LinkedHashMap<>();
        for (ActionLookupRecord record : records) {
            result.putIfAbsent(record.id(), record);
        }
        return result;
    }

    static String blankToEmpty(String value, String fallback) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        return fallback == null ? "" : fallback;
    }

    static RoleSummary summary(IamRoleDefinitionEntity row) {
        return new RoleSummary(text(row.getId()), row.getCode(), row.getName(), row.getDescription(),
                row.getGroupName(), row.getKind(), statusOf(Boolean.TRUE.equals(row.getEnabled())));
    }

    static Long tenantId(AuthorizationDomain domain, ActiveIdentity actor) {
        if (domain != AuthorizationDomain.TENANT) {
            return null;
        }
        return actor == null ? 0L : IamIds.require(actor.context().tenantId());
    }

    static IamAction action(AuthorizationDomain domain, boolean shared, AccessKind kind) {
        return switch (kind) {
            case READ -> shared ? IamAction.PLATFORM_SHARED_ROLE_READ : domain == AuthorizationDomain.PLATFORM
                    ? IamAction.PLATFORM_ROLE_READ : IamAction.TENANT_ROLE_READ;
            case CREATE -> shared ? IamAction.PLATFORM_SHARED_ROLE_CREATE : domain == AuthorizationDomain.PLATFORM
                    ? IamAction.PLATFORM_ROLE_CREATE : IamAction.TENANT_ROLE_CREATE;
            case STATUS -> shared ? IamAction.PLATFORM_SHARED_ROLE_STATUS : domain == AuthorizationDomain.PLATFORM
                    ? IamAction.PLATFORM_ROLE_STATUS : IamAction.TENANT_ROLE_STATUS;
            case DELETE -> shared ? IamAction.PLATFORM_SHARED_ROLE_DELETE : domain == AuthorizationDomain.PLATFORM
                    ? IamAction.PLATFORM_ROLE_DELETE : IamAction.TENANT_ROLE_DELETE;
            case PUBLISH -> shared ? IamAction.PLATFORM_SHARED_ROLE_UPDATE : domain == AuthorizationDomain.PLATFORM
                    ? IamAction.PLATFORM_ROLE_PUBLISH : IamAction.TENANT_ROLE_UPDATE;
            case PREVIEW -> shared ? IamAction.PLATFORM_SHARED_ROLE_PREVIEW : domain == AuthorizationDomain.PLATFORM
                    ? IamAction.PLATFORM_ROLE_PREVIEW : IamAction.TENANT_ROLE_PREVIEW;
        };
    }

    static ConfigurationStatus statusOf(boolean enabled) {
        return enabled ? ConfigurationStatus.ENABLED : ConfigurationStatus.DISABLED;
    }

    static String nextVersion(BigInteger current) {
        return current.add(BigInteger.ONE).toString();
    }

    static String version(BigInteger value) {
        return value == null ? "0" : value.toString();
    }

    static String text(BigInteger id) {
        return id == null ? null : IamIds.text(id.longValue());
    }

    /**
     * 读取当前入口的角色定义，不装配展示能力；调用方仍须完成操作准入和原有对象范围门禁。
     * @param domain 可信接口域
     * @param shared 是否共享入口
     * @param actor 已准入身份
     * @param id 角色 ID
     * @return 同域同租户角色定义
     */
    IamRoleDefinitionEntity requireDefinition(AuthorizationDomain domain, boolean shared, ActiveIdentity actor,
            long id) {
        IamRoleDefinitionEntity row = roles.findDefinition(domain, shared, tenantId(domain, actor), id);
        if (row == null) throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        return row;
    }
}
