package com.ingot.cloud.iam.infrastructure.persistence;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamActionEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamApplicationEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamMenuEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamPlanEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamResourceEntity;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.domain.catalog.MenuAccessMode;
import com.ingot.cloud.iam.domain.catalog.ActionMatchMode;
import com.ingot.cloud.iam.domain.catalog.MenuKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


/**
 * <p>聚合目录持久化接口，应用、资源、操作、菜单、套餐和跨表清理分别委托专用仓储。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class CatalogRepository {
    private final CatalogApplicationRepository application;
    private final CatalogResourceRepository resource;
    private final CatalogActionRepository action;
    private final CatalogMenuRepository menu;
    private final CatalogPlanRepository plan;
    private final CatalogCleanupRepository cleanup;


    /**
     * 分页列出应用目录。
     *
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @param domain 管理域，必填
     * @param name 应用名称包含匹配，空白表示不限制
     * @param enabled 启用状态，空表示不限制
     * @param baseline 是否组织默认开通，空表示不限制
     * @return 应用页
     */
    public Page<IamApplicationEntity> pageApplications(int page, int pageSize, AuthorizationDomain domain, String name,
                                                       Boolean enabled, Boolean baseline) {
        return application.pageApplications(page, pageSize, domain, name, enabled, baseline);
    }

    /**
     * 读取单个应用。
     *
     * @param id 应用 ID
     * @return 应用行，不存在时为空
     */
    public IamApplicationEntity findApplication(long id) {
        return application.findApplication(id);
    }

    /**
     * 按应用 ID 批量读取目录名称与图标，供开通列表展示。
     *
     * @param applicationIds 应用 ID，可含空值与重复
     * @return 有名称或图标的目录项；找不到应用，或名称与图标都空白时不入表
     */
    public Map<BigInteger, ApplicationDisplay> applicationDisplays(Collection<BigInteger> applicationIds) {
        return application.applicationDisplays(applicationIds);
    }

    /**
     * 按目录名称包含匹配应用 ID，供开通列表筛选。
     *
     * @param name 已规范化的名称关键字
     * @return 命中的应用 ID；关键字空白时为空
     */
    public List<BigInteger> applicationIdsByName(String name) {
        return application.applicationIdsByName(name);
    }

    /**
     * 判断应用是否存在。
     *
     * @param id 应用 ID
     * @return 存在时为 true
     */
    public boolean existsApplication(long id) {
        return application.existsApplication(id);
    }

    /**
     * 插入应用目录项，版本由数据库默认值承担。
     *
     * @param entity 待插入行
     */
    public void insertApplication(IamApplicationEntity entity) {
        application.insertApplication(entity);
    }

    /**
     * 锁定应用行，调用方须处于事务中。
     *
     * @param id 应用 ID
     * @return 锁定行，不存在时为空
     */
    public IamApplicationEntity lockApplication(long id) {
        return application.lockApplication(id);
    }

    /**
     * 更新应用展示信息并将版本加一。
     *
     * @param id 应用 ID
     * @param name 名称
     * @param description 说明，可空
     * @param icon 图标，可空
     * @param sortOrder 排序
     * @param baseline 是否基础开通
     * @param currentVersion 锁定后的当前版本
     */
    public void updateApplication(long id, String name, String description, String icon, int sortOrder,
                                  boolean baseline, BigInteger currentVersion) {
        application.updateApplication(id, name, description, icon, sortOrder, baseline, currentVersion);
    }

    /**
     * 更新应用启用状态并将版本加一。
     *
     * @param id 应用 ID
     * @param enabled 是否启用
     * @param currentVersion 锁定后的当前版本
     */
    public void updateApplicationEnabled(long id, boolean enabled, BigInteger currentVersion) {
        application.updateApplicationEnabled(id, enabled, currentVersion);
    }

    /**
     * 删除应用。
     *
     * @param id 应用 ID
     */
    public void deleteApplication(long id) {
        application.deleteApplication(id);
    }

    /**
     * 按依赖顺序清除该应用全部关联后删除应用行。
     *
     * @param applicationId 应用 ID
     */
    public void purgeApplication(long applicationId) {
        cleanup.purgeApplication(applicationId);
    }

    /**
     * 统计应用下资源数。
     *
     * @param applicationId 应用 ID
     * @return 资源条数
     */
    public long countResources(long applicationId) {
        return resource.countResources(applicationId);
    }

    /**
     * 统计引用该应用的开通数。
     *
     * @param applicationId 应用 ID
     * @return 开通条数
     */
    public long countEntitlements(long applicationId) {
        return application.countEntitlements(applicationId);
    }

    /**
     * 统计套餐对该应用的引用。
     *
     * @param applicationId 应用 ID
     * @return 引用条数
     */
    public long countPlanApplications(long applicationId) {
        return plan.countPlanApplications(applicationId);
    }

    /**
     * 统计应用下菜单数。
     *
     * @param applicationId 应用 ID
     * @return 菜单条数
     */
    public long countMenus(long applicationId) {
        return menu.countMenus(applicationId);
    }

    /**
     * 分页列出应用内资源。
     *
     * @param applicationId 应用 ID
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @param name 资源名称包含匹配，空白表示不限制
     * @param code 资源编码包含匹配，空白表示不限制
     * @return 资源页
     */
    public Page<IamResourceEntity> pageResources(long applicationId, int page, int pageSize, String name, String code) {
        return resource.pageResources(applicationId, page, pageSize, name, code);
    }

    /**
     * 列出应用内全部启用资源，供角色权限选择一次组树。
     *
     * @param applicationId 应用 ID
     * @return 启用资源行
     */
    public List<IamResourceEntity> listEnabledResources(long applicationId) {
        return resource.listEnabledResources(applicationId);
    }

    /**
     * 列出应用内全部资源，供操作目录组树。
     *
     * @param applicationId 应用 ID
     * @return 资源行
     */
    public List<IamResourceEntity> listResources(long applicationId) {
        return resource.listResources(applicationId);
    }

    /**
     * 按 ID 批量读取资源。
     *
     * @param ids 资源 ID
     * @return 资源行；空集合返回空列表
     */
    public List<IamResourceEntity> listResourcesByIds(Collection<Long> ids) {
        return resource.listResourcesByIds(ids);
    }

    /**
     * 按 ID 批量读取应用。
     *
     * @param ids 应用 ID
     * @return 应用行；空集合返回空列表
     */
    public List<IamApplicationEntity> listApplicationsByIds(Collection<Long> ids) {
        return application.listApplicationsByIds(ids);
    }

    /**
     * 读取指定应用下的资源。
     *
     * @param applicationId 应用 ID
     * @param id 资源 ID
     * @return 资源行，不存在时为空
     */
    public IamResourceEntity findResource(long applicationId, long id) {
        return resource.findResource(applicationId, id);
    }

    /**
     * 插入资源，版本由数据库默认值承担。
     *
     * @param entity 待插入行
     */
    public void insertResource(IamResourceEntity entity) {
        resource.insertResource(entity);
    }

    /**
     * 锁定资源行。
     *
     * @param applicationId 应用 ID
     * @param id 资源 ID
     * @return 锁定行，不存在时为空
     */
    public IamResourceEntity lockResource(long applicationId, long id) {
        return resource.lockResource(applicationId, id);
    }

    /**
     * 更新资源名称与能力并将版本加一。
     *
     * @param applicationId 应用 ID
     * @param id 资源 ID
     * @param name 名称
     * @param scopes JSON 数组
     * @param fields JSON 数组
     * @param fieldPolicy 是否接入租户字段策略
     * @param currentVersion 锁定后的当前版本
     */
    public void updateResource(long applicationId, long id, String name, String scopes, String fields,
                               boolean fieldPolicy, BigInteger currentVersion) {
        resource.updateResource(applicationId, id, name, scopes, fields, fieldPolicy, currentVersion);
    }

    /**
     * 删除资源。
     *
     * @param applicationId 应用 ID
     * @param id 资源 ID
     */
    public void deleteResource(long applicationId, long id) {
        resource.deleteResource(applicationId, id);
    }

    /**
     * 统计资源下操作数。
     *
     * @param applicationId 应用 ID
     * @param resourceId 资源 ID
     * @return 操作条数
     */
    public long countActions(long applicationId, long resourceId) {
        return action.countActions(applicationId, resourceId);
    }

    /**
     * 分页列出应用内操作。
     *
     * @param applicationId 应用 ID
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @param resourceId 所属资源，空表示不限制
     * @param name 操作名称包含匹配，空白表示不限制
     * @param ids 限定操作 ID，空表示不限制
     * @return 操作页
     */
    public Page<IamActionEntity> pageActions(long applicationId, int page, int pageSize, Long resourceId, String name,
                                             List<Long> ids) {
        return action.pageActions(applicationId, page, pageSize, resourceId, name, ids);
    }

    /**
     * 列出应用内全部操作，供操作目录组树。
     *
     * @param applicationId 应用 ID
     * @return 操作行
     */
    public List<IamActionEntity> listActions(long applicationId) {
        return action.listActions(applicationId);
    }

    /**
     * 列出应用内全部启用操作，供角色权限选择一次组树。
     *
     * @param applicationId 应用 ID
     * @return 启用操作行
     */
    public List<IamActionEntity> listEnabledActions(long applicationId) {
        return action.listEnabledActions(applicationId);
    }

    /**
     * 列出指定资源下的全部操作，供权限树展开。
     *
     * @param applicationId 应用 ID
     * @param resourceId 资源 ID
     * @return 操作行
     */
    public List<IamActionEntity> listActionsByResource(long applicationId, long resourceId) {
        return action.listActionsByResource(applicationId, resourceId);
    }

    /**
     * 列出指定资源集合下的启用操作，供授权选择目录组树。
     *
     * @param applicationId 应用 ID
     * @param resourceIds 资源 ID
     * @return 启用操作行；空集合返回空列表
     */
    public List<IamActionEntity> listEnabledActionsByResourceIds(long applicationId, Collection<Long> resourceIds) {
        return action.listEnabledActionsByResourceIds(applicationId, resourceIds);
    }

    /**
     * 按操作 ID 批量读取，供回显解析。
     *
     * @param ids 操作 ID
     * @return 操作行；空集合返回空列表
     */
    public List<IamActionEntity> listActionsByIds(Collection<Long> ids) {
        return action.listActionsByIds(ids);
    }

    /**
     * 读取指定应用下的操作。
     *
     * @param applicationId 应用 ID
     * @param id 操作 ID
     * @return 操作行，不存在时为空
     */
    public IamActionEntity findAction(long applicationId, long id) {
        return action.findAction(applicationId, id);
    }

    /**
     * 判断应用下操作是否存在。
     *
     * @param applicationId 应用 ID
     * @param id 操作 ID
     * @return 存在时为 true
     */
    public boolean existsAction(long applicationId, long id) {
        return action.existsAction(applicationId, id);
    }

    /**
     * 插入操作，版本由数据库默认值承担。
     *
     * @param entity 待插入行
     */
    public void insertAction(IamActionEntity entity) {
        action.insertAction(entity);
    }

    /**
     * 锁定操作行。
     *
     * @param applicationId 应用 ID
     * @param id 操作 ID
     * @return 锁定行，不存在时为空
     */
    public IamActionEntity lockAction(long applicationId, long id) {
        return action.lockAction(applicationId, id);
    }

    /**
     * 更新操作名称并将版本加一。
     *
     * @param applicationId 应用 ID
     * @param id 操作 ID
     * @param name 名称
     * @param currentVersion 锁定后的当前版本
     */
    public void updateActionName(long applicationId, long id, String name, BigInteger currentVersion) {
        action.updateActionName(applicationId, id, name, currentVersion);
    }

    /**
     * 更新操作启用状态并将版本加一。
     *
     * @param applicationId 应用 ID
     * @param id 操作 ID
     * @param enabled 是否启用
     * @param currentVersion 锁定后的当前版本
     */
    public void updateActionEnabled(long applicationId, long id, boolean enabled, BigInteger currentVersion) {
        action.updateActionEnabled(applicationId, id, enabled, currentVersion);
    }

    /**
     * 删除操作。
     *
     * @param applicationId 应用 ID
     * @param id 操作 ID
     */
    public void deleteAction(long applicationId, long id) {
        action.deleteAction(applicationId, id);
    }

    /**
     * 统计菜单对该操作的引用。
     *
     * @param applicationId 应用 ID
     * @param actionId 操作 ID
     * @return 引用条数
     */
    public long countMenuActions(long applicationId, long actionId) {
        return action.countMenuActions(applicationId, actionId);
    }

    /**
     * 统计角色授权对该操作的引用。
     *
     * @param actionId 操作 ID
     * @return 引用条数
     */
    public long countRoleGrants(long actionId) {
        return action.countRoleGrants(actionId);
    }

    /**
     * 统计角色差异对该操作的引用。
     *
     * @param actionId 操作 ID
     * @return 引用条数
     */
    public long countRoleDeltas(long actionId) {
        return action.countRoleDeltas(actionId);
    }

    /**
     * 分页列出应用菜单。
     *
     * @param applicationId 应用 ID
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @return 菜单页
     */
    public Page<IamMenuEntity> pageMenus(long applicationId, int page, int pageSize) {
        return menu.pageMenus(applicationId, page, pageSize);
    }

    /**
     * 列出应用全部菜单，按排序和 ID 稳定排列。
     *
     * @param applicationId 应用 ID
     * @return 菜单行
     */
    public List<IamMenuEntity> listMenus(long applicationId) {
        return menu.listMenus(applicationId);
    }

    /**
     * 读取指定应用下的菜单。
     *
     * @param applicationId 应用 ID
     * @param id 菜单 ID
     * @return 菜单行，不存在时为空
     */
    public IamMenuEntity findMenu(long applicationId, long id) {
        return menu.findMenu(applicationId, id);
    }

    /**
     * 判断应用下菜单是否存在。
     *
     * @param applicationId 应用 ID
     * @param id 菜单 ID
     * @return 存在时为 true
     */
    public boolean existsMenu(long applicationId, long id) {
        return menu.existsMenu(applicationId, id);
    }

    /**
     * 插入菜单，版本由数据库默认值承担。
     *
     * @param entity 待插入行
     */
    public void insertMenu(IamMenuEntity entity) {
        menu.insertMenu(entity);
    }

    /**
     * 锁定菜单行。
     *
     * @param applicationId 应用 ID
     * @param id 菜单 ID
     * @return 锁定行，不存在时为空
     */
    public IamMenuEntity lockMenu(long applicationId, long id) {
        return menu.lockMenu(applicationId, id);
    }

    /**
     * 更新菜单展示字段并将版本加一。
     *
     * @param applicationId 应用 ID
     * @param id 菜单 ID
     * @param parentId 父菜单，可空
     * @param name 名称
     * @param path 路由路径，可空
     * @param viewPath 视图路径，可空
     * @param routeName 路由名称，可空
     * @param icon 图标，可空
     * @param kind 菜单类型
     * @param matchMode 操作匹配
     * @param accessMode 访问方式
     * @param sortOrder 排序
     * @param currentVersion 锁定后的当前版本
     * @param hidden 是否隐藏导航
     * @param isCache 是否缓存页面
     * @param props 是否透传路径参数
     * @param routeParams 有序参数 JSON
     */
    public void updateMenu(long applicationId, long id, Long parentId, String name, String path, String viewPath,
                           String routeName, String icon, MenuKind kind, ActionMatchMode matchMode,
                           MenuAccessMode accessMode, int sortOrder, BigInteger currentVersion,
                           boolean hidden, boolean isCache, boolean props, String routeParams) {
        menu.updateMenu(applicationId, id, parentId, name, path, viewPath, routeName, icon, kind, matchMode, accessMode, sortOrder, currentVersion, hidden, isCache, props, routeParams);
    }

    /**
     * 判断候选父菜单是否位于当前菜单的子孙链上。
     *
     * @param applicationId 应用 ID
     * @param candidateParent 候选父菜单
     * @param menuId 当前菜单
     * @return 形成环时为 true
     */
    public boolean isMenuAncestor(long applicationId, long candidateParent, long menuId) {
        return menu.isMenuAncestor(applicationId, candidateParent, menuId);
    }

    /**
     * 整体替换菜单绑定的操作。
     *
     * @param applicationId 应用 ID
     * @param menuId 菜单 ID
     * @param actionIds 精确操作 ID，可空表示清空
     */
    public void replaceMenuActions(long applicationId, long menuId, List<Long> actionIds) {
        menu.replaceMenuActions(applicationId, menuId, actionIds);
    }

    /**
     * 读取菜单绑定的操作 ID，按操作 ID 排序。
     *
     * @param applicationId 应用 ID
     * @param menuId 菜单 ID
     * @return 操作 ID
     */
    public List<BigInteger> menuActionIds(long applicationId, long menuId) {
        return menu.menuActionIds(applicationId, menuId);
    }

    /**
     * 批量读取当前页菜单绑定的操作，避免逐行查询。
     *
     * @param applicationId 应用 ID
     * @param menuIds 菜单 ID
     * @return 菜单 ID 到按操作 ID 排序的绑定
     */
    public Map<BigInteger, List<BigInteger>> menuActionIds(long applicationId, List<BigInteger> menuIds) {
        return menu.menuActionIds(applicationId, menuIds);
    }

    /**
     * 统计直接子菜单。
     *
     * @param applicationId 应用 ID
     * @param parentId 父菜单 ID
     * @return 子菜单条数
     */
    public long countChildMenus(long applicationId, long parentId) {
        return menu.countChildMenus(applicationId, parentId);
    }

    /**
     * 删除菜单及其操作绑定。
     *
     * @param applicationId 应用 ID
     * @param id 菜单 ID
     */
    public void deleteMenu(long applicationId, long id) {
        menu.deleteMenu(applicationId, id);
    }

    /**
     * 分页列出套餐。
     *
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @param name 套餐名称包含匹配，空白表示不限制
     * @param enabled 启用状态，空表示不限制
     * @return 套餐页
     */
    public Page<IamPlanEntity> pagePlans(int page, int pageSize, String name, Boolean enabled) {
        return plan.pagePlans(page, pageSize, name, enabled);
    }

    /**
     * 读取套餐。
     *
     * @param id 套餐 ID
     * @return 套餐行，不存在时为空
     */
    public IamPlanEntity findPlan(long id) {
        return plan.findPlan(id);
    }

    /**
     * 插入套餐，版本由数据库默认值承担。
     *
     * @param entity 待插入行
     */
    public void insertPlan(IamPlanEntity entity) {
        plan.insertPlan(entity);
    }

    /**
     * 锁定套餐行。
     *
     * @param id 套餐 ID
     * @return 锁定行，不存在时为空
     */
    public IamPlanEntity lockPlan(long id) {
        return plan.lockPlan(id);
    }

    /**
     * 更新套餐名称、说明与启停状态，并将版本加一。
     *
     * @param id 套餐 ID
     * @param name 名称
     * @param description 说明，可空
     * @param enabled 是否启用
     * @param currentVersion 锁定后的当前版本
     */
    public void updatePlan(long id, String name, String description, boolean enabled, BigInteger currentVersion) {
        plan.updatePlan(id, name, description, enabled, currentVersion);
    }

    /**
     * 整体替换套餐应用清单。
     *
     * @param planId 套餐 ID
     * @param applicationIds 应用 ID
     */
    public void replacePlanApplications(long planId, List<Long> applicationIds) {
        plan.replacePlanApplications(planId, applicationIds);
    }

    /**
     * 读取套餐绑定的应用 ID，按应用 ID 排序。
     *
     * @param planId 套餐 ID
     * @return 应用 ID
     */
    public List<BigInteger> planApplicationIds(long planId) {
        return plan.planApplicationIds(planId);
    }

    /**
     * 列出指定域启用的基础应用。
     *
     * @param domain 授权域
     * @return 基础应用 ID
     */
    public List<BigInteger> enabledBaselineIds(AuthorizationDomain domain) {
        return application.enabledBaselineIds(domain);
    }
}
