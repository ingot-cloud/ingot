package com.ingot.cloud.iam.application.catalog;

import com.ingot.cloud.iam.domain.catalog.CatalogInUse;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamActionEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamApplicationEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamResourceEntity;
import com.ingot.cloud.iam.application.shared.ResourceDetailFactory;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.cloud.iam.application.shared.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.cloud.iam.application.catalog.model.ActionCatalogResource;
import com.ingot.cloud.iam.application.catalog.model.ActionCatalogView;
import com.ingot.cloud.iam.application.catalog.model.GrantCatalogResource;
import com.ingot.cloud.iam.domain.catalog.ActionCodes;
import com.ingot.cloud.iam.application.catalog.model.ActionDraft;
import com.ingot.cloud.iam.application.catalog.model.ActionLookupInput;
import com.ingot.cloud.iam.application.catalog.model.ActionLookupRecord;
import com.ingot.cloud.iam.application.catalog.model.ActionRecord;
import com.ingot.cloud.iam.application.catalog.model.ActionUpdateInput;
import com.ingot.cloud.iam.domain.audit.AuditChangeType;
import com.ingot.cloud.iam.domain.audit.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.domain.shared.ConfigurationStatus;
import com.ingot.cloud.iam.application.shared.model.ConfigurationStatusInput;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.framework.commons.model.iam.ScopeKind;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * <p>维护对应目录对象并保留原授权、审计与事务顺序。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ActionCatalogService {
    private final CatalogOperations operationsContext;

    /**
     * 列出应用内精确操作。
     * @param applicationId 应用 ID
     * @param page 页码
     * @param pageSize 页大小
     * @param resourceId 所属资源，空白表示不限制
     * @param name 操作名称包含匹配，空白表示不限制
     * @param ids 逗号分隔操作 ID，空白表示不限制
     * @return 操作页
     */
    public PageResponse<ResourceDetail<ActionRecord>> listActions(String applicationId, int page, int pageSize,
            String resourceId, String name, String ids) {
        operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACTION_READ);
        long appId = IamIds.require(applicationId);
        operationsContext.requireApplication(appId);
        IamPages.require(page, pageSize);
        Page<IamActionEntity> rows = operationsContext.catalog().pageActions(appId, page, pageSize, IamIds.optional(resourceId), name,
                IamIds.optionalList(ids));
        List<ResourceDetail<ActionRecord>> items = rows.getRecords()
            .stream()
            .map(row -> ResourceDetailFactory.of(action(row), CatalogOperations.version(row.getVersion())))
            .toList();
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    /**
     * 一次返回应用内资源及操作树，供菜单选择操作。
     * @param applicationId 应用 ID
     * @return 操作目录
     */
    public ActionCatalogView getActionCatalog(String applicationId) {
        operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACTION_READ);
        long appId = IamIds.require(applicationId);
        IamApplicationEntity application = operationsContext.catalog().findApplication(appId);
        if (application == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        List<IamResourceEntity> resources = operationsContext.catalog().listResources(appId);
        Map<BigInteger, List<IamActionEntity>> actions = new LinkedHashMap<>();
        for (IamActionEntity row : operationsContext.catalog().listActions(appId)) {
            actions.computeIfAbsent(row.getResourceId(), key -> new ArrayList<>()).add(row);
        }
        List<ActionCatalogResource> nodes = resources.stream()
            .map(resource -> new ActionCatalogResource(CatalogOperations.text(resource.getId()), resource.getCode(), resource.getName(),
                    actions.getOrDefault(resource.getId(), List.of()).stream().map(operationsContext::catalogItem).toList()))
            .toList();
        return new ActionCatalogView(CatalogOperations.text(application.getId()), application.getCode(), application.getName(), nodes);
    }

    /**
     * 一次返回应用内全部启用资源及其启用操作，供角色权限选择器组树。
     *
     * @param applicationId 应用 ID
     * @return 授权选择目录
     */
    public List<GrantCatalogResource> listGrantCatalog(String applicationId) {
        operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACTION_READ);
        long appId = IamIds.require(applicationId);
        operationsContext.requireApplication(appId);
        List<IamResourceEntity> rows = operationsContext.catalog().listEnabledResources(appId);
        Map<BigInteger, List<IamActionEntity>> actions = new LinkedHashMap<>();
        for (IamActionEntity row : operationsContext.catalog().listEnabledActions(appId)) {
            actions.computeIfAbsent(row.getResourceId(), key -> new ArrayList<>()).add(row);
        }
        var descriptors = operationsContext.fieldMetadata().describe(rows);
        return rows.stream()
            .map(resource -> new GrantCatalogResource(CatalogOperations.text(resource.getId()), resource.getCode(), resource.getName(),
                    scopesOf(resource),
                    actions.getOrDefault(resource.getId(), List.of()).stream().map(operationsContext::grantCatalogAction).toList(),
                    descriptors.containsKey(CatalogOperations.text(resource.getId())) ? descriptors.get(CatalogOperations.text(resource.getId())).fields()
                            : List.of(),
                    descriptors.containsKey(CatalogOperations.text(resource.getId())) ? descriptors.get(CatalogOperations.text(resource.getId())).defaults()
                            : Map.of()))
            .toList();
    }

    /**
     * 一次返回指定资源下的全部操作，供权限树展开。
     * @param applicationId 应用 ID
     * @param resourceId 资源 ID
     * @return 操作列表
     */
    public List<ActionRecord> listResourceActions(String applicationId, String resourceId) {
        operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACTION_READ);
        long appId = IamIds.require(applicationId);
        long id = IamIds.require(resourceId);
        if (operationsContext.catalog().findResource(appId, id) == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return operationsContext.catalog().listActionsByResource(appId, id).stream().map(ActionCatalogService::action).toList();
    }

    /**
     * 按操作 ID 批量解析名称、应用与资源，供权限回显。
     * @param input 操作 ID
     * @return 解析结果；未命中的 ID 省略
     */
    public List<ActionLookupRecord> lookupActions(ActionLookupInput input) {
        operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACTION_READ);
        return resolveActions(input.ids());
    }

    /**
     * 按操作 ID 解析目录内容，不校验 ACTION；调用方须已完成自身读授权。
     * @param ids 操作 ID
     * @return 解析结果；未命中的 ID 省略
     */
    public List<ActionLookupRecord> resolveActions(List<String> ids) {
        List<Long> wanted = requireIds(ids);
        if (wanted.isEmpty()) {
            return List.of();
        }
        List<IamActionEntity> rows = operationsContext.catalog().listActionsByIds(wanted);
        Map<BigInteger, IamApplicationEntity> applications = new LinkedHashMap<>();
        for (IamApplicationEntity application : operationsContext.catalog()
            .listApplicationsByIds(rows.stream().map(row -> row.getApplicationId().longValue()).distinct().toList())) {
            applications.put(application.getId(), application);
        }
        Map<BigInteger, IamResourceEntity> resources = new LinkedHashMap<>();
        for (IamResourceEntity resource : operationsContext.catalog()
            .listResourcesByIds(rows.stream().map(row -> row.getResourceId().longValue()).distinct().toList())) {
            resources.put(resource.getId(), resource);
        }
        Map<BigInteger, IamActionEntity> byId = rows.stream()
            .collect(Collectors.toMap(IamActionEntity::getId, row -> row, (left, right) -> left, LinkedHashMap::new));
        List<ActionLookupRecord> result = new ArrayList<>();
        for (Long id : wanted) {
            IamActionEntity row = byId.get(BigInteger.valueOf(id));
            if (row == null) {
                continue;
            }
            IamApplicationEntity application = applications.get(row.getApplicationId());
            IamResourceEntity resource = resources.get(row.getResourceId());
            List<ScopeKind> scopes = resource == null ? List.of()
                    : IamJsonCodec.read(resource.getScopeCapabilities(), CatalogOperations.SCOPES);
            result.add(new ActionLookupRecord(CatalogOperations.text(row.getId()), row.getCode(), row.getName(),
                    CatalogOperations.text(row.getApplicationId()), application == null ? "" : application.getCode(),
                    application == null ? "" : application.getName(), CatalogOperations.text(row.getResourceId()),
                    resource == null ? "" : resource.getCode(), resource == null ? "" : resource.getName(),
                    scopes == null ? List.of() : scopes, CatalogOperations.statusOf(row.getEnabled())));
        }
        return result;
    }

    /**
     * 创建绑定资源的精确操作，操作码不得含通配符。
     * @param applicationId 应用 ID
     * @param input 操作草稿
     * @return 新操作 ID
     */
    public CreatedResource createAction(String applicationId, ActionDraft input) {
        ActiveIdentity actor = operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACTION_CREATE);
        if (input.code() == null || input.code().indexOf('*') >= 0) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        long appId = IamIds.require(applicationId);
        long resourceId = IamIds.require(input.resourceId());
        return operationsContext.transaction().execute(status -> {
            IamApplicationEntity application = operationsContext.catalog().findApplication(appId);
            if (application == null) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            IamResourceEntity resource = CatalogOperations.requireLocked(operationsContext.catalog().lockResource(appId, resourceId));
            long id = operationsContext.access().nextId();
            IamActionEntity entity = new IamActionEntity();
            entity.setId(BigInteger.valueOf(id));
            entity.setApplicationId(BigInteger.valueOf(appId));
            entity.setResourceId(BigInteger.valueOf(resourceId));
            entity.setCode(ActionCodes.compose(application.getCode(), resource.getCode(), input.code()));
            entity.setName(input.name());
            entity.setEnabled(true);
            try {
                operationsContext.catalog().insertAction(entity);
            }
            catch (DuplicateKeyException exception) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), CatalogOperations.ACTION, IamIds.text(id), AuditChangeType.CREATE, Map.of(),
                    Map.of(AuditField.NAME, input.name()), Map.of(CatalogOperations.ACTION, "0"));
            return new CreatedResource(IamIds.text(id), "0");
        });
    }

    /**
     * 更新操作名称，不能改写操作码或所属资源。
     * @param applicationId 应用 ID
     * @param actionId 操作 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    public ResourceDetail<ActionRecord> updateAction(String applicationId, String actionId, ActionUpdateInput input) {
        ActiveIdentity actor = operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACTION_UPDATE);
        long appId = IamIds.require(applicationId);
        long id = IamIds.require(actionId);
        return operationsContext.transaction().execute(status -> {
            IamActionEntity current = CatalogOperations.requireLocked(operationsContext.catalog().lockAction(appId, id));
            IamIds.requireVersion(input.expectedVersion(), CatalogOperations.version(current.getVersion()));
            operationsContext.catalog().updateActionName(appId, id, input.name(), current.getVersion());
            String next = CatalogOperations.nextVersion(current.getVersion());
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), CatalogOperations.ACTION, actionId, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.getName()), Map.of(AuditField.NAME, input.name()),
                    Map.of(CatalogOperations.ACTION, next));
            return loadAction(appId, id);
        });
    }

    /**
     * 全局启停操作。
     * @param applicationId 应用 ID
     * @param actionId 操作 ID
     * @param input 目标状态
     * @return 提交后版本
     */
    public CreatedResource changeActionStatus(String applicationId, String actionId, ConfigurationStatusInput input) {
        ActiveIdentity actor = operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACTION_STATUS);
        long appId = IamIds.require(applicationId);
        long id = IamIds.require(actionId);
        return operationsContext.transaction().execute(status -> {
            IamActionEntity current = CatalogOperations.requireLocked(operationsContext.catalog().lockAction(appId, id));
            IamIds.requireVersion(input.expectedVersion(), CatalogOperations.version(current.getVersion()));
            boolean enabled = input.status() == ConfigurationStatus.ENABLED;
            if (Boolean.TRUE.equals(current.getEnabled()) == enabled) {
                return new CreatedResource(actionId, CatalogOperations.version(current.getVersion()));
            }
            operationsContext.catalog().updateActionEnabled(appId, id, enabled, current.getVersion());
            String next = CatalogOperations.nextVersion(current.getVersion());
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), CatalogOperations.ACTION, actionId,
                    enabled ? AuditChangeType.ENABLE : AuditChangeType.DISABLE,
                    Map.of(AuditField.STATUS, CatalogOperations.statusOf(current.getEnabled()).name()),
                    Map.of(AuditField.STATUS, input.status().name()), Map.of(CatalogOperations.ACTION, next));
            operationsContext.changes().markAll();
            return new CreatedResource(actionId, next);
        });
    }

    /**
     * 删除未被菜单或角色引用的操作。
     * @param applicationId 应用 ID
     * @param actionId 操作 ID
     * @return 删除前版本
     */
    public CreatedResource deleteAction(String applicationId, String actionId) {
        ActiveIdentity actor = operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACTION_DELETE);
        long appId = IamIds.require(applicationId);
        long id = IamIds.require(actionId);
        return operationsContext.transaction().execute(status -> {
            IamActionEntity current = CatalogOperations.requireLocked(operationsContext.catalog().lockAction(appId, id));
            CatalogInUse.requireUnused(operationsContext.catalog().countMenuActions(appId, id), CatalogInUse.ACTION_HAS_MENUS);
            CatalogInUse.requireUnused(operationsContext.catalog().countRoleGrants(id), CatalogInUse.ACTION_HAS_GRANTS);
            CatalogInUse.requireUnused(operationsContext.catalog().countRoleDeltas(id), CatalogInUse.ACTION_HAS_DELTAS);
            operationsContext.catalog().deleteAction(appId, id);
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), CatalogOperations.ACTION, actionId, AuditChangeType.REMOVE,
                    Map.of(AuditField.NAME, current.getName()), Map.of(),
                    Map.of(CatalogOperations.ACTION, CatalogOperations.version(current.getVersion())));
            return new CreatedResource(actionId, CatalogOperations.version(current.getVersion()));
        });
    }

    private ResourceDetail<ActionRecord> loadAction(long applicationId, long id) {
        IamActionEntity row = operationsContext.catalog().findAction(applicationId, id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return ResourceDetailFactory.of(action(row), CatalogOperations.version(row.getVersion()));
    }

    private static List<ScopeKind> scopesOf(IamResourceEntity row) {
        List<ScopeKind> scopes = IamJsonCodec.read(row.getScopeCapabilities(), CatalogOperations.SCOPES);
        return scopes == null ? List.of() : scopes;
    }

    private static ActionRecord action(IamActionEntity row) {
        return new ActionRecord(CatalogOperations.text(row.getId()), CatalogOperations.text(row.getApplicationId()), CatalogOperations.text(row.getResourceId()),
                row.getCode(), row.getName(), CatalogOperations.statusOf(row.getEnabled()));
    }

    private static List<Long> requireIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                result.add(IamIds.require(id));
            }
        }
        return result;
    }
}
