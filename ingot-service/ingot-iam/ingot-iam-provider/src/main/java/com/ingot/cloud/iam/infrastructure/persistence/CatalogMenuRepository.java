package com.ingot.cloud.iam.infrastructure.persistence;

import java.util.stream.Collectors;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamMenuActionEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamMenuEntity;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamMenuActionMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamMenuMapper;
import com.ingot.cloud.iam.domain.catalog.MenuAccessMode;
import com.ingot.cloud.iam.domain.catalog.ActionMatchMode;
import com.ingot.cloud.iam.domain.catalog.MenuKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


/**
 * <p>维护菜单树和操作绑定，保留顺序、祖先检查与关系差量写入。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class CatalogMenuRepository {
    private final IamMenuMapper menus;
    private final IamMenuActionMapper menuActions;


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
     * 列出应用全部菜单，按排序和 ID 稳定排列。
     *
     * @param applicationId 应用 ID
     * @return 菜单行
     */
    public List<IamMenuEntity> listMenus(long applicationId) {
        return menus.selectList(Wrappers.<IamMenuEntity>lambdaQuery()
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
     * @param hidden 是否隐藏导航
     * @param isCache 是否缓存页面
     * @param props 是否透传路径参数
     * @param routeParams 有序参数 JSON
     */
    public void updateMenu(long applicationId, long id, Long parentId, String name, String path, String viewPath,
                           String routeName, String icon, MenuKind kind, ActionMatchMode matchMode,
                           MenuAccessMode accessMode, int sortOrder, BigInteger currentVersion,
                           boolean hidden, boolean isCache, boolean props, String routeParams) {
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
                .set(IamMenuEntity::getHidden, hidden)
                .set(IamMenuEntity::getIsCache, isCache)
                .set(IamMenuEntity::getProps, props)
                .set(IamMenuEntity::getRouteParams, routeParams)
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
        var after = CatalogRelationIds.uniqueRelationIds(actionIds);
        var before = menuActionIds(applicationId, menuId);
        BigInteger application = BigInteger.valueOf(applicationId);
        BigInteger menu = BigInteger.valueOf(menuId);
        before.stream().filter(id -> !after.contains(id.longValueExact())).forEach(id ->
                menuActions.delete(Wrappers.<IamMenuActionEntity>lambdaQuery()
                        .eq(IamMenuActionEntity::getApplicationId, application)
                        .eq(IamMenuActionEntity::getMenuId, menu)
                        .eq(IamMenuActionEntity::getActionId, id)));
        var oldIds = before.stream().map(BigInteger::longValueExact).collect(Collectors.toSet());
        after.stream().filter(id -> !oldIds.contains(id)).forEach(id -> {
            var row = new IamMenuActionEntity();
            row.setApplicationId(application);
            row.setMenuId(menu);
            row.setActionId(BigInteger.valueOf(id));
            menuActions.insert(row);
        });
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
}
