package com.ingot.cloud.iam.catalog;

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
import com.ingot.framework.commons.model.iam.ActionDraft;
import com.ingot.framework.commons.model.iam.ActionMatchMode;
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
import com.ingot.framework.commons.model.iam.MenuAccessMode;
import com.ingot.framework.commons.model.iam.MenuDraft;
import com.ingot.framework.commons.model.iam.MenuKind;
import com.ingot.framework.commons.model.iam.MenuRecord;
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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
public class JdbcCatalogService {
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
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、审计、目录库与事务。
     *
     * @param access 当前身份与 ACTION
     * @param audits 同事务审计
     * @param changes 应用/操作启停后的授权失效
     * @param dataSource IAM 目标库
     * @param transactionManager 同一数据源事务
     */
    public JdbcCatalogService(IamAccess access, IamAuditWriter audits, AuthorizationChangeNotifier changes,
                              DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.access = access;
        this.audits = audits;
        this.changes = changes;
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 分页列出应用目录。
     *
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @return 应用详情页
     */
    public PageResponse<ResourceDetail<ApplicationRecord>> listApplications(int page, int pageSize) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_APPLICATION_READ);
        IamPages.require(page, pageSize);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM iam_application", Map.of(), Long.class);
        List<ResourceDetail<ApplicationRecord>> items = jdbc.query("""
                SELECT id,code,domain,name,description,icon,sort_order,baseline,enabled,version
                  FROM iam_application ORDER BY sort_order,id LIMIT :limit OFFSET :offset
                """, Map.of("limit", pageSize, "offset", IamPages.offset(page, pageSize)),
                (row, index) -> IamDetails.of(application(row), row.getString("version")));
        return IamPages.details(items, total == null ? 0 : total, page, pageSize);
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
            try {
                jdbc.update("""
                        INSERT INTO iam_application(id,code,domain,name,description,icon,sort_order,baseline,enabled)
                        VALUES (:id,:code,:domain,:name,:description,:icon,:sortOrder,:baseline,TRUE)
                        """, applicationParameters(id, input.code(), input.domain(), input.name(),
                        input.description(), input.icon(), input.sortOrder(), input.baseline()));
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
            ApplicationRow current = lockApplication(applicationId);
            IamIds.requireVersion(input.expectedVersion(), current.version());
            if (current.domain() == AuthorizationDomain.PLATFORM && input.baseline()) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            jdbc.update("""
                    UPDATE iam_application SET name=:name,description=:description,icon=:icon,sort_order=:sortOrder,
                           baseline=:baseline,version=version+1 WHERE id=:id
                    """, applicationParameters(applicationId, current.code(), current.domain(), input.name(),
                    input.description(), input.icon(), input.sortOrder(), input.baseline()));
            String version = nextVersion(current.version());
            audits.write(actor.context(), access.nextId(), APPLICATION, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.name()), Map.of(AuditField.NAME, input.name()),
                    Map.of(APPLICATION, version));
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
            ApplicationRow current = lockApplication(applicationId);
            IamIds.requireVersion(input.expectedVersion(), current.version());
            boolean enabled = input.status() == ConfigurationStatus.ENABLED;
            if (current.enabled() == enabled) {
                return new CreatedResource(id, current.version());
            }
            jdbc.update("UPDATE iam_application SET enabled=:enabled,version=version+1 WHERE id=:id",
                    Map.of("enabled", enabled, "id", applicationId));
            String version = nextVersion(current.version());
            audits.write(actor.context(), access.nextId(), APPLICATION, id,
                    enabled ? AuditChangeType.ENABLE : AuditChangeType.DISABLE,
                    Map.of(AuditField.STATUS, statusOf(current.enabled()).name()),
                    Map.of(AuditField.STATUS, input.status().name()), Map.of(APPLICATION, version));
            changes.markAll();
            return new CreatedResource(id, version);
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
            ApplicationRow current = lockApplication(applicationId);
            requireUnused("SELECT COUNT(*) FROM iam_resource WHERE application_id=:id", applicationId);
            requireUnused("SELECT COUNT(*) FROM iam_tenant_app_entitlement WHERE application_id=:id", applicationId);
            requireUnused("SELECT COUNT(*) FROM iam_plan_application WHERE application_id=:id", applicationId);
            requireUnused("SELECT COUNT(*) FROM iam_menu WHERE application_id=:id", applicationId);
            jdbc.update("DELETE FROM iam_application WHERE id=:id", Map.of("id", applicationId));
            audits.write(actor.context(), access.nextId(), APPLICATION, id, AuditChangeType.REMOVE,
                    Map.of(AuditField.NAME, current.name()), Map.of(), Map.of(APPLICATION, current.version()));
            return new CreatedResource(id, current.version());
        });
    }

    /**
     * 列出应用内资源。
     *
     * @param applicationId 应用 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 资源页
     */
    public PageResponse<ResourceDetail<ResourceRecord>> listResources(String applicationId, int page, int pageSize) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_RESOURCE_READ);
        long id = IamIds.require(applicationId);
        requireApplication(id);
        IamPages.require(page, pageSize);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM iam_resource WHERE application_id=:id",
                Map.of("id", id), Long.class);
        List<ResourceDetail<ResourceRecord>> items = jdbc.query("""
                SELECT id,application_id,code,name,scope_capabilities,field_capabilities,enabled,version
                  FROM iam_resource WHERE application_id=:id ORDER BY id LIMIT :limit OFFSET :offset
                """, Map.of("id", id, "limit", pageSize, "offset", IamPages.offset(page, pageSize)),
                (row, index) -> IamDetails.of(resource(row), row.getString("version")));
        return IamPages.details(items, total == null ? 0 : total, page, pageSize);
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
            lockApplication(appId);
            long id = access.nextId();
            try {
                jdbc.update("""
                        INSERT INTO iam_resource(id,application_id,code,name,scope_capabilities,field_capabilities,enabled)
                        VALUES (:id,:applicationId,:code,:name,:scopes,:fields,TRUE)
                        """, Map.of("id", id, "applicationId", appId, "code", input.code(), "name", input.name(),
                        "scopes", IamJson.array(input.scopeCapabilities()),
                        "fields", IamJson.array(input.fieldCapabilities())));
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
            ResourceRow current = lockResource(appId, id);
            IamIds.requireVersion(input.expectedVersion(), current.version());
            jdbc.update("""
                    UPDATE iam_resource SET name=:name,scope_capabilities=:scopes,field_capabilities=:fields,
                           version=version+1 WHERE application_id=:applicationId AND id=:id
                    """, Map.of("name", input.name(), "scopes", IamJson.array(input.scopeCapabilities()),
                    "fields", IamJson.array(input.fieldCapabilities()), "applicationId", appId, "id", id));
            String version = nextVersion(current.version());
            audits.write(actor.context(), access.nextId(), RESOURCE, resourceId, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.name()), Map.of(AuditField.NAME, input.name()),
                    Map.of(RESOURCE, version));
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
            ResourceRow current = lockResource(appId, id);
            requireUnused("SELECT COUNT(*) FROM iam_action WHERE application_id=:applicationId AND resource_id=:id",
                    Map.of("applicationId", appId, "id", id));
            jdbc.update("DELETE FROM iam_resource WHERE application_id=:applicationId AND id=:id",
                    Map.of("applicationId", appId, "id", id));
            audits.write(actor.context(), access.nextId(), RESOURCE, resourceId, AuditChangeType.REMOVE,
                    Map.of(AuditField.NAME, current.name()), Map.of(), Map.of(RESOURCE, current.version()));
            return new CreatedResource(resourceId, current.version());
        });
    }

    /**
     * 列出应用内精确操作。
     *
     * @param applicationId 应用 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 操作页
     */
    public PageResponse<ResourceDetail<ActionRecord>> listActions(String applicationId, int page, int pageSize) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACTION_READ);
        long appId = IamIds.require(applicationId);
        requireApplication(appId);
        IamPages.require(page, pageSize);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM iam_action WHERE application_id=:id",
                Map.of("id", appId), Long.class);
        List<ResourceDetail<ActionRecord>> items = jdbc.query("""
                SELECT id,application_id,resource_id,code,name,enabled,version
                  FROM iam_action WHERE application_id=:id ORDER BY id LIMIT :limit OFFSET :offset
                """, Map.of("id", appId, "limit", pageSize, "offset", IamPages.offset(page, pageSize)),
                (row, index) -> IamDetails.of(action(row), row.getString("version")));
        return IamPages.details(items, total == null ? 0 : total, page, pageSize);
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
            lockResource(appId, resourceId);
            long id = access.nextId();
            try {
                jdbc.update("""
                        INSERT INTO iam_action(id,application_id,resource_id,code,name,enabled)
                        VALUES (:id,:applicationId,:resourceId,:code,:name,TRUE)
                        """, Map.of("id", id, "applicationId", appId, "resourceId", resourceId,
                        "code", input.code(), "name", input.name()));
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
            ActionRow current = lockAction(appId, id);
            IamIds.requireVersion(input.expectedVersion(), current.version());
            jdbc.update("UPDATE iam_action SET name=:name,version=version+1 WHERE application_id=:applicationId AND id=:id",
                    Map.of("name", input.name(), "applicationId", appId, "id", id));
            String version = nextVersion(current.version());
            audits.write(actor.context(), access.nextId(), ACTION, actionId, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.name()), Map.of(AuditField.NAME, input.name()),
                    Map.of(ACTION, version));
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
            ActionRow current = lockAction(appId, id);
            IamIds.requireVersion(input.expectedVersion(), current.version());
            boolean enabled = input.status() == ConfigurationStatus.ENABLED;
            if (current.enabled() == enabled) {
                return new CreatedResource(actionId, current.version());
            }
            jdbc.update("UPDATE iam_action SET enabled=:enabled,version=version+1 WHERE application_id=:applicationId AND id=:id",
                    Map.of("enabled", enabled, "applicationId", appId, "id", id));
            String version = nextVersion(current.version());
            audits.write(actor.context(), access.nextId(), ACTION, actionId,
                    enabled ? AuditChangeType.ENABLE : AuditChangeType.DISABLE,
                    Map.of(AuditField.STATUS, statusOf(current.enabled()).name()),
                    Map.of(AuditField.STATUS, input.status().name()), Map.of(ACTION, version));
            changes.markAll();
            return new CreatedResource(actionId, version);
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
            ActionRow current = lockAction(appId, id);
            requireUnused("SELECT COUNT(*) FROM iam_menu_action WHERE application_id=:applicationId AND action_id=:id",
                    Map.of("applicationId", appId, "id", id));
            requireUnused("SELECT COUNT(*) FROM iam_role_grant WHERE action_id=:id", Map.of("id", id));
            requireUnused("SELECT COUNT(*) FROM iam_role_delta WHERE action_id=:id", Map.of("id", id));
            jdbc.update("DELETE FROM iam_action WHERE application_id=:applicationId AND id=:id",
                    Map.of("applicationId", appId, "id", id));
            audits.write(actor.context(), access.nextId(), ACTION, actionId, AuditChangeType.REMOVE,
                    Map.of(AuditField.NAME, current.name()), Map.of(), Map.of(ACTION, current.version()));
            return new CreatedResource(actionId, current.version());
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
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM iam_menu WHERE application_id=:id",
                Map.of("id", appId), Long.class);
        List<ResourceDetail<MenuRecord>> items = jdbc.query("""
                SELECT id,application_id,parent_id,name,path,view_path,route_name,icon,kind,match_mode,access_mode,
                       sort_order,enabled,version
                  FROM iam_menu WHERE application_id=:id ORDER BY sort_order,id LIMIT :limit OFFSET :offset
                """, Map.of("id", appId, "limit", pageSize, "offset", IamPages.offset(page, pageSize)),
                (row, index) -> IamDetails.of(menu(row, menuActions(appId, row.getLong("id"))), row.getString("version")));
        return IamPages.details(items, total == null ? 0 : total, page, pageSize);
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
            lockApplication(appId);
            Long parentId = requireMenuParent(appId, input.parentId());
            long id = access.nextId();
            jdbc.update("""
                    INSERT INTO iam_menu(id,application_id,parent_id,name,path,view_path,route_name,icon,kind,match_mode,
                                         access_mode,sort_order,enabled)
                    VALUES (:id,:applicationId,:parentId,:name,:path,:viewPath,:routeName,:icon,:kind,:matchMode,
                            :accessMode,:sortOrder,TRUE)
                    """, menuParameters(id, appId, parentId, input));
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
            MenuRow current = lockMenu(appId, id);
            IamIds.requireVersion(input.expectedVersion(), current.version());
            Long parentId = requireMenuParent(appId, input.menu().parentId());
            if (parentId != null && parentId == id) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            if (parentId != null && isMenuAncestor(appId, parentId, id)) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            jdbc.update("""
                    UPDATE iam_menu SET parent_id=:parentId,name=:name,path=:path,view_path=:viewPath,route_name=:routeName,
                           icon=:icon,kind=:kind,match_mode=:matchMode,access_mode=:accessMode,sort_order=:sortOrder,
                           version=version+1
                     WHERE application_id=:applicationId AND id=:id
                    """, menuParameters(id, appId, parentId, input.menu()));
            replaceMenuActions(appId, id, input.menu().actionIds());
            String version = nextVersion(current.version());
            audits.write(actor.context(), access.nextId(), MENU, menuId, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.name()), Map.of(AuditField.NAME, input.menu().name()),
                    Map.of(MENU, version));
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
            MenuRow current = lockMenu(appId, id);
            requireUnused("SELECT COUNT(*) FROM iam_menu WHERE application_id=:applicationId AND parent_id=:id",
                    Map.of("applicationId", appId, "id", id));
            jdbc.update("DELETE FROM iam_menu_action WHERE application_id=:applicationId AND menu_id=:id",
                    Map.of("applicationId", appId, "id", id));
            jdbc.update("DELETE FROM iam_menu WHERE application_id=:applicationId AND id=:id",
                    Map.of("applicationId", appId, "id", id));
            audits.write(actor.context(), access.nextId(), MENU, menuId, AuditChangeType.REMOVE,
                    Map.of(AuditField.NAME, current.name()), Map.of(), Map.of(MENU, current.version()));
            return new CreatedResource(menuId, current.version());
        });
    }

    /**
     * 分页列出套餐。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @return 套餐页
     */
    public PageResponse<ResourceDetail<PlanRecord>> listPlans(int page, int pageSize) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_PLAN_READ);
        IamPages.require(page, pageSize);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM iam_plan", Map.of(), Long.class);
        List<ResourceDetail<PlanRecord>> items = jdbc.query(
                "SELECT id,name,description,enabled,version FROM iam_plan ORDER BY id LIMIT :limit OFFSET :offset",
                Map.of("limit", pageSize, "offset", IamPages.offset(page, pageSize)),
                (row, index) -> IamDetails.of(plan(row, planApplications(row.getLong("id"))), row.getString("version")));
        return IamPages.details(items, total == null ? 0 : total, page, pageSize);
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
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("id", id);
            parameters.put("name", input.name());
            parameters.put("description", nullable(input.description()));
            jdbc.update("INSERT INTO iam_plan(id,name,description,enabled) VALUES (:id,:name,:description,TRUE)",
                    parameters);
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
            PlanRow current = lockPlan(planId);
            IamIds.requireVersion(input.expectedVersion(), current.version());
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("name", input.plan().name());
            parameters.put("description", nullable(input.plan().description()));
            parameters.put("id", planId);
            jdbc.update("UPDATE iam_plan SET name=:name,description=:description,version=version+1 WHERE id=:id",
                    parameters);
            replacePlanApplications(planId, input.plan().applicationIds());
            String version = nextVersion(current.version());
            audits.write(actor.context(), access.nextId(), PLAN, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.name()), Map.of(AuditField.NAME, input.plan().name()),
                    Map.of(PLAN, version));
            return loadPlan(planId);
        });
    }

    private ResourceDetail<ApplicationRecord> loadApplication(long id) {
        List<ResourceDetail<ApplicationRecord>> rows = jdbc.query("""
                SELECT id,code,domain,name,description,icon,sort_order,baseline,enabled,version
                  FROM iam_application WHERE id=:id
                """, Map.of("id", id), (row, index) -> IamDetails.of(application(row), row.getString("version")));
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return rows.getFirst();
    }

    private ResourceDetail<ResourceRecord> loadResource(long applicationId, long id) {
        List<ResourceDetail<ResourceRecord>> rows = jdbc.query("""
                SELECT id,application_id,code,name,scope_capabilities,field_capabilities,enabled,version
                  FROM iam_resource WHERE application_id=:applicationId AND id=:id
                """, Map.of("applicationId", applicationId, "id", id),
                (row, index) -> IamDetails.of(resource(row), row.getString("version")));
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return rows.getFirst();
    }

    private ResourceDetail<ActionRecord> loadAction(long applicationId, long id) {
        List<ResourceDetail<ActionRecord>> rows = jdbc.query("""
                SELECT id,application_id,resource_id,code,name,enabled,version
                  FROM iam_action WHERE application_id=:applicationId AND id=:id
                """, Map.of("applicationId", applicationId, "id", id),
                (row, index) -> IamDetails.of(action(row), row.getString("version")));
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return rows.getFirst();
    }

    private ResourceDetail<MenuRecord> loadMenu(long applicationId, long id) {
        List<ResourceDetail<MenuRecord>> rows = jdbc.query("""
                SELECT id,application_id,parent_id,name,path,view_path,route_name,icon,kind,match_mode,access_mode,
                       sort_order,enabled,version
                  FROM iam_menu WHERE application_id=:applicationId AND id=:id
                """, Map.of("applicationId", applicationId, "id", id),
                (row, index) -> IamDetails.of(menu(row, menuActions(applicationId, id)), row.getString("version")));
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return rows.getFirst();
    }

    private ResourceDetail<PlanRecord> loadPlan(long id) {
        List<ResourceDetail<PlanRecord>> rows = jdbc.query(
                "SELECT id,name,description,enabled,version FROM iam_plan WHERE id=:id", Map.of("id", id),
                (row, index) -> IamDetails.of(plan(row, planApplications(id)), row.getString("version")));
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return rows.getFirst();
    }

    private ApplicationRow lockApplication(long id) {
        List<ApplicationRow> rows = jdbc.query("""
                SELECT id,code,domain,name,baseline,enabled,version FROM iam_application WHERE id=:id FOR UPDATE
                """, Map.of("id", id), (row, index) -> new ApplicationRow(row.getString("code"),
                AuthorizationDomain.valueOf(row.getString("domain")), row.getString("name"),
                row.getBoolean("baseline"), row.getBoolean("enabled"), row.getString("version")));
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return rows.getFirst();
    }

    private ResourceRow lockResource(long applicationId, long id) {
        List<ResourceRow> rows = jdbc.query("""
                SELECT name,version FROM iam_resource WHERE application_id=:applicationId AND id=:id FOR UPDATE
                """, Map.of("applicationId", applicationId, "id", id),
                (row, index) -> new ResourceRow(row.getString("name"), row.getString("version")));
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return rows.getFirst();
    }

    private ActionRow lockAction(long applicationId, long id) {
        List<ActionRow> rows = jdbc.query("""
                SELECT name,enabled,version FROM iam_action WHERE application_id=:applicationId AND id=:id FOR UPDATE
                """, Map.of("applicationId", applicationId, "id", id),
                (row, index) -> new ActionRow(row.getString("name"), row.getBoolean("enabled"), row.getString("version")));
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return rows.getFirst();
    }

    private MenuRow lockMenu(long applicationId, long id) {
        List<MenuRow> rows = jdbc.query("""
                SELECT name,version FROM iam_menu WHERE application_id=:applicationId AND id=:id FOR UPDATE
                """, Map.of("applicationId", applicationId, "id", id),
                (row, index) -> new MenuRow(row.getString("name"), row.getString("version")));
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return rows.getFirst();
    }

    private PlanRow lockPlan(long id) {
        List<PlanRow> rows = jdbc.query("SELECT name,version FROM iam_plan WHERE id=:id FOR UPDATE", Map.of("id", id),
                (row, index) -> new PlanRow(row.getString("name"), row.getString("version")));
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return rows.getFirst();
    }

    private void requireApplication(long id) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM iam_application WHERE id=:id", Map.of("id", id), Long.class);
        if (count == null || count != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
    }

    private void requireUnused(String sql, long id) {
        requireUnused(sql, Map.of("id", id));
    }

    private void requireUnused(String sql, Map<String, ?> parameters) {
        Long count = jdbc.queryForObject(sql, parameters, Long.class);
        if (count != null && count > 0) {
            throw new BizException(IamReasonCode.OBJECT_IN_USE);
        }
    }

    private Long requireMenuParent(long applicationId, String parentId) {
        if (parentId == null || parentId.isBlank()) {
            return null;
        }
        long id = IamIds.require(parentId);
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM iam_menu WHERE application_id=:applicationId AND id=:id",
                Map.of("applicationId", applicationId, "id", id), Long.class);
        if (count == null || count != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return id;
    }

    private boolean isMenuAncestor(long applicationId, long candidateParent, long menuId) {
        Long current = candidateParent;
        while (current != null) {
            if (current == menuId) {
                return true;
            }
            List<Long> parents = jdbc.query(
                    "SELECT parent_id FROM iam_menu WHERE application_id=:applicationId AND id=:id",
                    Map.of("applicationId", applicationId, "id", current), (row, index) -> {
                        long parent = row.getLong("parent_id");
                        return row.wasNull() ? null : parent;
                    });
            current = parents.isEmpty() ? null : parents.getFirst();
        }
        return false;
    }

    private void replaceMenuActions(long applicationId, long menuId, List<String> actionIds) {
        jdbc.update("DELETE FROM iam_menu_action WHERE application_id=:applicationId AND menu_id=:menuId",
                Map.of("applicationId", applicationId, "menuId", menuId));
        if (actionIds == null) {
            return;
        }
        for (String actionId : actionIds) {
            long id = IamIds.require(actionId);
            Long count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM iam_action WHERE application_id=:applicationId AND id=:id",
                    Map.of("applicationId", applicationId, "id", id), Long.class);
            if (count == null || count != 1) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            jdbc.update("""
                    INSERT INTO iam_menu_action(application_id,menu_id,action_id)
                    VALUES (:applicationId,:menuId,:actionId)
                    """, Map.of("applicationId", applicationId, "menuId", menuId, "actionId", id));
        }
    }

    private List<String> menuActions(long applicationId, long menuId) {
        return jdbc.queryForList("""
                SELECT action_id FROM iam_menu_action
                 WHERE application_id=:applicationId AND menu_id=:menuId ORDER BY action_id
                """, Map.of("applicationId", applicationId, "menuId", menuId), Long.class)
                .stream().map(IamIds::text).toList();
    }

    private void replacePlanApplications(long planId, List<String> applicationIds) {
        jdbc.update("DELETE FROM iam_plan_application WHERE plan_id=:id", Map.of("id", planId));
        if (applicationIds == null) {
            return;
        }
        for (String applicationId : applicationIds) {
            long id = IamIds.require(applicationId);
            requireApplication(id);
            jdbc.update("INSERT INTO iam_plan_application(plan_id,application_id) VALUES (:planId,:applicationId)",
                    Map.of("planId", planId, "applicationId", id));
        }
    }

    private List<String> planApplications(long planId) {
        return jdbc.queryForList("SELECT application_id FROM iam_plan_application WHERE plan_id=:id ORDER BY application_id",
                Map.of("id", planId), Long.class).stream().map(IamIds::text).toList();
    }

    private Map<String, Object> applicationParameters(long id, String code, AuthorizationDomain domain, String name,
                                                      String description, String icon, int sortOrder, boolean baseline) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", id);
        parameters.put("code", code);
        parameters.put("domain", domain.name());
        parameters.put("name", name);
        parameters.put("description", nullable(description));
        parameters.put("icon", nullable(icon));
        parameters.put("sortOrder", sortOrder);
        parameters.put("baseline", baseline);
        return parameters;
    }

    private Map<String, Object> menuParameters(long id, long applicationId, Long parentId, MenuDraft input) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", id);
        parameters.put("applicationId", applicationId);
        parameters.put("parentId", parentId);
        parameters.put("name", input.name());
        parameters.put("path", nullable(input.path()));
        parameters.put("viewPath", nullable(input.viewPath()));
        parameters.put("routeName", nullable(input.routeName()));
        parameters.put("icon", nullable(input.icon()));
        parameters.put("kind", input.kind().name());
        parameters.put("matchMode", input.matchMode().name());
        parameters.put("accessMode", input.accessMode().name());
        parameters.put("sortOrder", input.sortOrder());
        return parameters;
    }

    private static ApplicationRecord application(java.sql.ResultSet row) throws java.sql.SQLException {
        return new ApplicationRecord(row.getString("id"), row.getString("code"),
                AuthorizationDomain.valueOf(row.getString("domain")), row.getString("name"),
                row.getString("description"), row.getString("icon"), row.getInt("sort_order"),
                row.getBoolean("baseline"), statusOf(row.getBoolean("enabled")));
    }

    private static ResourceRecord resource(java.sql.ResultSet row) throws java.sql.SQLException {
        List<ScopeKind> scopes = IamJson.read(row.getString("scope_capabilities"), SCOPES);
        List<FieldCapability> fields = IamJson.read(row.getString("field_capabilities"), FIELDS);
        return new ResourceRecord(row.getString("id"), row.getString("application_id"), row.getString("code"),
                row.getString("name"), scopes == null ? List.of() : scopes, fields == null ? List.of() : fields,
                statusOf(row.getBoolean("enabled")));
    }

    private static ActionRecord action(java.sql.ResultSet row) throws java.sql.SQLException {
        return new ActionRecord(row.getString("id"), row.getString("application_id"), row.getString("resource_id"),
                row.getString("code"), row.getString("name"), statusOf(row.getBoolean("enabled")));
    }

    private static MenuRecord menu(java.sql.ResultSet row, List<String> actionIds) throws java.sql.SQLException {
        String parent = row.getString("parent_id");
        return new MenuRecord(row.getString("id"), row.getString("application_id"),
                parent == null || parent.isBlank() ? null : parent, row.getString("name"),
                MenuKind.valueOf(row.getString("kind")), row.getString("path"), row.getString("view_path"),
                row.getString("route_name"), row.getString("icon"),
                MenuAccessMode.valueOf(row.getString("access_mode")),
                ActionMatchMode.valueOf(row.getString("match_mode")),
                actionIds == null ? List.of() : new ArrayList<>(actionIds), row.getInt("sort_order"),
                statusOf(row.getBoolean("enabled")));
    }

    private static PlanRecord plan(java.sql.ResultSet row, List<String> applicationIds) throws java.sql.SQLException {
        return new PlanRecord(row.getString("id"), row.getString("name"), row.getString("description"),
                applicationIds, statusOf(row.getBoolean("enabled")));
    }

    private static ConfigurationStatus statusOf(boolean enabled) {
        return enabled ? ConfigurationStatus.ENABLED : ConfigurationStatus.DISABLED;
    }

    private static String nextVersion(String current) {
        return Long.toString(Long.parseLong(current) + 1);
    }

    private static Object nullable(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record ApplicationRow(String code, AuthorizationDomain domain, String name, boolean baseline,
                                  boolean enabled, String version) {
    }

    private record ResourceRow(String name, String version) {
    }

    private record ActionRow(String name, boolean enabled, String version) {
    }

    private record MenuRow(String name, String version) {
    }

    private record PlanRow(String name, String version) {
    }
}
