package com.ingot.cloud.iam.application.role;

import com.ingot.cloud.iam.domain.role.RoleRevisionSnapshot;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleDefinitionEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleRevisionEntity;
import com.ingot.cloud.iam.application.authorization.ActionCapabilitySnapshot;
import com.ingot.cloud.iam.domain.shared.IamFilters;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.application.shared.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.cloud.iam.application.catalog.model.ActionLookupRecord;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.cloud.iam.application.role.model.RoleGrantRecord;
import com.ingot.cloud.iam.application.role.model.RoleGrantList;
import com.ingot.cloud.iam.application.role.model.RoleRevisionSummary;
import com.ingot.cloud.iam.application.role.model.RoleSummary;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * <p>执行对应平台角色用例，保留固定版本、字段权限和乐观锁规则。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PlatformRoleQueryService {
    private final PlatformRoleOperations operationsContext;
    private final PlatformRoleRevisionService platformRoleRevisionService;

    /**
     * 列出当前路径可见的角色目录。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param page 页码
     * @param pageSize 页大小
     * @return 角色页，不按名称或状态筛选
     */
    public PageResponse<ResourceDetail<RoleSummary>> list(AuthorizationDomain domain, boolean shared, int page,
            int pageSize) {
        return list(domain, shared, page, pageSize, null, null);
    }

    /**
     * 列出当前路径可见的角色目录，可按名称包含匹配和启停状态筛选。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param page 页码
     * @param pageSize 页大小
     * @param name 角色名称包含匹配，空白表示不限制
     * @param status 启停状态，空白表示不限制；仅接受 ENABLED/DISABLED
     * @return 角色页
     * @throws BizException 状态字面量非法时为 {@link IamReasonCode#INVALID_ARGUMENT}
     */
    public PageResponse<ResourceDetail<RoleSummary>> list(AuthorizationDomain domain, boolean shared, int page,
            int pageSize, String name, String status) {
        ActiveIdentity actor = operationsContext.access().require(domain, PlatformRoleOperations.action(domain, shared, PlatformRoleOperations.AccessKind.READ));
        IamPages.require(page, pageSize);
        Page<IamRoleDefinitionEntity> rows = operationsContext.roles().pageDefinitions(domain, shared, PlatformRoleOperations.tenantId(domain, actor), page,
                pageSize, name, IamFilters.enabledOf(status));
        ActionCapabilitySnapshot permissions = rows.getRecords().isEmpty() ? new ActionCapabilitySnapshot(Map.of())
                : operationsContext.summaryCapabilities(domain, shared, actor);
        List<ResourceDetail<RoleSummary>> items = rows.getRecords()
            .stream()
            .map(row -> operationsContext.summaryDetail(domain, shared, row, permissions))
            .toList();
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    /**
     * 读取角色元数据。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @return 角色详情
     */
    public ResourceDetail<RoleSummary> get(AuthorizationDomain domain, boolean shared, String id) {
        ActiveIdentity actor = operationsContext.access().require(domain, PlatformRoleOperations.action(domain, shared, PlatformRoleOperations.AccessKind.READ));
        return operationsContext.loadSummary(domain, shared, actor, IamIds.require(id));
    }

    /**
     * 读取角色最新已发布版本的当前绑定权限，带目录展示内容。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @return 同一固定版本的当前绑定权限及摘要；尚无版本时为空列表且省略摘要
     */
    public RoleGrantList listCurrentGrants(AuthorizationDomain domain, boolean shared, String id) {
        ActiveIdentity actor = operationsContext.access().require(domain, PlatformRoleOperations.action(domain, shared, PlatformRoleOperations.AccessKind.READ));
        long roleId = IamIds.require(id);
        operationsContext.requireDefinition(domain, shared, actor, roleId);
        IamRoleRevisionEntity latest = operationsContext.roles().latestRevision(roleId);
        if (latest == null || latest.getId() == null) {
            return new RoleGrantList(List.of());
        }
        List<ActionGrant> grants = operationsContext.grantRows(operationsContext.roles().listGrants(latest.getId().longValueExact()));
        if (grants.isEmpty() && PlatformRoleOperations.based(PlatformRoleOperations.text(latest.getBaseRevisionId()))) {
            var base = operationsContext.roles().findRevision(latest.getBaseRevisionId().longValueExact());
            if (base == null) throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
            grants = operationsContext.synthesis().grants(new RoleRevisionSnapshot(latest.getId().longValueExact(),
                    base.getId().longValueExact(), operationsContext.grantRows(operationsContext.roles().listGrants(base.getId().longValueExact())),
                    operationsContext.deltaRows(operationsContext.roles().listDeltas(latest.getId().longValueExact()))));
        }
        var summary = new RoleRevisionSummary(PlatformRoleOperations.text(latest.getId()), PlatformRoleOperations.version(latest.getRevision()),
                PlatformRoleOperations.normalizeName(latest.getVersionName(), latest.getRevision().longValueExact()));
        return new RoleGrantList(toGrantRecords(grants, com.ingot.cloud.iam.application.authorization.extension.RoleFieldPermissionService
                .snapshot(latest.getResourceFieldPermissions())), summary);
    }

    /**
     * 读取同角色任一已发布版本的完整合成授权与字段快照，供历史编辑使用。
     * @param domain 管理域
     * @param shared 是否共享入口
     * @param id 角色ID
     * @param revisionId 版本ID
     * @return 不省略字段权限的固定授权
     */
    public List<RoleGrantRecord> listRevisionGrants(AuthorizationDomain domain, boolean shared, String id, String revisionId) {
        platformRoleRevisionService.getRevision(domain, shared, id, revisionId);
        var data = operationsContext.loadRevision(IamIds.require(revisionId));
        return toGrantRecords(platformRoleRevisionService.synthesizedGrants(IamIds.require(revisionId)), data.fields());
    }

    /**
     * 读取当前入口的角色定义，不装配展示能力；调用方仍须完成操作准入和原有对象范围门禁。
     * @param domain 可信接口域
     * @param shared 是否共享入口
     * @param actor 已准入身份
     * @param id 角色 ID
     * @return 同域同租户角色定义
     */
    public IamRoleDefinitionEntity requireDefinition(AuthorizationDomain domain, boolean shared, ActiveIdentity actor,
            long id) {
        return operationsContext.requireDefinition(domain, shared, actor, id);
    }

    private List<RoleGrantRecord> toGrantRecords(List<ActionGrant> grants,
            Map<String, com.ingot.framework.commons.model.iam.ResourceFieldDefinition> fields) {
        if (grants == null || grants.isEmpty()) {
            return List.of();
        }
        Map<String, ActionLookupRecord> actions = PlatformRoleOperations.indexActions(operationsContext.catalog().resolveActions(grants.stream()
            .map(ActionGrant::actionId)
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .toList()));
        var descriptors = operationsContext.fieldMetadata()
            .load(actions.values().stream().map(a -> new BigInteger(a.resourceId())).distinct().toList());
        List<RoleGrantRecord> result = new ArrayList<>();
        for (ActionGrant grant : grants) {
            ActionLookupRecord action = actions.get(grant.actionId());
            var descriptor = action == null ? null : descriptors.get(action.resourceId());
            result.add(new RoleGrantRecord(grant.actionId(), action == null ? "" : action.code(),
                    action == null ? "" : PlatformRoleOperations.blankToEmpty(action.name(), action.code()),
                    action == null ? "" : action.applicationId(), action == null ? "" : action.applicationCode(),
                    action == null ? "" : action.applicationName(), action == null ? "" : action.resourceId(),
                    action == null ? "" : action.resourceCode(), action == null ? "" : action.resourceName(),
                    grant.scopes() == null ? List.of() : grant.scopes(),
                    action == null || action.scopeCapabilities() == null ? List.of() : action.scopeCapabilities(),
                    action == null ? null : action.status(), descriptor == null ? List.of() : descriptor.fields(),
                    descriptor == null ? Map.of() : descriptor.defaults(),
                    fields == null || action == null ? com.ingot.framework.commons.model.iam.ResourceFieldDefinition.EMPTY : fields.getOrDefault(action.resourceId(), com.ingot.framework.commons.model.iam.ResourceFieldDefinition.EMPTY)));
        }
        return result;
    }
}
