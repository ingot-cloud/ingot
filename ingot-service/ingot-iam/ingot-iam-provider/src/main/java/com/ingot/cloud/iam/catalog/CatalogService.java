package com.ingot.cloud.iam.catalog;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.CatalogRepository;
import com.ingot.cloud.iam.persistence.entity.IamActionEntity;
import com.ingot.cloud.iam.persistence.entity.IamApplicationEntity;
import com.ingot.cloud.iam.persistence.entity.IamMenuEntity;
import com.ingot.cloud.iam.persistence.entity.IamPlanEntity;
import com.ingot.cloud.iam.persistence.entity.IamResourceEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamDetails;
import com.ingot.cloud.iam.support.IamFilters;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionDraft;
import com.ingot.framework.commons.model.iam.ActionRecord;
import com.ingot.framework.commons.model.iam.ActionUpdateInput;
import com.ingot.framework.commons.model.iam.ApplicationDraft;
import com.ingot.framework.commons.model.iam.ApplicationRecord;
import com.ingot.framework.commons.model.iam.ApplicationUpdateInput;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.ConfigurationStatus;
import com.ingot.framework.commons.model.iam.ConfigurationStatusInput;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.FieldCapability;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MenuDraft;
import com.ingot.framework.commons.model.iam.MenuRecord;
import com.ingot.framework.commons.model.iam.MenuTreeNode;
import com.ingot.framework.commons.model.iam.MenuUpdateInput;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.PlanDraft;
import com.ingot.framework.commons.model.iam.PlanRecord;
import com.ingot.framework.commons.model.iam.PlanUpdateInput;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.ResourceDraft;
import com.ingot.framework.commons.model.iam.ResourceRecord;
import com.ingot.framework.commons.model.iam.ResourceUpdateInput;
import com.ingot.framework.commons.model.iam.ScopeKind;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>维护应用、资源、操作、菜单和套餐目录，不把启停或套餐变更写成隐式开通。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class CatalogService {
    private static final TypeReference<List<ScopeKind>> SCOPES = new TypeReference<>() {
    };
    private static final TypeReference<List<FieldCapability>> FIELDS = new TypeReference<>() {
    };
    private static final String APPLICATION = "application";
    private static final String RESOURCE = "resource";
    private static final String ACTION = "action";
    private static final String MENU = "menu";
    private static final String PLAN = "plan";
    private final IamAccess access;
    private final IamAuditWriter audits;
    private final AuthorizationChangeNotifier changes;
    private final CatalogRepository catalog;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、审计、目录库与事务。
     * <p>TransactionTemplate 无法由 Lombok 从 PlatformTransactionManager 直接生成，保留显式构造器。</p>
     *
     * @param access 当前身份与 ACTION
     * @param audits 同事务审计
     * @param changes 应用/操作启停后的授权失效
     * @param catalog 目录持久化
     * @param transactionManager 同一数据源事务
     */
    public CatalogService(IamAccess access, IamAuditWriter audits, AuthorizationChangeNotifier changes,
                              CatalogRepository catalog, PlatformTransactionManager transactionManager) {
        this.access = access;
        this.audits = audits;
        this.changes = changes;
        this.catalog = catalog;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 分页列出应用目录。
     *
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @param domain {@link AuthorizationDomain} 稳定字面量，必填
     * @param name 应用名称包含匹配，空白表示不限制
     * @param status {@link ConfigurationStatus} 稳定字面量，空白表示不限制
     * @param baseline 是否组织默认开通，空表示不限制
     * @return 应用详情页
     */
    public PageResponse<ResourceDetail<ApplicationRecord>> listApplications(int page, int pageSize, String domain,
                                                                            String name, String status,
                                                                            Boolean baseline) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_APPLICATION_READ);
        IamPages.require(page, pageSize);
        Page<IamApplicationEntity> rows = catalog.pageApplications(page, pageSize, IamFilters.requireDomain(domain),
                name, IamFilters.enabledOf(status), baseline);
        List<ResourceDetail<ApplicationRecord>> items = rows.getRecords().stream()
                .map(row -> IamDetails.of(application(row), version(row.getVersion()))).toList();
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    /**
     * 读取单个应用。
     *
     * @param id 应用 ID
     * @return 应用详情
     */
    public ResourceDetail<ApplicationRecord> getApplication(String id) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_APPLICATION_READ);
        return loadApplication(IamIds.require(id));
    }

    /**
     * 创建应用目录项，不自动为任何租户开通。
     *
     * @param input 创建草稿
     * @return 新应用 ID 与版本
     */
    public CreatedResource createApplication(ApplicationDraft input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_APPLICATION_CREATE);
        if (input.domain() == AuthorizationDomain.PLATFORM && input.baseline()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return transaction.execute(status -> {
            long id = access.nextId();
            IamApplicationEntity entity = new IamApplicationEntity();
            entity.setId(BigInteger.valueOf(id));
            entity.setCode(input.code());
            entity.setDomain(input.domain());
            entity.setName(input.name());
            entity.setDescription(nullable(input.description()));
            entity.setIcon(nullable(input.icon()));
            entity.setSortOrder(input.sortOrder());
            entity.setBaseline(input.baseline());
            entity.setEnabled(true);
            try {
                catalog.insertApplication(entity);
            } catch (DuplicateKeyException exception) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            audits.write(actor.context(), access.nextId(), APPLICATION, IamIds.text(id), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.NAME, input.name()), Map.of(APPLICATION, "0"));
            return new CreatedResource(IamIds.text(id), "0");
        });
    }

    /**
     * 更新展示信息与基础开通标记，不能改写命名空间或管理域。
     *
     * @param id 应用 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    public ResourceDetail<ApplicationRecord> updateApplication(String id, ApplicationUpdateInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_APPLICATION_UPDATE);
        long applicationId = IamIds.require(id);
        return transaction.execute(status -> {
            IamApplicationEntity current = requireLocked(catalog.lockApplication(applicationId));
            IamIds.requireVersion(input.expectedVersion(), version(current.getVersion()));
            if (current.getDomain() == AuthorizationDomain.PLATFORM && input.baseline()) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            catalog.updateApplication(applicationId, input.name(), nullable(input.description()),
                    nullable(input.icon()), input.sortOrder(), input.baseline(), current.getVersion());
            String next = nextVersion(current.getVersion());
            audits.write(actor.context(), access.nextId(), APPLICATION, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.getName()), Map.of(AuditField.NAME, input.name()),
                    Map.of(APPLICATION, next));
            return loadApplication(applicationId);
        });
    }

    /**
     * 全局启停应用，停用后该应用全部操作失败关闭。
     *
     * @param id 应用 ID
     * @param input 目标状态与版本
     * @return 提交后版本
     */
    public CreatedResource changeApplicationStatus(String id, ConfigurationStatusInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_APPLICATION_STATUS);
        long applicationId = IamIds.require(id);
        return transaction.execute(status -> {
            IamApplicationEntity current = requireLocked(catalog.lockApplication(applicationId));
            IamIds.requireVersion(input.expectedVersion(), version(current.getVersion()));
            boolean enabled = input.status() == ConfigurationStatus.ENABLED;
            if (Boolean.TRUE.equals(current.getEnabled()) == enabled) {
                return new CreatedResource(id, version(current.getVersion()));
            }
            catalog.updateApplicationEnabled(applicationId, enabled, current.getVersion());
            String next = nextVersion(current.getVersion());
            audits.write(actor.context(), access.nextId(), APPLICATION, id,
                    enabled ? AuditChangeType.ENABLE : AuditChangeType.DISABLE,
                    Map.of(AuditField.STATUS, statusOf(current.getEnabled()).name()),
                    Map.of(AuditField.STATUS, input.status().name()), Map.of(APPLICATION, next));
            changes.markAll();
            return new CreatedResource(id, next);
        });
    }

    /**
     * 删除未被资源、开通或套餐引用的应用。
     *
     * @param id 应用 ID
     * @return 删除前版本
     */
    public CreatedResource deleteApplication(String id) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_APPLICATION_DELETE);
        long applicationId = IamIds.require(id);
        return transaction.execute(status -> {
            IamApplicationEntity current = requireLocked(catalog.lockApplication(applicationId));
            requireUnused(catalog.countResources(applicationId));
            requireUnused(catalog.countEntitlements(applicationId));
            requireUnused(catalog.countPlanApplications(applicationId));
            requireUnused(catalog.countMenus(applicationId));
            catalog.deleteApplication(applicationId);
            audits.write(actor.context(), access.nextId(), APPLICATION, id, AuditChangeType.REMOVE,
                    Map.of(AuditField.NAME, current.getName()), Map.of(),
                    Map.of(APPLICATION, version(current.getVersion())));
            return new CreatedResource(id, version(current.getVersion()));
        });
    }

    /**
     * 列出应用内资源。
     *
     * @param applicationId 应用 ID
     * @param page 页码
     * @param pageSize 页大小
     * @param name 资源名称包含匹配，空白表示不限制
     * @param code 资源编码包含匹配，空白表示不限制
     * @return 资源页
     */
    public PageResponse<ResourceDetail<ResourceRecord>> listResources(String applicationId, int page, int pageSize,
                                                                     String name, String code) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_RESOURCE_READ);
        long id = IamIds.require(applicationId);
        requireApplication(id);
        IamPages.require(page, pageSize);
        Page<IamResourceEntity> rows = catalog.pageResources(id, page, pageSize, name, code);
        List<ResourceDetail<ResourceRecord>> items = rows.getRecords().stream()
                .map(row -> IamDetails.of(resource(row), version(row.getVersion()))).toList();
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    /**
     * 在指定应用下创建资源。
     *
     * @param applicationId 应用 ID
     * @param input 资源草稿
     * @return 新资源 ID
     */
    public CreatedResource createResource(String applicationId, ResourceDraft input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_RESOURCE_CREATE);
        long appId = IamIds.require(applicationId);
        return transaction.execute(status -> {
            requireLocked(catalog.lockApplication(appId));
            long id = access.nextId();
            IamResourceEntity entity = new IamResourceEntity();
            entity.setId(BigInteger.valueOf(id));
            entity.setApplicationId(BigInteger.valueOf(appId));
            entity.setCode(input.code());
            entity.setName(input.name());
            entity.setScopeCapabilities(IamJson.array(input.scopeCapabilities()));
            entity.setFieldCapabilities(IamJson.array(input.fieldCapabilities()));
            entity.setEnabled(true);
            try {
                catalog.insertResource(entity);
            } catch (DuplicateKeyException exception) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            audits.write(actor.context(), access.nextId(), RESOURCE, IamIds.text(id), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.NAME, input.name()), Map.of(RESOURCE, "0"));
            return new CreatedResource(IamIds.text(id), "0");
        });
    }

    /**
     * 更新资源名称与能力，不能改写编码或所属应用。
     *
     * @param applicationId 应用 ID
     * @param resourceId 资源 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    public ResourceDetail<ResourceRecord> updateResource(String applicationId, String resourceId,
                                                         ResourceUpdateInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_RESOURCE_UPDATE);
        long appId = IamIds.require(applicationId);
        long id = IamIds.require(resourceId);
        return transaction.execute(status -> {
            IamResourceEntity current = requireLocked(catalog.lockResource(appId, id));
            IamIds.requireVersion(input.expectedVersion(), version(current.getVersion()));
            catalog.updateResource(appId, id, input.name(), IamJson.array(input.scopeCapabilities()),
                    IamJson.array(input.fieldCapabilities()), current.getVersion());
            String next = nextVersion(current.getVersion());
            audits.write(actor.context(), access.nextId(), RESOURCE, resourceId, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.getName()), Map.of(AuditField.NAME, input.name()),
                    Map.of(RESOURCE, next));
            return loadResource(appId, id);
        });
    }

    /**
     * 删除未被操作引用的资源。
     *
     * @param applicationId 应用 ID
     * @param resourceId 资源 ID
     * @return 删除前版本
     */
    public CreatedResource deleteResource(String applicationId, String resourceId) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_RESOURCE_DELETE);
        long appId = IamIds.require(applicationId);
        long id = IamIds.require(resourceId);
        return transaction.execute(status -> {
            IamResourceEntity current = requireLocked(catalog.lockResource(appId, id));
            requireUnused(catalog.countActions(appId, id));
            catalog.deleteResource(appId, id);
            audits.write(actor.context(), access.nextId(), RESOURCE, resourceId, AuditChangeType.REMOVE,
                    Map.of(AuditField.NAME, current.getName()), Map.of(),
                    Map.of(RESOURCE, version(current.getVersion())));
            return new CreatedResource(resourceId, version(current.getVersion()));
        });
    }

    /**
     * 列出应用内精确操作。
     *
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
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACTION_READ);
        long appId = IamIds.require(applicationId);
        requireApplication(appId);
        IamPages.require(page, pageSize);
        Page<IamActionEntity> rows = catalog.pageActions(appId, page, pageSize, IamIds.optional(resourceId), name,
                IamIds.optionalList(ids));
        List<ResourceDetail<ActionRecord>> items = rows.getRecords().stream()
                .map(row -> IamDetails.of(action(row), version(row.getVersion()))).toList();
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    /**
     * 创建绑定资源的精确操作，操作码不得含通配符。
     *
     * @param applicationId 应用 ID
     * @param input 操作草稿
     * @return 新操作 ID
     */
    public CreatedResource createAction(String applicationId, ActionDraft input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACTION_CREATE);
        if (input.code() == null || input.code().indexOf('*') >= 0) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        long appId = IamIds.require(applicationId);
        long resourceId = IamIds.require(input.resourceId());
        return transaction.execute(status -> {
            requireLocked(catalog.lockResource(appId, resourceId));
            long id = access.nextId();
            IamActionEntity entity = new IamActionEntity();
            entity.setId(BigInteger.valueOf(id));
            entity.setApplicationId(BigInteger.valueOf(appId));
            entity.setResourceId(BigInteger.valueOf(resourceId));
            entity.setCode(input.code());
            entity.setName(input.name());
            entity.setEnabled(true);
            try {
                catalog.insertAction(entity);
            } catch (DuplicateKeyException exception) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            audits.write(actor.context(), access.nextId(), ACTION, IamIds.text(id), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.NAME, input.name()), Map.of(ACTION, "0"));
            return new CreatedResource(IamIds.text(id), "0");
        });
    }

    /**
     * 更新操作名称，不能改写操作码或所属资源。
     *
     * @param applicationId 应用 ID
     * @param actionId 操作 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    public ResourceDetail<ActionRecord> updateAction(String applicationId, String actionId, ActionUpdateInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACTION_UPDATE);
        long appId = IamIds.require(applicationId);
        long id = IamIds.require(actionId);
        return transaction.execute(status -> {
            IamActionEntity current = requireLocked(catalog.lockAction(appId, id));
            IamIds.requireVersion(input.expectedVersion(), version(current.getVersion()));
            catalog.updateActionName(appId, id, input.name(), current.getVersion());
            String next = nextVersion(current.getVersion());
            audits.write(actor.context(), access.nextId(), ACTION, actionId, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.getName()), Map.of(AuditField.NAME, input.name()),
                    Map.of(ACTION, next));
            return loadAction(appId, id);
        });
    }

    /**
     * 全局启停操作。
     *
     * @param applicationId 应用 ID
     * @param actionId 操作 ID
     * @param input 目标状态
     * @return 提交后版本
     */
    public CreatedResource changeActionStatus(String applicationId, String actionId, ConfigurationStatusInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACTION_STATUS);
        long appId = IamIds.require(applicationId);
        long id = IamIds.require(actionId);
        return transaction.execute(status -> {
            IamActionEntity current = requireLocked(catalog.lockAction(appId, id));
            IamIds.requireVersion(input.expectedVersion(), version(current.getVersion()));
            boolean enabled = input.status() == ConfigurationStatus.ENABLED;
            if (Boolean.TRUE.equals(current.getEnabled()) == enabled) {
                return new CreatedResource(actionId, version(current.getVersion()));
            }
            catalog.updateActionEnabled(appId, id, enabled, current.getVersion());
            String next = nextVersion(current.getVersion());
            audits.write(actor.context(), access.nextId(), ACTION, actionId,
                    enabled ? AuditChangeType.ENABLE : AuditChangeType.DISABLE,
                    Map.of(AuditField.STATUS, statusOf(current.getEnabled()).name()),
                    Map.of(AuditField.STATUS, input.status().name()), Map.of(ACTION, next));
            changes.markAll();
            return new CreatedResource(actionId, next);
        });
    }

    /**
     * 删除未被菜单或角色引用的操作。
     *
     * @param applicationId 应用 ID
     * @param actionId 操作 ID
     * @return 删除前版本
     */
    public CreatedResource deleteAction(String applicationId, String actionId) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACTION_DELETE);
        long appId = IamIds.require(applicationId);
        long id = IamIds.require(actionId);
        return transaction.execute(status -> {
            IamActionEntity current = requireLocked(catalog.lockAction(appId, id));
            requireUnused(catalog.countMenuActions(appId, id));
            requireUnused(catalog.countRoleGrants(id));
            requireUnused(catalog.countRoleDeltas(id));
            catalog.deleteAction(appId, id);
            audits.write(actor.context(), access.nextId(), ACTION, actionId, AuditChangeType.REMOVE,
                    Map.of(AuditField.NAME, current.getName()), Map.of(),
                    Map.of(ACTION, version(current.getVersion())));
            return new CreatedResource(actionId, version(current.getVersion()));
        });
    }

    /**
     * 列出应用菜单。
     *
     * @param applicationId 应用 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 菜单页
     */
    public PageResponse<ResourceDetail<MenuRecord>> listMenus(String applicationId, int page, int pageSize) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_MENU_READ);
        long appId = IamIds.require(applicationId);
        requireApplication(appId);
        IamPages.require(page, pageSize);
        Page<IamMenuEntity> rows = catalog.pageMenus(appId, page, pageSize);
        java.util.Map<BigInteger, List<BigInteger>> actions = catalog.menuActionIds(appId,
                rows.getRecords().stream().map(IamMenuEntity::getId).toList());
        List<ResourceDetail<MenuRecord>> items = rows.getRecords().stream()
                .map(row -> IamDetails.of(menu(row, texts(actions.getOrDefault(row.getId(), List.of()))),
                        version(row.getVersion()))).toList();
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    /**
     * 返回应用菜单树，不带分页。
     *
     * @param applicationId 应用 ID
     * @return 根节点
     */
    public List<MenuTreeNode> listMenuTree(String applicationId) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_MENU_READ);
        long appId = IamIds.require(applicationId);
        requireApplication(appId);
        List<IamMenuEntity> rows = catalog.listMenus(appId);
        Map<BigInteger, List<BigInteger>> actions = catalog.menuActionIds(appId,
                rows.stream().map(IamMenuEntity::getId).toList());
        Map<BigInteger, List<IamMenuEntity>> children = new LinkedHashMap<>();
        List<IamMenuEntity> roots = new ArrayList<>();
        Set<BigInteger> ids = rows.stream().map(IamMenuEntity::getId).collect(Collectors.toSet());
        for (IamMenuEntity row : rows) {
            if (row.getParentId() == null || !ids.contains(row.getParentId())) {
                roots.add(row);
                continue;
            }
            children.computeIfAbsent(row.getParentId(), key -> new ArrayList<>()).add(row);
        }
        return roots.stream().map(row -> menuTree(row, children, actions)).toList();
    }

    /**
     * 创建菜单并绑定本应用精确操作。
     *
     * @param applicationId 应用 ID
     * @param input 菜单草稿
     * @return 新菜单 ID
     */
    public CreatedResource createMenu(String applicationId, MenuDraft input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_MENU_CREATE);
        long appId = IamIds.require(applicationId);
        return transaction.execute(status -> {
            requireLocked(catalog.lockApplication(appId));
            Long parentId = requireMenuParent(appId, input.parentId());
            long id = access.nextId();
            catalog.insertMenu(menuEntity(id, appId, parentId, input, true));
            replaceMenuActions(appId, id, input.actionIds());
            audits.write(actor.context(), access.nextId(), MENU, IamIds.text(id), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.NAME, input.name()), Map.of(MENU, "0"));
            return new CreatedResource(IamIds.text(id), "0");
        });
    }

    /**
     * 整体更新菜单并重验本应用操作引用。
     *
     * @param applicationId 应用 ID
     * @param menuId 菜单 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    public ResourceDetail<MenuRecord> updateMenu(String applicationId, String menuId, MenuUpdateInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_MENU_UPDATE);
        long appId = IamIds.require(applicationId);
        long id = IamIds.require(menuId);
        return transaction.execute(status -> {
            IamMenuEntity current = requireLocked(catalog.lockMenu(appId, id));
            IamIds.requireVersion(input.expectedVersion(), version(current.getVersion()));
            Long parentId = requireMenuParent(appId, input.menu().parentId());
            if (parentId != null && parentId == id) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            if (parentId != null && catalog.isMenuAncestor(appId, parentId, id)) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            MenuDraft draft = input.menu();
            catalog.updateMenu(appId, id, parentId, draft.name(), nullable(draft.path()), nullable(draft.viewPath()),
                    nullable(draft.routeName()), nullable(draft.icon()), draft.kind(), draft.matchMode(),
                    draft.accessMode(), draft.sortOrder(), current.getVersion());
            replaceMenuActions(appId, id, draft.actionIds());
            String next = nextVersion(current.getVersion());
            audits.write(actor.context(), access.nextId(), MENU, menuId, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.getName()), Map.of(AuditField.NAME, draft.name()),
                    Map.of(MENU, next));
            return loadMenu(appId, id);
        });
    }

    /**
     * 删除没有子菜单的菜单。
     *
     * @param applicationId 应用 ID
     * @param menuId 菜单 ID
     * @return 删除前版本
     */
    public CreatedResource deleteMenu(String applicationId, String menuId) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_MENU_DELETE);
        long appId = IamIds.require(applicationId);
        long id = IamIds.require(menuId);
        return transaction.execute(status -> {
            IamMenuEntity current = requireLocked(catalog.lockMenu(appId, id));
            requireUnused(catalog.countChildMenus(appId, id));
            catalog.deleteMenu(appId, id);
            audits.write(actor.context(), access.nextId(), MENU, menuId, AuditChangeType.REMOVE,
                    Map.of(AuditField.NAME, current.getName()), Map.of(),
                    Map.of(MENU, version(current.getVersion())));
            return new CreatedResource(menuId, version(current.getVersion()));
        });
    }

    /**
     * 分页列出套餐。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @param name 套餐名称包含匹配，空白表示不限制
     * @param status {@link ConfigurationStatus} 稳定字面量，空白表示不限制
     * @return 套餐页
     */
    public PageResponse<ResourceDetail<PlanRecord>> listPlans(int page, int pageSize, String name, String status) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_PLAN_READ);
        IamPages.require(page, pageSize);
        Page<IamPlanEntity> rows = catalog.pagePlans(page, pageSize, name, IamFilters.enabledOf(status));
        List<ResourceDetail<PlanRecord>> items = new ArrayList<>();
        for (IamPlanEntity row : rows.getRecords()) {
            items.add(IamDetails.of(plan(row, texts(catalog.planApplicationIds(row.getId().longValue()))),
                    version(row.getVersion())));
        }
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    /**
     * 读取套餐详情。
     *
     * @param id 套餐 ID
     * @return 套餐详情
     */
    public ResourceDetail<PlanRecord> getPlan(String id) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_PLAN_READ);
        return loadPlan(IamIds.require(id));
    }

    /**
     * 创建套餐。修改套餐不会改变既有租户开通。
     *
     * @param input 套餐草稿
     * @return 新套餐 ID
     */
    public CreatedResource createPlan(PlanDraft input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_PLAN_CREATE);
        return transaction.execute(status -> {
            long id = access.nextId();
            IamPlanEntity entity = new IamPlanEntity();
            entity.setId(BigInteger.valueOf(id));
            entity.setName(input.name());
            entity.setDescription(nullable(input.description()));
            entity.setEnabled(input.status() != ConfigurationStatus.DISABLED);
            catalog.insertPlan(entity);
            replacePlanApplications(id, input.applicationIds());
            audits.write(actor.context(), access.nextId(), PLAN, IamIds.text(id), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.NAME, input.name()), Map.of(PLAN, "0"));
            return new CreatedResource(IamIds.text(id), "0");
        });
    }

    /**
     * 整体替换套餐应用清单，不影响已开通租户。
     *
     * @param id 套餐 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    public ResourceDetail<PlanRecord> updatePlan(String id, PlanUpdateInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_PLAN_UPDATE);
        long planId = IamIds.require(id);
        return transaction.execute(status -> {
            IamPlanEntity current = requireLocked(catalog.lockPlan(planId));
            IamIds.requireVersion(input.expectedVersion(), version(current.getVersion()));
            ConfigurationStatus nextStatus = input.plan().status() == null
                    ? statusOf(current.getEnabled()) : input.plan().status();
            catalog.updatePlan(planId, input.plan().name(), nullable(input.plan().description()),
                    nextStatus == ConfigurationStatus.ENABLED, current.getVersion());
            replacePlanApplications(planId, input.plan().applicationIds());
            String next = nextVersion(current.getVersion());
            audits.write(actor.context(), access.nextId(), PLAN, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.getName(), AuditField.STATUS, statusOf(current.getEnabled()).name()),
                    Map.of(AuditField.NAME, input.plan().name(), AuditField.STATUS, nextStatus.name()),
                    Map.of(PLAN, next));
            return loadPlan(planId);
        });
    }

    private ResourceDetail<ApplicationRecord> loadApplication(long id) {
        IamApplicationEntity row = catalog.findApplication(id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return IamDetails.of(application(row), version(row.getVersion()));
    }

    private ResourceDetail<ResourceRecord> loadResource(long applicationId, long id) {
        IamResourceEntity row = catalog.findResource(applicationId, id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return IamDetails.of(resource(row), version(row.getVersion()));
    }

    private ResourceDetail<ActionRecord> loadAction(long applicationId, long id) {
        IamActionEntity row = catalog.findAction(applicationId, id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return IamDetails.of(action(row), version(row.getVersion()));
    }

    private ResourceDetail<MenuRecord> loadMenu(long applicationId, long id) {
        IamMenuEntity row = catalog.findMenu(applicationId, id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return IamDetails.of(menu(row, texts(catalog.menuActionIds(applicationId, id))), version(row.getVersion()));
    }

    private ResourceDetail<PlanRecord> loadPlan(long id) {
        IamPlanEntity row = catalog.findPlan(id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return IamDetails.of(plan(row, texts(catalog.planApplicationIds(id))), version(row.getVersion()));
    }

    private void requireApplication(long id) {
        if (!catalog.existsApplication(id)) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
    }

    private static <T> T requireLocked(T row) {
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return row;
    }

    private static void requireUnused(long count) {
        if (count > 0) {
            throw new BizException(IamReasonCode.OBJECT_IN_USE);
        }
    }

    private Long requireMenuParent(long applicationId, String parentId) {
        if (parentId == null || parentId.isBlank()) {
            return null;
        }
        long id = IamIds.require(parentId);
        if (!catalog.existsMenu(applicationId, id)) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return id;
    }

    private void replaceMenuActions(long applicationId, long menuId, List<String> actionIds) {
        List<Long> ids = new ArrayList<>();
        if (actionIds != null) {
            for (String actionId : actionIds) {
                long id = IamIds.require(actionId);
                if (!catalog.existsAction(applicationId, id)) {
                    throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
                }
                ids.add(id);
            }
        }
        catalog.replaceMenuActions(applicationId, menuId, actionIds == null ? null : ids);
    }

    private void replacePlanApplications(long planId, List<String> applicationIds) {
        List<Long> ids = new ArrayList<>();
        if (applicationIds != null) {
            for (String applicationId : applicationIds) {
                long id = IamIds.require(applicationId);
                requireApplication(id);
                ids.add(id);
            }
        }
        catalog.replacePlanApplications(planId, applicationIds == null ? null : ids);
    }

    private static IamMenuEntity menuEntity(long id, long applicationId, Long parentId, MenuDraft input,
                                            boolean enabled) {
        IamMenuEntity entity = new IamMenuEntity();
        entity.setId(BigInteger.valueOf(id));
        entity.setApplicationId(BigInteger.valueOf(applicationId));
        entity.setParentId(parentId == null ? null : BigInteger.valueOf(parentId));
        entity.setName(input.name());
        entity.setPath(nullable(input.path()));
        entity.setViewPath(nullable(input.viewPath()));
        entity.setRouteName(nullable(input.routeName()));
        entity.setIcon(nullable(input.icon()));
        entity.setKind(input.kind());
        entity.setMatchMode(input.matchMode());
        entity.setAccessMode(input.accessMode());
        entity.setSortOrder(input.sortOrder());
        entity.setEnabled(enabled);
        return entity;
    }

    private static ApplicationRecord application(IamApplicationEntity row) {
        return new ApplicationRecord(text(row.getId()), row.getCode(), row.getDomain(), row.getName(),
                row.getDescription(), row.getIcon(), row.getSortOrder() == null ? 0 : row.getSortOrder(),
                Boolean.TRUE.equals(row.getBaseline()), statusOf(row.getEnabled()));
    }

    private static ResourceRecord resource(IamResourceEntity row) {
        List<ScopeKind> scopes = IamJson.read(row.getScopeCapabilities(), SCOPES);
        List<FieldCapability> fields = IamJson.read(row.getFieldCapabilities(), FIELDS);
        return new ResourceRecord(text(row.getId()), text(row.getApplicationId()), row.getCode(), row.getName(),
                scopes == null ? List.of() : scopes, fields == null ? List.of() : fields, statusOf(row.getEnabled()));
    }

    private static ActionRecord action(IamActionEntity row) {
        return new ActionRecord(text(row.getId()), text(row.getApplicationId()), text(row.getResourceId()),
                row.getCode(), row.getName(), statusOf(row.getEnabled()));
    }

    private MenuTreeNode menuTree(IamMenuEntity row, Map<BigInteger, List<IamMenuEntity>> children,
                                 Map<BigInteger, List<BigInteger>> actions) {
        List<MenuTreeNode> nodes = children.getOrDefault(row.getId(), List.of()).stream()
                .map(child -> menuTree(child, children, actions)).toList();
        return MenuTreeNode.of(IamDetails.of(menu(row, texts(actions.getOrDefault(row.getId(), List.of()))),
                version(row.getVersion())), nodes);
    }

    private static MenuRecord menu(IamMenuEntity row, List<String> actionIds) {
        return new MenuRecord(text(row.getId()), text(row.getApplicationId()),
                row.getParentId() == null ? null : text(row.getParentId()), row.getName(), row.getKind(),
                row.getPath(), row.getViewPath(), row.getRouteName(), row.getIcon(), row.getAccessMode(),
                row.getMatchMode(), actionIds == null ? List.of() : new ArrayList<>(actionIds),
                row.getSortOrder() == null ? 0 : row.getSortOrder(), statusOf(row.getEnabled()));
    }

    private static PlanRecord plan(IamPlanEntity row, List<String> applicationIds) {
        return new PlanRecord(text(row.getId()), row.getName(), row.getDescription(), applicationIds,
                statusOf(row.getEnabled()));
    }

    private static ConfigurationStatus statusOf(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? ConfigurationStatus.ENABLED : ConfigurationStatus.DISABLED;
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

    private static List<String> texts(List<BigInteger> ids) {
        return ids.stream().map(CatalogService::text).toList();
    }

    private static String nullable(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
