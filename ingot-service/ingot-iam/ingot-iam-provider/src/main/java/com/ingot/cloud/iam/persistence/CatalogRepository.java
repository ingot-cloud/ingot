package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.persistence.entity.IamActionEntity;
import com.ingot.cloud.iam.persistence.entity.IamApplicationEntity;
import com.ingot.cloud.iam.persistence.entity.IamMenuActionEntity;
import com.ingot.cloud.iam.persistence.entity.IamMenuEntity;
import com.ingot.cloud.iam.persistence.entity.IamPlanApplicationEntity;
import com.ingot.cloud.iam.persistence.entity.IamPlanEntity;
import com.ingot.cloud.iam.persistence.entity.IamResourceEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleDeltaEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleGrantEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantAppEntitlementEntity;
import com.ingot.cloud.iam.persistence.mapper.IamActionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamApplicationMapper;
import com.ingot.cloud.iam.persistence.mapper.IamMenuActionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamMenuMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlanApplicationMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlanMapper;
import com.ingot.cloud.iam.persistence.mapper.IamResourceMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleDeltaMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleGrantMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantAppEntitlementMapper;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.MenuAccessMode;
import com.ingot.framework.commons.model.iam.ActionMatchMode;
import com.ingot.framework.commons.model.iam.MenuKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>维护应用、资源、操作、菜单与套餐目录行，不把启停写成隐式开通。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class CatalogRepository {
    private final IamApplicationMapper applications;
    private final IamResourceMapper resources;
    private final IamActionMapper actions;
    private final IamMenuMapper menus;
    private final IamMenuActionMapper menuActions;
    private final IamPlanMapper plans;
    private final IamPlanApplicationMapper planApplications;
    private final IamTenantAppEntitlementMapper entitlements;
    private final IamRoleGrantMapper roleGrants;
    private final IamRoleDeltaMapper roleDeltas;

    /**
     * 分页列出应用目录。
     *
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @return 应用页
     */
    public Page<IamApplicationEntity> pageApplications(int page, int pageSize) {
        return applications.selectPage(new Page<>(page, pageSize), Wrappers.<IamApplicationEntity>lambdaQuery()
                .orderByAsc(IamApplicationEntity::getSortOrder)
                .orderByAsc(IamApplicationEntity::getId));
    }

    /**
     * 读取单个应用。
     *
     * @param id 应用 ID
     * @return 应用行，不存在时为空
     */
    public IamApplicationEntity findApplication(long id) {
        return applications.selectById(BigInteger.valueOf(id));
    }

    /**
     * 判断应用是否存在。
     *
     * @param id 应用 ID
     * @return 存在时为 true
     */
    public boolean existsApplication(long id) {
        return applications.selectCount(Wrappers.<IamApplicationEntity>lambdaQuery()
                .eq(IamApplicationEntity::getId, BigInteger.valueOf(id))) == 1;
    }

    /**
     * 插入应用目录项，版本由数据库默认值承担。
     *
     * @param entity 待插入行
     */
    public void insertApplication(IamApplicationEntity entity) {
        applications.insert(entity);
    }

    /**
     * 锁定应用行，调用方须处于事务中。
     *
     * @param id 应用 ID
     * @return 锁定行，不存在时为空
     */
    public IamApplicationEntity lockApplication(long id) {
        return applications.lockRow(BigInteger.valueOf(id));
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
        applications.update(Wrappers.<IamApplicationEntity>lambdaUpdate()
                .eq(IamApplicationEntity::getId, BigInteger.valueOf(id))
                .set(IamApplicationEntity::getName, name)
                .set(IamApplicationEntity::getDescription, description)
                .set(IamApplicationEntity::getIcon, icon)
                .set(IamApplicationEntity::getSortOrder, sortOrder)
                .set(IamApplicationEntity::getBaseline, baseline)
                .set(IamApplicationEntity::getVersion, currentVersion.add(BigInteger.ONE)));
    }

    /**
     * 更新应用启用状态并将版本加一。
     *
     * @param id 应用 ID
     * @param enabled 是否启用
     * @param currentVersion 锁定后的当前版本
     */
    public void updateApplicationEnabled(long id, boolean enabled, BigInteger currentVersion) {
        applications.update(Wrappers.<IamApplicationEntity>lambdaUpdate()
                .eq(IamApplicationEntity::getId, BigInteger.valueOf(id))
                .set(IamApplicationEntity::getEnabled, enabled)
                .set(IamApplicationEntity::getVersion, currentVersion.add(BigInteger.ONE)));
    }

    /**
     * 删除应用。
     *
     * @param id 应用 ID
     */
    public void deleteApplication(long id) {
        applications.deleteById(BigInteger.valueOf(id));
    }

    /**
     * 统计应用下资源数。
     *
     * @param applicationId 应用 ID
     * @return 资源条数
     */
    public long countResources(long applicationId) {
        return resources.selectCount(Wrappers.<IamResourceEntity>lambdaQuery()
                .eq(IamResourceEntity::getApplicationId, BigInteger.valueOf(applicationId)));
    }

    /**
     * 统计引用该应用的开通数。
     *
     * @param applicationId 应用 ID
     * @return 开通条数
     */
    public long countEntitlements(long applicationId) {
        return entitlements.selectCount(Wrappers.<IamTenantAppEntitlementEntity>lambdaQuery()
                .eq(IamTenantAppEntitlementEntity::getApplicationId, BigInteger.valueOf(applicationId)));
    }

    /**
     * 统计套餐对该应用的引用。
     *
     * @param applicationId 应用 ID
     * @return 引用条数
     */
    public long countPlanApplications(long applicationId) {
        return planApplications.selectCount(Wrappers.<IamPlanApplicationEntity>lambdaQuery()
                .eq(IamPlanApplicationEntity::getApplicationId, BigInteger.valueOf(applicationId)));
    }

    /**
     * 统计应用下菜单数。
     *
     * @param applicationId 应用 ID
     * @return 菜单条数
     */
    public long countMenus(long applicationId) {
        return menus.selectCount(Wrappers.<IamMenuEntity>lambdaQuery()
                .eq(IamMenuEntity::getApplicationId, BigInteger.valueOf(applicationId)));
    }

    /**
     * 分页列出应用内资源。
     *
     * @param applicationId 应用 ID
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @return 资源页
     */
    public Page<IamResourceEntity> pageResources(long applicationId, int page, int pageSize) {
        return resources.selectPage(new Page<>(page, pageSize), Wrappers.<IamResourceEntity>lambdaQuery()
                .eq(IamResourceEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .orderByAsc(IamResourceEntity::getId));
    }

    /**
     * 读取指定应用下的资源。
     *
     * @param applicationId 应用 ID
     * @param id 资源 ID
     * @return 资源行，不存在时为空
     */
    public IamResourceEntity findResource(long applicationId, long id) {
        return resources.selectOne(Wrappers.<IamResourceEntity>lambdaQuery()
                .eq(IamResourceEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamResourceEntity::getId, BigInteger.valueOf(id)));
    }

    /**
     * 插入资源，版本由数据库默认值承担。
     *
     * @param entity 待插入行
     */
    public void insertResource(IamResourceEntity entity) {
        resources.insert(entity);
    }

    /**
     * 锁定资源行。
     *
     * @param applicationId 应用 ID
     * @param id 资源 ID
     * @return 锁定行，不存在时为空
     */
    public IamResourceEntity lockResource(long applicationId, long id) {
        return resources.lockRow(BigInteger.valueOf(applicationId), BigInteger.valueOf(id));
    }

    /**
     * 更新资源名称与能力并将版本加一。
     *
     * @param applicationId 应用 ID
     * @param id 资源 ID
     * @param name 名称
     * @param scopes JSON 数组
     * @param fields JSON 数组
     * @param currentVersion 锁定后的当前版本
     */
    public void updateResource(long applicationId, long id, String name, String scopes, String fields,
                               BigInteger currentVersion) {
        resources.update(Wrappers.<IamResourceEntity>lambdaUpdate()
                .eq(IamResourceEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamResourceEntity::getId, BigInteger.valueOf(id))
                .set(IamResourceEntity::getName, name)
                .set(IamResourceEntity::getScopeCapabilities, scopes)
                .set(IamResourceEntity::getFieldCapabilities, fields)
                .set(IamResourceEntity::getVersion, currentVersion.add(BigInteger.ONE)));
    }

    /**
     * 删除资源。
     *
     * @param applicationId 应用 ID
     * @param id 资源 ID
     */
    public void deleteResource(long applicationId, long id) {
        resources.delete(Wrappers.<IamResourceEntity>lambdaQuery()
                .eq(IamResourceEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamResourceEntity::getId, BigInteger.valueOf(id)));
    }

    /**
     * 统计资源下操作数。
     *
     * @param applicationId 应用 ID
     * @param resourceId 资源 ID
     * @return 操作条数
     */
    public long countActions(long applicationId, long resourceId) {
        return actions.selectCount(Wrappers.<IamActionEntity>lambdaQuery()
                .eq(IamActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamActionEntity::getResourceId, BigInteger.valueOf(resourceId)));
    }

    /**
     * 分页列出应用内操作。
     *
     * @param applicationId 应用 ID
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @return 操作页
     */
    public Page<IamActionEntity> pageActions(long applicationId, int page, int pageSize) {
        return actions.selectPage(new Page<>(page, pageSize), Wrappers.<IamActionEntity>lambdaQuery()
                .eq(IamActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .orderByAsc(IamActionEntity::getId));
    }

    /**
     * 读取指定应用下的操作。
     *
     * @param applicationId 应用 ID
     * @param id 操作 ID
     * @return 操作行，不存在时为空
     */
    public IamActionEntity findAction(long applicationId, long id) {
        return actions.selectOne(Wrappers.<IamActionEntity>lambdaQuery()
                .eq(IamActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamActionEntity::getId, BigInteger.valueOf(id)));
    }

    /**
     * 判断应用下操作是否存在。
     *
     * @param applicationId 应用 ID
     * @param id 操作 ID
     * @return 存在时为 true
     */
    public boolean existsAction(long applicationId, long id) {
        return actions.selectCount(Wrappers.<IamActionEntity>lambdaQuery()
                .eq(IamActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamActionEntity::getId, BigInteger.valueOf(id))) == 1;
    }

    /**
     * 插入操作，版本由数据库默认值承担。
     *
     * @param entity 待插入行
     */
    public void insertAction(IamActionEntity entity) {
        actions.insert(entity);
    }

    /**
     * 锁定操作行。
     *
     * @param applicationId 应用 ID
     * @param id 操作 ID
     * @return 锁定行，不存在时为空
     */
    public IamActionEntity lockAction(long applicationId, long id) {
        return actions.lockRow(BigInteger.valueOf(applicationId), BigInteger.valueOf(id));
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
        actions.update(Wrappers.<IamActionEntity>lambdaUpdate()
                .eq(IamActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamActionEntity::getId, BigInteger.valueOf(id))
                .set(IamActionEntity::getName, name)
                .set(IamActionEntity::getVersion, currentVersion.add(BigInteger.ONE)));
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
        actions.update(Wrappers.<IamActionEntity>lambdaUpdate()
                .eq(IamActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamActionEntity::getId, BigInteger.valueOf(id))
                .set(IamActionEntity::getEnabled, enabled)
                .set(IamActionEntity::getVersion, currentVersion.add(BigInteger.ONE)));
    }

    /**
     * 删除操作。
     *
     * @param applicationId 应用 ID
     * @param id 操作 ID
     */
    public void deleteAction(long applicationId, long id) {
        actions.delete(Wrappers.<IamActionEntity>lambdaQuery()
                .eq(IamActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamActionEntity::getId, BigInteger.valueOf(id)));
    }

    /**
     * 统计菜单对该操作的引用。
     *
     * @param applicationId 应用 ID
     * @param actionId 操作 ID
     * @return 引用条数
     */
    public long countMenuActions(long applicationId, long actionId) {
        return menuActions.selectCount(Wrappers.<IamMenuActionEntity>lambdaQuery()
                .eq(IamMenuActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamMenuActionEntity::getActionId, BigInteger.valueOf(actionId)));
    }

    /**
     * 统计角色授权对该操作的引用。
     *
     * @param actionId 操作 ID
     * @return 引用条数
     */
    public long countRoleGrants(long actionId) {
        return roleGrants.selectCount(Wrappers.<IamRoleGrantEntity>lambdaQuery()
                .eq(IamRoleGrantEntity::getActionId, BigInteger.valueOf(actionId)));
    }

    /**
     * 统计角色差异对该操作的引用。
     *
     * @param actionId 操作 ID
     * @return 引用条数
     */
    public long countRoleDeltas(long actionId) {
        return roleDeltas.selectCount(Wrappers.<IamRoleDeltaEntity>lambdaQuery()
                .eq(IamRoleDeltaEntity::getActionId, BigInteger.valueOf(actionId)));
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
        return menus.selectPage(new Page<>(page, pageSize), Wrappers.<IamMenuEntity>lambdaQuery()
                .eq(IamMenuEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .orderByAsc(IamMenuEntity::getSortOrder)
                .orderByAsc(IamMenuEntity::getId));
    }

    /**
     * 读取指定应用下的菜单。
     *
     * @param applicationId 应用 ID
     * @param id 菜单 ID
     * @return 菜单行，不存在时为空
     */
    public IamMenuEntity findMenu(long applicationId, long id) {
        return menus.selectOne(Wrappers.<IamMenuEntity>lambdaQuery()
                .eq(IamMenuEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamMenuEntity::getId, BigInteger.valueOf(id)));
    }

    /**
     * 判断应用下菜单是否存在。
     *
     * @param applicationId 应用 ID
     * @param id 菜单 ID
     * @return 存在时为 true
     */
    public boolean existsMenu(long applicationId, long id) {
        return menus.selectCount(Wrappers.<IamMenuEntity>lambdaQuery()
                .eq(IamMenuEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamMenuEntity::getId, BigInteger.valueOf(id))) == 1;
    }

    /**
     * 插入菜单，版本由数据库默认值承担。
     *
     * @param entity 待插入行
     */
    public void insertMenu(IamMenuEntity entity) {
        menus.insert(entity);
    }

    /**
     * 锁定菜单行。
     *
     * @param applicationId 应用 ID
     * @param id 菜单 ID
     * @return 锁定行，不存在时为空
     */
    public IamMenuEntity lockMenu(long applicationId, long id) {
        return menus.lockRow(BigInteger.valueOf(applicationId), BigInteger.valueOf(id));
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
     */
    public void updateMenu(long applicationId, long id, Long parentId, String name, String path, String viewPath,
                           String routeName, String icon, MenuKind kind, ActionMatchMode matchMode,
                           MenuAccessMode accessMode, int sortOrder, BigInteger currentVersion) {
        menus.update(Wrappers.<IamMenuEntity>lambdaUpdate()
                .eq(IamMenuEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamMenuEntity::getId, BigInteger.valueOf(id))
                .set(IamMenuEntity::getParentId, parentId == null ? null : BigInteger.valueOf(parentId))
                .set(IamMenuEntity::getName, name)
                .set(IamMenuEntity::getPath, path)
                .set(IamMenuEntity::getViewPath, viewPath)
                .set(IamMenuEntity::getRouteName, routeName)
                .set(IamMenuEntity::getIcon, icon)
                .set(IamMenuEntity::getKind, kind)
                .set(IamMenuEntity::getMatchMode, matchMode)
                .set(IamMenuEntity::getAccessMode, accessMode)
                .set(IamMenuEntity::getSortOrder, sortOrder)
                .set(IamMenuEntity::getVersion, currentVersion.add(BigInteger.ONE)));
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
        Long current = candidateParent;
        while (current != null) {
            if (current == menuId) {
                return true;
            }
            IamMenuEntity row = findMenu(applicationId, current);
            current = row == null || row.getParentId() == null ? null : row.getParentId().longValue();
        }
        return false;
    }

    /**
     * 整体替换菜单绑定的操作。
     *
     * @param applicationId 应用 ID
     * @param menuId 菜单 ID
     * @param actionIds 精确操作 ID，可空表示清空
     */
    public void replaceMenuActions(long applicationId, long menuId, List<Long> actionIds) {
        BigInteger application = BigInteger.valueOf(applicationId);
        BigInteger menu = BigInteger.valueOf(menuId);
        menuActions.delete(Wrappers.<IamMenuActionEntity>lambdaQuery()
                .eq(IamMenuActionEntity::getApplicationId, application)
                .eq(IamMenuActionEntity::getMenuId, menu));
        if (actionIds == null) {
            return;
        }
        for (Long actionId : actionIds) {
            IamMenuActionEntity link = new IamMenuActionEntity();
            link.setApplicationId(application);
            link.setMenuId(menu);
            link.setActionId(BigInteger.valueOf(actionId));
            menuActions.insert(link);
        }
    }

    /**
     * 读取菜单绑定的操作 ID，按操作 ID 排序。
     *
     * @param applicationId 应用 ID
     * @param menuId 菜单 ID
     * @return 操作 ID
     */
    public List<BigInteger> menuActionIds(long applicationId, long menuId) {
        return menuActions.selectList(Wrappers.<IamMenuActionEntity>lambdaQuery()
                        .eq(IamMenuActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                        .eq(IamMenuActionEntity::getMenuId, BigInteger.valueOf(menuId))
                        .orderByAsc(IamMenuActionEntity::getActionId))
                .stream().map(IamMenuActionEntity::getActionId).toList();
    }

    /**
     * 批量读取当前页菜单绑定的操作，避免逐行查询。
     *
     * @param applicationId 应用 ID
     * @param menuIds 菜单 ID
     * @return 菜单 ID 到按操作 ID 排序的绑定
     */
    public Map<BigInteger, List<BigInteger>> menuActionIds(long applicationId, List<BigInteger> menuIds) {
        Map<BigInteger, List<BigInteger>> result = new LinkedHashMap<>();
        if (menuIds == null || menuIds.isEmpty()) {
            return result;
        }
        List<IamMenuActionEntity> links = menuActions.selectList(Wrappers.<IamMenuActionEntity>lambdaQuery()
                .eq(IamMenuActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .in(IamMenuActionEntity::getMenuId, menuIds)
                .orderByAsc(IamMenuActionEntity::getMenuId)
                .orderByAsc(IamMenuActionEntity::getActionId));
        for (IamMenuActionEntity link : links) {
            result.computeIfAbsent(link.getMenuId(), key -> new ArrayList<>()).add(link.getActionId());
        }
        return result;
    }

    /**
     * 统计直接子菜单。
     *
     * @param applicationId 应用 ID
     * @param parentId 父菜单 ID
     * @return 子菜单条数
     */
    public long countChildMenus(long applicationId, long parentId) {
        return menus.selectCount(Wrappers.<IamMenuEntity>lambdaQuery()
                .eq(IamMenuEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamMenuEntity::getParentId, BigInteger.valueOf(parentId)));
    }

    /**
     * 删除菜单及其操作绑定。
     *
     * @param applicationId 应用 ID
     * @param id 菜单 ID
     */
    public void deleteMenu(long applicationId, long id) {
        BigInteger application = BigInteger.valueOf(applicationId);
        BigInteger menu = BigInteger.valueOf(id);
        menuActions.delete(Wrappers.<IamMenuActionEntity>lambdaQuery()
                .eq(IamMenuActionEntity::getApplicationId, application)
                .eq(IamMenuActionEntity::getMenuId, menu));
        menus.delete(Wrappers.<IamMenuEntity>lambdaQuery()
                .eq(IamMenuEntity::getApplicationId, application)
                .eq(IamMenuEntity::getId, menu));
    }

    /**
     * 分页列出套餐。
     *
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @return 套餐页
     */
    public Page<IamPlanEntity> pagePlans(int page, int pageSize) {
        return plans.selectPage(new Page<>(page, pageSize), Wrappers.<IamPlanEntity>lambdaQuery()
                .orderByAsc(IamPlanEntity::getId));
    }

    /**
     * 读取套餐。
     *
     * @param id 套餐 ID
     * @return 套餐行，不存在时为空
     */
    public IamPlanEntity findPlan(long id) {
        return plans.selectById(BigInteger.valueOf(id));
    }

    /**
     * 插入套餐，版本由数据库默认值承担。
     *
     * @param entity 待插入行
     */
    public void insertPlan(IamPlanEntity entity) {
        plans.insert(entity);
    }

    /**
     * 锁定套餐行。
     *
     * @param id 套餐 ID
     * @return 锁定行，不存在时为空
     */
    public IamPlanEntity lockPlan(long id) {
        return plans.lockRow(BigInteger.valueOf(id));
    }

    /**
     * 更新套餐名称与说明并将版本加一。
     *
     * @param id 套餐 ID
     * @param name 名称
     * @param description 说明，可空
     * @param currentVersion 锁定后的当前版本
     */
    public void updatePlan(long id, String name, String description, BigInteger currentVersion) {
        plans.update(Wrappers.<IamPlanEntity>lambdaUpdate()
                .eq(IamPlanEntity::getId, BigInteger.valueOf(id))
                .set(IamPlanEntity::getName, name)
                .set(IamPlanEntity::getDescription, description)
                .set(IamPlanEntity::getVersion, currentVersion.add(BigInteger.ONE)));
    }

    /**
     * 整体替换套餐应用清单。
     *
     * @param planId 套餐 ID
     * @param applicationIds 应用 ID
     */
    public void replacePlanApplications(long planId, List<Long> applicationIds) {
        BigInteger plan = BigInteger.valueOf(planId);
        planApplications.delete(Wrappers.<IamPlanApplicationEntity>lambdaQuery()
                .eq(IamPlanApplicationEntity::getPlanId, plan));
        if (applicationIds == null) {
            return;
        }
        for (Long applicationId : applicationIds) {
            IamPlanApplicationEntity link = new IamPlanApplicationEntity();
            link.setPlanId(plan);
            link.setApplicationId(BigInteger.valueOf(applicationId));
            planApplications.insert(link);
        }
    }

    /**
     * 读取套餐绑定的应用 ID，按应用 ID 排序。
     *
     * @param planId 套餐 ID
     * @return 应用 ID
     */
    public List<BigInteger> planApplicationIds(long planId) {
        return planApplications.selectList(Wrappers.<IamPlanApplicationEntity>lambdaQuery()
                        .eq(IamPlanApplicationEntity::getPlanId, BigInteger.valueOf(planId))
                        .orderByAsc(IamPlanApplicationEntity::getApplicationId))
                .stream().map(IamPlanApplicationEntity::getApplicationId).toList();
    }

    /**
     * 列出指定域启用的基础应用。
     *
     * @param domain 授权域
     * @return 基础应用 ID
     */
    public List<BigInteger> enabledBaselineIds(AuthorizationDomain domain) {
        return applications.selectList(Wrappers.<IamApplicationEntity>lambdaQuery()
                        .eq(IamApplicationEntity::getDomain, domain)
                        .eq(IamApplicationEntity::getEnabled, true)
                        .eq(IamApplicationEntity::getBaseline, true)
                        .select(IamApplicationEntity::getId))
                .stream().map(IamApplicationEntity::getId).toList();
    }
}
