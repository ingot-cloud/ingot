package com.ingot.cloud.iam.application.role;

import com.ingot.cloud.iam.domain.role.RoleRevisionSnapshot;
import java.util.stream.Collectors;
import java.math.BigInteger;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleDeltaEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleGrantEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleParameterEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleRevisionEntity;
import com.ingot.cloud.iam.application.shared.ResourceDetailFactory;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.application.shared.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.cloud.iam.application.catalog.model.ActionLookupRecord;
import com.ingot.cloud.iam.domain.audit.AuditChangeType;
import com.ingot.cloud.iam.domain.audit.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.cloud.iam.domain.role.RoleDelta;
import com.ingot.cloud.iam.domain.role.RoleDeltaOperation;
import com.ingot.cloud.iam.application.role.model.RoleDisplayDelta;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.cloud.iam.application.role.model.RolePublishInput;
import com.ingot.cloud.iam.application.role.model.RoleRevision;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * <p>执行对应平台角色用例，保留固定版本、字段权限和乐观锁规则。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PlatformRoleRevisionService {
    private final PlatformRoleOperations operationsContext;

    /**
     * 列出角色不可变版本。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 版本页
     */
    public PageResponse<ResourceDetail<RoleRevision>> listRevisions(AuthorizationDomain domain, boolean shared,
            String id, int page, int pageSize) {
        ActiveIdentity actor = operationsContext.access().require(domain, PlatformRoleOperations.action(domain, shared, PlatformRoleOperations.AccessKind.READ));
        long roleId = IamIds.require(id);
        operationsContext.requireDefinition(domain, shared, actor, roleId);
        IamPages.require(page, pageSize);
        Page<IamRoleRevisionEntity> rows = operationsContext.roles().pageRevisions(roleId, page, pageSize);
        List<IamRoleRevisionEntity> records = rows.getRecords();
        IamRoleRevisionEntity previousRow = olderRevisionRow(roleId, records);
        List<IamRoleRevisionEntity> all = new ArrayList<>(records);
        if (previousRow != null) all.add(previousRow);
        var data = loadRevisions(all);
        List<RoleRevision> revisions = records.stream().map(row -> revision(row, data.get(row.getId()))).toList();
        RoleRevision older = previousRow == null ? null : revision(previousRow, data.get(previousRow.getId()));
        Set<String> actionIds = new LinkedHashSet<>();
        for (int index = 0; index < revisions.size(); index++) {
            RoleRevision current = revisions.get(index);
            RoleRevision previous = index + 1 < revisions.size() ? revisions.get(index + 1) : older;
            for (RoleDelta delta : rawDisplayDeltas(current, previous)) {
                if (delta.actionId() != null && !delta.actionId().isBlank()) {
                    actionIds.add(delta.actionId());
                }
            }
        }
        Map<String, ActionLookupRecord> actions = PlatformRoleOperations.indexActions(operationsContext.catalog().resolveActions(List.copyOf(actionIds)));
        List<ResourceDetail<RoleRevision>> items = new ArrayList<>();
        for (int index = 0; index < revisions.size(); index++) {
            RoleRevision current = revisions.get(index);
            RoleRevision previous = index + 1 < revisions.size() ? revisions.get(index + 1) : older;
            items.add(ResourceDetailFactory.of(withDisplayDeltas(current, previous, displayDeltas(current, previous, actions)),
                    PlatformRoleOperations.version(records.get(index).getRevision())));
        }
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    /** 读取同角色的固定版本，受当前域与对象查看权限约束。 */
    public ResourceDetail<RoleRevision> getRevision(AuthorizationDomain domain, boolean shared, String id, String revisionId) {
        var actor = operationsContext.access().require(domain, PlatformRoleOperations.action(domain, shared, PlatformRoleOperations.AccessKind.READ));
        long roleId = IamIds.require(id);
        operationsContext.loadSummary(domain, shared, actor, roleId);
        var row = operationsContext.roles().findRevision(IamIds.require(revisionId));
        if (row == null || row.getRoleId().longValueExact() != roleId)
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        return ResourceDetailFactory.of(revision(row), PlatformRoleOperations.version(row.getRevision()));
    }

    /** 修改非系统角色的版本说明；不改变权限定义或清理授权缓存。 */
    public CreatedResource updateRevisionMetadata(AuthorizationDomain domain, boolean shared, String id, String revisionId,
            com.ingot.cloud.iam.application.role.model.RoleRevisionMetadataInput input) {
        operationsContext.access().require(domain, PlatformRoleOperations.action(domain, shared, PlatformRoleOperations.AccessKind.STATUS));
        return operationsContext.transaction().execute(status -> {
            operationsContext.roles().lockAuthorization(domain);
            var actor = operationsContext.access().require(domain, PlatformRoleOperations.action(domain, shared, PlatformRoleOperations.AccessKind.STATUS));
            long roleId = IamIds.require(id);
            var role = operationsContext.lock(domain, shared, actor, roleId);
            if (role.kind() == RoleKind.SYSTEM) throw new BizException(IamReasonCode.ACTION_DENIED);
            var row = operationsContext.roles().findRevision(IamIds.require(revisionId));
            if (row == null || row.getRoleId().longValueExact() != roleId)
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            IamIds.requireVersion(input.expectedMetadataVersion(), PlatformRoleOperations.version(row.getMetadataVersion()));
            String name = PlatformRoleOperations.normalizeName(input.versionName(), row.getRevision().longValueExact());
            if (operationsContext.roles().updateRevisionMetadata(row.getId().longValueExact(), name, input.remark(), row.getMetadataVersion()) != 1)
                throw new BizException(IamReasonCode.REVISION_CONFLICT);
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), PlatformRoleOperations.ROLE, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, PlatformRoleOperations.normalizeName(row.getVersionName(), row.getRevision().longValueExact()),
                            AuditField.REMARK, row.getRemark() == null ? "" : row.getRemark()),
                    Map.of(AuditField.NAME, name, AuditField.REMARK, input.remark() == null ? "" : input.remark()), Map.of(PlatformRoleOperations.ROLE, input.expectedMetadataVersion()));
            return new CreatedResource(revisionId, row.getMetadataVersion().add(BigInteger.ONE).toString());
        });
    }

    /**
     * 发布新版本，不改写既有授权引用。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @param input 待发布定义
     * @return 新版本 ID
     */
    public CreatedResource publish(AuthorizationDomain domain, boolean shared, String id, RolePublishInput input) {
        ActiveIdentity actor = operationsContext.access().require(domain, PlatformRoleOperations.action(domain, shared, PlatformRoleOperations.AccessKind.PUBLISH));
        long roleId = IamIds.require(id);
        return operationsContext.transaction().execute(status -> {
            operationsContext.roles().lockAuthorization(domain);
            PlatformRoleOperations.RoleRow current = operationsContext.lock(domain, shared, actor, roleId);
            if (current.kind() == RoleKind.SYSTEM) {
                throw new BizException(IamReasonCode.ACTION_DENIED);
            }
            IamIds.requireVersion(input.expectedVersion(), PlatformRoleOperations.version(current.version()));
            IamRoleRevisionEntity previous = operationsContext.roles().latestRevision(roleId);
            long last = previous == null || previous.getRevision() == null ? 0L : previous.getRevision().longValue();
            String baseId = current.kind() == RoleKind.TENANT_CUSTOM ? operationsContext.currentBase(roleId) : null;
            if (input.sourceRevisionId() != null) {
                var source = operationsContext.roles().findRevision(IamIds.require(input.sourceRevisionId()));
                if (source == null || source.getRoleId().longValueExact() != roleId)
                    throw new BizException(IamReasonCode.INVALID_ARGUMENT);
                baseId = PlatformRoleOperations.text(source.getBaseRevisionId());
            }
            long revisionId = operationsContext.publishRevision(roleId, current.kind(), last + 1, baseId, input.definition(), input.versionName(), input.remark());
            operationsContext.roles().incrementVersion(roleId, current.version());
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), PlatformRoleOperations.ROLE, id, AuditChangeType.UPDATE,
                    previous == null ? Map.of() : Map.of(AuditField.ROLE_REVISION, previous.getId().toString()),
                    Map.of(AuditField.ROLE_REVISION, IamIds.text(revisionId)),
                    Map.of(PlatformRoleOperations.ROLE, PlatformRoleOperations.nextVersion(current.version())));
            return new CreatedResource(IamIds.text(revisionId), Long.toString(last + 1));
        });
    }

    /**
     * 合成指定版本的有效授权，供求值引擎复用。
     * @param revisionId 版本 ID
     * @return 合成后的操作范围
     */
    public List<ActionGrant> synthesizedGrants(long revisionId) {
        PlatformRoleOperations.RevisionData revision = operationsContext.loadRevision(revisionId);
        RoleRevisionSnapshot snapshot;
        if (revision.baseRevisionId() == null) {
            snapshot = new RoleRevisionSnapshot(revisionId, null, revision.grants(), List.of());
        }
        else {
            PlatformRoleOperations.RevisionData base = operationsContext.loadRevision(IamIds.require(revision.baseRevisionId()));
            snapshot = new RoleRevisionSnapshot(revisionId, IamIds.require(revision.baseRevisionId()), base.grants(),
                    revision.deltas());
        }
        return operationsContext.synthesis().grants(snapshot);
    }

    private Map<BigInteger, PlatformRoleOperations.RevisionData> loadRevisions(List<IamRoleRevisionEntity> rows) {
        if (rows.isEmpty()) return Map.of();
        var ids = rows.stream().map(IamRoleRevisionEntity::getId).distinct().toList();
        var grants = operationsContext.roles().listGrants(ids).stream().collect(Collectors.groupingBy(
                IamRoleGrantEntity::getRevisionId));
        var deltas = operationsContext.roles().listDeltas(ids).stream().collect(Collectors.groupingBy(
                IamRoleDeltaEntity::getRevisionId));
        var parameters = operationsContext.roles().listParameters(ids).stream().collect(Collectors.groupingBy(
                IamRoleParameterEntity::getRevisionId));
        Map<BigInteger, PlatformRoleOperations.RevisionData> result = new LinkedHashMap<>();
        rows.forEach(row -> result.put(row.getId(), operationsContext.revisionData(row, grants.getOrDefault(row.getId(), List.of()),
                deltas.getOrDefault(row.getId(), List.of()), parameters.getOrDefault(row.getId(), List.of()))));
        return result;
    }

    private RoleRevision revision(IamRoleRevisionEntity row) {
        return revision(row, operationsContext.loadRevision(row.getId().longValue()));
    }

    private RoleRevision revision(IamRoleRevisionEntity row, PlatformRoleOperations.RevisionData data) {
        return new RoleRevision(PlatformRoleOperations.text(row.getId()), PlatformRoleOperations.text(row.getRoleId()), PlatformRoleOperations.version(row.getRevision()), row.getKind(),
                PlatformRoleOperations.text(row.getBaseRevisionId()), data.grants(), data.deltas(), data.parameters(), data.metadata(),
                List.of(), data.fields(), null, PlatformRoleOperations.normalizeName(row.getVersionName(), row.getRevision().longValueExact()),
                row.getRemark(), PlatformRoleOperations.version(row.getMetadataVersion()),
                row.getPublishedAt() == null ? null : row.getPublishedAt() == null ? null : row.getPublishedAt().toInstant(ZoneOffset.UTC));
    }

    private IamRoleRevisionEntity olderRevisionRow(long roleId, List<IamRoleRevisionEntity> records) {
        IamRoleRevisionEntity last = records.isEmpty() ? null : records.getLast();
        if (last == null || last.getRevision() == null || last.getRevision().longValue() <= 1) {
            return null;
        }
        IamRoleRevisionEntity previous = operationsContext.roles().findPreviousRevision(roleId, last.getRevision().longValue());
        return previous;
    }

    private static List<RoleDelta> rawDisplayDeltas(RoleRevision current, RoleRevision previous) {
        if (current.deltas() != null && !current.deltas().isEmpty()) {
            return current.deltas();
        }
        if (previous == null) {
            return List.of();
        }
        return grantDeltas(current.grants(), previous.grants());
    }

    private static List<RoleDisplayDelta> displayDeltas(RoleRevision current, RoleRevision previous,
            Map<String, ActionLookupRecord> actions) {
        List<RoleDisplayDelta> result = new ArrayList<>();
        for (RoleDelta delta : rawDisplayDeltas(current, previous)) {
            ActionLookupRecord action = actions.get(delta.actionId());
            result.add(new RoleDisplayDelta(delta.actionId(),
                    action == null ? "" : PlatformRoleOperations.blankToEmpty(action.name(), action.code()), delta.operation(),
                    delta.scopes() == null ? List.of() : delta.scopes()));
        }
        return result;
    }

    private static List<RoleDelta> grantDeltas(List<ActionGrant> current, List<ActionGrant> previous) {
        Map<String, ActionGrant> older = indexGrants(previous);
        Map<String, ActionGrant> newer = indexGrants(current);
        List<RoleDelta> deltas = new ArrayList<>();
        for (ActionGrant grant : current == null ? List.<ActionGrant>of() : current) {
            ActionGrant before = older.get(grant.actionId());
            if (before == null) {
                deltas.add(new RoleDelta(grant.actionId(), RoleDeltaOperation.ADD, grant.scopes()));
                continue;
            }
            if (!scopeFingerprint(before.scopes()).equals(scopeFingerprint(grant.scopes()))) {
                deltas.add(new RoleDelta(grant.actionId(), RoleDeltaOperation.REPLACE_SCOPE, grant.scopes()));
            }
        }
        for (ActionGrant grant : previous == null ? List.<ActionGrant>of() : previous) {
            if (!newer.containsKey(grant.actionId())) {
                deltas.add(new RoleDelta(grant.actionId(), RoleDeltaOperation.REMOVE, grant.scopes()));
            }
        }
        return deltas;
    }

    private static Map<String, ActionGrant> indexGrants(List<ActionGrant> grants) {
        Map<String, ActionGrant> result = new LinkedHashMap<>();
        if (grants == null) {
            return result;
        }
        for (ActionGrant grant : grants) {
            result.putIfAbsent(grant.actionId(), grant);
        }
        return result;
    }

    private static RoleRevision withDisplayDeltas(RoleRevision revision, RoleRevision previous,
            List<RoleDisplayDelta> deltas) {
        return new RoleRevision(revision.id(), revision.roleId(), revision.revision(), revision.kind(),
                revision.baseRevisionId(), revision.grants(), revision.deltas(), revision.parameterDefinitions(),
                revision.metadataOverrides(), deltas, revision.resourceFieldPermissions(),
                previous == null ? null : previous.resourceFieldPermissions(), revision.versionName(), revision.remark(),
                revision.metadataVersion(), revision.publishedAt());
    }

    private static String scopeFingerprint(List<ScopeExpression> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return "";
        }
        return scopes.stream()
            .map(scope -> (scope.kind() == null ? "" : scope.kind().name()) + "|"
                    + (scope.parameterKey() == null ? "" : scope.parameterKey()) + "|"
                    + Boolean.TRUE.equals(scope.includeDescendants()))
            .sorted()
            .reduce((left, right) -> left + ";" + right)
            .orElse("");
    }
}
