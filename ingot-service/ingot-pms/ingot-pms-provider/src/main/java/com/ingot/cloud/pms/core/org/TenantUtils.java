package com.ingot.cloud.pms.core.org;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.convert.AuthorityConvert;
import com.ingot.cloud.pms.api.model.convert.DeptConvert;
import com.ingot.cloud.pms.api.model.convert.MenuConvert;
import com.ingot.cloud.pms.api.model.types.AuthorityType;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.commons.constants.IDConstants;
import com.ingot.framework.commons.model.common.RelationDTO;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.commons.utils.tree.TreeNode;
import com.ingot.framework.commons.utils.tree.TreeUtil;
import com.ingot.framework.tenant.TenantEnv;
import com.ingot.framework.tenant.properties.TenantProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

import java.util.*;

/**
 * <p>Description  : TenantUtils.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/11/23.</p>
 * <p>Time         : 16:45.</p>
 */
@Slf4j
public class TenantUtils {


    /**
     * 根据当前菜单，遍历创建新菜单
     *
     * @param collect        收集
     * @param tree           节点列表
     * @param pid            父ID
     * @param menuConvert      对象转换类
     * @param sysMenuService menu service
     */
    public static void createMenuFn(List<SysMenu> collect,
                                    List<? extends TreeNode<Long>> tree,
                                    long pid,
                                    MenuConvert menuConvert,
                                    SysMenuService sysMenuService) {

        for (TreeNode<Long> node : tree) {
            if (node instanceof MenuTreeNodeVO menuNode) {
                SysMenu item = menuConvert.to(menuNode);
                item.setId(null);
                item.setPid(pid);
                item.setUpdatedAt(null);
                item.setDeletedAt(null);
                item.setCreatedAt(DateUtil.now());
                sysMenuService.save(item);
                if (collect != null) {
                    collect.add(item);
                }

                if (CollUtil.isNotEmpty(node.getChildren())) {
                    createMenuFn(collect, node.getChildren(), item.getId(), menuConvert, sysMenuService);
                }
            }
        }
    }

    public static void createDeptFn(List<SysDept> collect,
                                    List<? extends TreeNode<Long>> tree,
                                    long pid,
                                    DeptConvert trans,
                                    SysDeptService service) {

        for (TreeNode<Long> node : tree) {
            if (node instanceof DeptTreeNodeVO menuNode) {
                SysDept item = trans.to(menuNode);
                item.setId(null);
                item.setPid(pid);
                item.setDeletedAt(null);
                item.setCreatedAt(DateUtil.now());
                item.setUpdatedAt(item.getCreatedAt());
                service.save(item);
                if (collect != null) {
                    collect.add(item);
                }

                if (CollUtil.isNotEmpty(node.getChildren())) {
                    createDeptFn(collect, node.getChildren(), item.getId(), trans, service);
                }
            }
        }
    }

    /**
     * 创建权限，并且收集创建的权限
     *
     * @param collect                 收集列表
     * @param templateTree            指定节点类比
     * @param pid                     父ID
     * @param replaceAuthorityIdMenus 需要替换成新创建权限ID的菜单列表
     * @param sysAuthorityService     权限服务
     */
    public static void createAuthorityAndCollectFn(List<SysAuthority> collect,
                                                   List<? extends TreeNode<Long>> templateTree,
                                                   long pid,
                                                   List<MenuTreeNodeVO> replaceAuthorityIdMenus,
                                                   SysAuthorityService sysAuthorityService) {

        for (TreeNode<Long> node : templateTree) {
            if (node instanceof AuthorityTreeNodeVO authNode) {
                SysAuthority item = createAuthority(pid, authNode, sysAuthorityService);
                if (collect != null) {
                    collect.add(item);
                }

                // 替换权限ID
                replaceAuthorityIdMenus.stream()
                        .filter(menu -> Objects.equals(menu.getAuthorityId(), node.getId()))
                        .forEach(menu -> menu.setAuthorityId(item.getId()));

                if (CollUtil.isNotEmpty(node.getChildren())) {
                    createAuthorityAndCollectFn(collect, node.getChildren(), item.getId(), replaceAuthorityIdMenus, sysAuthorityService);
                }
            }
        }
    }

    /**
     * 创建权限
     *
     * @param pid                 pid
     * @param authorityType       {@link AuthorityType}
     * @param sysAuthorityService service
     * @return {@link SysAuthority}
     */
    public static SysAuthority createAuthority(long pid,
                                               AuthorityType authorityType,
                                               SysAuthorityService sysAuthorityService) {
        SysAuthority item = new SysAuthority();
        item.setPid(pid);
        item.setName(authorityType.getName());
        item.setCode(authorityType.getCode());
        item.setType(authorityType.getType());
        item.setStatus(authorityType.getStatus());
        item.setRemark(authorityType.getRemark());
        item.setCreatedAt(DateUtil.now());
        sysAuthorityService.save(item);
        return item;
    }

    /**
     * 创建菜单
     *
     * @param pid     pid
     * @param menu    menu
     * @param service 服务
     * @return {@link SysMenu}
     */
    public static SysMenu createMenu(long pid,
                                     SysMenu menu,
                                     MenuConvert menuConvert,
                                     SysMenuService service) {
        SysMenu item = menuConvert.copy(menu);
        item.setId(null);
        item.setPid(pid);
        item.setUpdatedAt(null);
        item.setDeletedAt(null);
        item.setCreatedAt(DateUtil.now());
        service.save(item);
        return item;
    }

    /**
     * 获取指定权限父ID
     *
     * @param orgId            组织ID
     * @param authId           权限ID
     * @param authorityService service
     * @return pid
     */
    public static long getAuthorityParentId(long orgId,
                                            long authId,
                                            SysAuthorityService authorityService) {
        return TenantEnv.applyAs(orgId, () -> {
            SysAuthority authority = authorityService.getById(authId);
            SysAuthority parent = authorityService.getById(authority.getPid());
            return parent != null ? parent.getId() : IDConstants.ROOT_TREE_ID;
        });
    }

    /**
     * 确保父权限链存在，并且返回父权限ID
     *
     * @param orgId                 组织ID
     * @param templateAuthorityList 父权限模版，也就是默认租户的权限模版链
     * @param authorityService      service
     * @return pid
     */
    public static long ensureAuthorityTargetOrgParent(long orgId,
                                                      List<SysAuthority> templateAuthorityList,
                                                      SysAuthorityService authorityService) {
        return TenantEnv.applyAs(orgId, () -> {
            int len = CollUtil.size(templateAuthorityList);
            long pid = IDConstants.ROOT_TREE_ID;
            // 从大到小
            for (int i = len - 1; i >= 0; i--) {
                SysAuthority authority = authorityService.getOne(Wrappers.<SysAuthority>lambdaQuery()
                        .eq(SysAuthority::getCode, templateAuthorityList.get(i).getCode()));
                if (authority != null) {
                    if (i == 0) {
                        return authority.getId();
                    }
                    continue;
                }

                authority = createAuthority(pid, templateAuthorityList.get(i), authorityService);
                pid = authority.getId();
            }
            return pid;
        });
    }

    /**
     * 确保父菜单链存在，并且返回父权限ID
     *
     * @param orgId            组织ID
     * @param templateMenuList 父权限模版，也就是默认租户的权限模版链
     * @param service          service
     * @return pid
     */
    public static long ensureMenuTargetOrgParent(long orgId,
                                                 List<SysMenu> templateMenuList,
                                                 MenuConvert menuConvert,
                                                 SysMenuService service) {
        return TenantEnv.applyAs(orgId, () -> {
            int len = CollUtil.size(templateMenuList);
            long pid = IDConstants.ROOT_TREE_ID;
            // 从大到小
            for (int i = len - 1; i >= 0; i--) {
                SysMenu menu = service.getOne(Wrappers.<SysMenu>lambdaQuery()
                        .eq(SysMenu::getPath, templateMenuList.get(i).getPath()));
                if (menu != null) {
                    if (i == 0) {
                        return menu.getId();
                    }
                    continue;
                }

                menu = createMenu(pid, templateMenuList.get(i), menuConvert, service);
                pid = menu.getId();
            }
            return pid;
        });
    }

    /**
     * 获取指定父级权限及以上权限列表
     *
     * @param pid              权限ID
     * @param authorityService service
     * @return {@link SysAuthority}
     */
    public static List<SysAuthority> getAuthorityParentList(long orgId, long pid, SysAuthorityService authorityService) {
        return TenantEnv.applyAs(orgId, () -> {
            List<SysAuthority> authorities = new ArrayList<>();
            SysAuthority parent = authorityService.getById(pid);
            if (parent != null) {
                authorities.add(parent);
                if (parent.getPid() != null) {
                    authorities.addAll(getAuthorityParentList(orgId, parent.getPid(), authorityService));
                }
            }

            return authorities;
        });
    }

    /**
     * 获取指定父级权限及以上权限列表
     *
     * @param pid         权限ID
     * @param menuService service
     * @return {@link SysMenu}
     */
    public static List<SysMenu> getMenuParentList(long orgId, long pid, SysMenuService menuService) {
        return TenantEnv.applyAs(orgId, () -> {
            List<SysMenu> authorities = new ArrayList<>();
            SysMenu parent = menuService.getById(pid);
            if (parent != null) {
                authorities.add(parent);
                if (parent.getPid() != null) {
                    authorities.addAll(getMenuParentList(orgId, parent.getPid(), menuService));
                }
            }

            return authorities;
        });
    }

    /**
     * 获取指定组织指定菜单列表，根据所给菜单，查询该菜单以及其所有子菜单
     *
     * @param orgId     组织ID
     * @param menuId    菜单ID
     * @param service   菜单服务
     * @param menuConvert 菜单转换
     * @return 菜单列表
     */
    public static List<MenuTreeNodeVO> getTargetMenus(long orgId,
                                                      long menuId,
                                                      SysMenuService service,
                                                      MenuConvert menuConvert) {
        return TenantEnv.applyAs(orgId, () -> {
            List<MenuTreeNodeVO> list = new ArrayList<>();

            SysMenu menu = service.getById(menuId);
            list.add(menuConvert.to(menu));

            List<SysMenu> children = service.list(Wrappers.<SysMenu>lambdaQuery()
                    .eq(SysMenu::getPid, menu.getId()));
            if (CollUtil.isNotEmpty(children)) {
                children.forEach(itemMenu -> list.addAll(getTargetMenus(orgId, itemMenu.getId(), service, menuConvert)));
            }

            return list;
        });
    }

    /**
     * 获取指定组织的指定权限的所有子权限，包含指定权限
     *
     * @param orgId       组织ID
     * @param authorityId 权限ID
     * @param service     服务
     * @return 权限列表
     */
    public static List<AuthorityTreeNodeVO> getTargetAuthorities(long orgId,
                                                                 long authorityId,
                                                                 SysAuthorityService service,
                                                                 AuthorityConvert authorityConvert) {
        return TenantEnv.applyAs(orgId, () -> {
            List<AuthorityTreeNodeVO> list = new ArrayList<>();

            SysAuthority authority = service.getById(authorityId);
            list.add(authorityConvert.to(authority));

            List<SysAuthority> children = service.list(Wrappers.<SysAuthority>lambdaQuery()
                    .eq(SysAuthority::getPid, authority.getId()));
            if (CollUtil.isNotEmpty(children)) {
                children.forEach(itemMenu -> list.addAll(getTargetAuthorities(orgId, itemMenu.getId(), service, authorityConvert)));
            }

            return list;
        });
    }

    /**
     * 给指定角色绑定权限，绑定指定权限列表中的最高权限
     *
     * @param roleId      角色ID
     * @param authorities 权限列表
     * @param service     服务
     */
    public static void bindAuthorities(long orgId,
                                       long roleId,
                                       List<SysAuthority> authorities,
                                       SysRoleAuthorityService service) {
        TenantEnv.runAs(orgId, () -> {
            if (CollUtil.isEmpty(authorities)) {
                return;
            }

            // 减少绑定数据，只绑定列表中的最高权限
            // 比如列表中有 a:b, a, b:c 那么只会绑定a和b:c
            List<Long> bindIds = authorities.stream()
                    .filter(item -> authorities.stream().noneMatch(i -> Objects.equals(i.getId(), item.getPid())))
                    .map(SysAuthority::getId).toList();

            RelationDTO<Long, Long> params = new RelationDTO<>();
            params.setId(roleId);
            params.setBindIds(bindIds);
            service.roleBindAuthorities(params);
        });
    }

    /**
     * 给指定角色解绑权限
     *
     * @param orgId       组织ID
     * @param roleIds     角色ID
     * @param authorities 要删除的权限
     * @param service     服务
     */
    public static void unbindAuthorities(long orgId,
                                         List<Long> roleIds,
                                         List<? extends AuthorityType> authorities,
                                         SysRoleAuthorityService service) {
        TenantEnv.runAs(orgId, () -> service.clearRoleWithAuthorities(roleIds, authorities.stream().map(AuthorityType::getId).toList()));
    }

    /**
     * 获取构建组织应用的相关数据
     *
     * @return {@link LoadAppInfo}
     */
    public static LoadAppInfo getLoadAppInfo(SysApplication application,
                                             TenantProperties tenantProperties,
                                             MenuConvert menuConvert,
                                             AuthorityConvert authorityConvert,
                                             SysAuthorityService sysAuthorityService,
                                             SysMenuService sysMenuService) {
        LoadAppInfo result = new LoadAppInfo();

        long rootMenu = application.getMenuId();
        long rootAuthority = application.getAuthorityId();
        List<MenuTreeNodeVO> menuList = TenantUtils.getTargetMenus(
                tenantProperties.getDefaultId(), rootMenu, sysMenuService, menuConvert);
        List<AuthorityTreeNodeVO> authorityList = TenantUtils.getTargetAuthorities(
                tenantProperties.getDefaultId(), rootAuthority, sysAuthorityService, authorityConvert);

        long rootAuthorityParentId = authorityList.get(0).getPid();
        long rootMenuParentId = menuList.get(0).getPid();
        List<MenuTreeNodeVO> menuTree = TreeUtil.build(menuList, rootMenuParentId);
        List<AuthorityTreeNodeVO> authorityTree = TreeUtil.build(authorityList, rootAuthorityParentId);
        List<SysAuthority> authorityParentTemplateList = TenantUtils.getAuthorityParentList(
                tenantProperties.getDefaultId(), rootAuthorityParentId, sysAuthorityService);
        List<SysMenu> menuParentTemplateList = TenantUtils.getMenuParentList(
                tenantProperties.getDefaultId(), rootMenuParentId, sysMenuService);

        result.setMenuList(menuList);
        result.setMenuTree(menuTree);
        result.setAuthorityList(authorityList);
        result.setAuthorityTree(authorityTree);
        result.setAuthorityParentTemplateList(authorityParentTemplateList);
        result.setMenuParentTemplateList(menuParentTemplateList);
        return result;
    }

    public static List<SysAuthority> createAppAndReturnAuthority(long orgId,
                                                                 SysApplication application,
                                                                 LoadAppInfo loadAppInfo,
                                                                 MenuConvert menuConvert,
                                                                 SysAuthorityService sysAuthorityService,
                                                                 SysMenuService sysMenuService,
                                                                 SysApplicationTenantService sysApplicationTenantService) {
        List<MenuTreeNodeVO> menuList = loadAppInfo.getMenuList();
        List<MenuTreeNodeVO> menuTree = loadAppInfo.getMenuTree();
        List<AuthorityTreeNodeVO> authorityTree = loadAppInfo.getAuthorityTree();
        List<SysAuthority> authorityParentTemplateList = loadAppInfo.getAuthorityParentTemplateList();
        List<SysMenu> menuParentTemplateList = loadAppInfo.getMenuParentTemplateList();

        // 获取当前应用的父权限ID
        long authorityParentId = TenantUtils.ensureAuthorityTargetOrgParent(
                orgId, authorityParentTemplateList, sysAuthorityService);
        List<SysAuthority> authorityCollect = new ArrayList<>();
        // 创建权限，并且替换待创建菜单中对应的权限ID
        TenantUtils.createAuthorityAndCollectFn(authorityCollect, authorityTree,
                authorityParentId, menuList, sysAuthorityService);

        // 获取当前应用的父菜单ID
        long menuParentId = TenantUtils.ensureMenuTargetOrgParent(
                orgId, menuParentTemplateList, menuConvert, sysMenuService);
        // 创建菜单
        List<SysMenu> menuCollect = new ArrayList<>();
        TenantUtils.createMenuFn(menuCollect, menuTree, menuParentId, menuConvert, sysMenuService);

        SysApplicationTenant applicationTenant = new SysApplicationTenant();
        applicationTenant.setAppId(application.getId());
        applicationTenant.setMenuId(menuCollect.get(0).getId());
        applicationTenant.setAuthorityId(authorityCollect.get(0).getId());
        // 模版应用是默认应用，并且是可用的情况，才给应用设置可用
        applicationTenant.setStatus(BooleanUtil.isTrue(application.getDefaultApp())
                && application.getStatus() == CommonStatusEnum.ENABLE ? CommonStatusEnum.ENABLE : CommonStatusEnum.LOCK);
        applicationTenant.setCreatedAt(DateUtil.now());
        sysApplicationTenantService.save(applicationTenant);

        return authorityCollect;
    }

    /**
     * 同步模版权限，对于diff进行删除和创建，并且返回模版ID和当前ID映射表
     *
     * @param orgId               组织ID
     * @param rootAuthorityId     当前跟权限ID
     * @param loadAppInfo         加载的模版信息
     * @param sysAuthorityService {@link SysAuthorityService}
     * @param authorityConvert      {@link AuthorityConvert}
     */
    public static Map<Long, Long> syncTemplateAuthorityAndReturnMap(long orgId,
                                                                    long rootAuthorityId,
                                                                    LoadAppInfo loadAppInfo,
                                                                    SysAuthorityService sysAuthorityService,
                                                                    AuthorityConvert authorityConvert,
                                                                    SysRoleAuthorityService sysRoleAuthorityService) {
        List<AuthorityTreeNodeVO> currentAuthorities = TenantUtils.getTargetAuthorities(
                orgId, rootAuthorityId, sysAuthorityService, authorityConvert);
        List<AuthorityTreeNodeVO> templateAuthorities = loadAppInfo.getAuthorityList();
        List<AuthorityTreeNodeVO> templateAuthorityTree = loadAppInfo.getAuthorityTree();

        Map<Long, Long> collectIdMap = new HashMap<>();
        authoritySyncDiffAndCollectIdMap(collectIdMap, templateAuthorityTree, currentAuthorities, rootAuthorityId, sysAuthorityService);

        // 待删除列表
        List<AuthorityTreeNodeVO> removeList = currentAuthorities.stream()
                .filter(menu -> templateAuthorities.stream()
                        .noneMatch(templateMenu -> StrUtil.equals(templateMenu.getCode(), menu.getCode())))
                .toList();
        if (CollUtil.isNotEmpty(removeList)) {
            // 删除权限
            List<Long> removeIds = removeList.stream().map(TreeNode::getId).toList();
            sysAuthorityService.remove(Wrappers.<SysAuthority>lambdaQuery()
                    .in(SysAuthority::getId, removeIds));

            // 删除关联权限
            sysRoleAuthorityService.remove(Wrappers.<SysRoleAuthority>lambdaQuery()
                    .in(SysRoleAuthority::getAuthorityId, removeIds));
        }

        return collectIdMap;
    }

    public static void syncTemplateManu(long orgId,
                                        long rootMenuId,
                                        LoadAppInfo loadAppInfo,
                                        Map<Long, Long> collectAuthorityIdMap,
                                        SysMenuService sysMenuService,
                                        MenuConvert menuConvert) {
        List<MenuTreeNodeVO> currentMenus = TenantUtils.getTargetMenus(orgId, rootMenuId, sysMenuService, menuConvert);
        List<MenuTreeNodeVO> templateMenuTree = loadAppInfo.getMenuTree();

        // 创建/更新菜单
        menuCreateDiff(collectAuthorityIdMap, templateMenuTree, currentMenus, rootMenuId, sysMenuService, menuConvert);

        // 删除菜单，如果当前组织菜单已经被删除，那么需要将组织中相应菜单删除
        List<MenuTreeNodeVO> templateMenuList = TreeUtil.stretch(templateMenuTree);
        List<Long> removeMenuIdList = currentMenus
                .stream()
                .filter(item -> templateMenuList.stream()
                        .noneMatch(templateMenu -> StrUtil.equals(templateMenu.getPath(), item.getPath())))
                .map(TreeNode::getId)
                .toList();
        if (CollUtil.isEmpty(removeMenuIdList)) {
            return;
        }
        sysMenuService.remove(Wrappers.<SysMenu>lambdaQuery().in(SysMenu::getId, removeMenuIdList));
    }

    /**
     * 比对当前权限和模版权限，同步当前缺失的权限，并且收集模版权限和当前权限的ID映射表
     *
     * @param collectIdMap        ID映射表
     * @param templateTree        模版tree
     * @param currentList         当前权限列表
     * @param pid                 当前权限PID
     * @param sysAuthorityService {@link SysAuthorityService}
     */
    public static void authoritySyncDiffAndCollectIdMap(@NonNull Map<Long, Long> collectIdMap,
                                                        List<? extends TreeNode<Long>> templateTree,
                                                        List<AuthorityTreeNodeVO> currentList,
                                                        long pid,
                                                        SysAuthorityService sysAuthorityService) {
        for (TreeNode<Long> node : templateTree) {
            if (node instanceof AuthorityTreeNodeVO authNode) {
                // 如果当前没有这个权限，则创建
                if (currentList.stream().noneMatch(currentAuthority -> StrUtil.equals(currentAuthority.getCode(), authNode.getCode()))) {
                    // 创建
                    SysAuthority item = createAuthority(pid, authNode, sysAuthorityService);
                    collectIdMap.put(authNode.getId(), item.getId());
                } else {
                    currentList.stream()
                            .filter(currentAuthority -> StrUtil.equals(currentAuthority.getCode(), authNode.getCode()))
                            .findFirst()
                            .ifPresent(current -> {
                                // 更新映射
                                collectIdMap.put(authNode.getId(), current.getId());

                                // 更新当前实体相关信息
                                SysAuthority item = new SysAuthority();
                                item.setId(current.getId());
                                item.setName(authNode.getName());
                                item.setStatus(authNode.getStatus());
                                item.setRemark(authNode.getRemark());
                                item.setUpdatedAt(DateUtil.now());
                                sysAuthorityService.updateById(item);
                            });
                }

                if (CollUtil.isNotEmpty(authNode.getChildren())) {
                    authoritySyncDiffAndCollectIdMap(collectIdMap, authNode.getChildren(), currentList,
                            collectIdMap.get(authNode.getId()), sysAuthorityService);
                }
            }
        }
    }

    public static void menuCreateDiff(@NonNull Map<Long, Long> collectAuthorityIdMap,
                                      List<? extends TreeNode<Long>> templateTree,
                                      List<MenuTreeNodeVO> currentList,
                                      long pid,
                                      SysMenuService sysMenuService,
                                      MenuConvert menuConvert) {
        for (TreeNode<Long> node : templateTree) {
            if (node instanceof MenuTreeNodeVO menu) {
                long nextPid = 0;
                // 如果当前没有这个菜单，则创建
                if (currentList.stream().noneMatch(menuItem -> StrUtil.equals(menuItem.getPath(), menu.getPath()))) {
                    // 创建
                    SysMenu item = menuConvert.to(menu);
                    item.setId(null);
                    item.setPid(pid);
                    // 替换权限ID
                    if (menu.getAuthorityId() != null) {
                        item.setAuthorityId(collectAuthorityIdMap.get(menu.getAuthorityId()));
                    }
                    item.setUpdatedAt(null);
                    item.setDeletedAt(null);
                    item.setCreatedAt(DateUtil.now());
                    sysMenuService.save(item);
                    nextPid = item.getId();
                } else {
                    MenuTreeNodeVO currentMenu = currentList.stream()
                            .filter(menuItem -> StrUtil.equals(menuItem.getPath(), menu.getPath()))
                            .findFirst().orElse(null);
                    if (currentMenu != null) {
                        nextPid = currentMenu.getId();

                        // 更新当前实体相关信息
                        SysMenu sysMenu = menuConvert.to(menu);
                        sysMenu.setId(currentMenu.getId());
                        sysMenu.setPid(null);
                        sysMenu.setAuthorityId(null);
                        // 如果当前模版菜单权限不为空，那么需要判断权限是否一致，不一致则需要更新
                        if (menu.getAuthorityId() != null) {
                            Optional.ofNullable(collectAuthorityIdMap.get(menu.getAuthorityId()))
                                    .ifPresent(authorityId -> {
                                        if (!Objects.equals(currentMenu.getAuthorityId(), authorityId)) {
                                            sysMenu.setAuthorityId(authorityId);
                                        }
                                    });
                        }
                        sysMenu.setUpdatedAt(DateUtil.now());
                        sysMenuService.updateById(sysMenu);
                    }
                }

                if (CollUtil.isNotEmpty(menu.getChildren())) {
                    menuCreateDiff(collectAuthorityIdMap, menu.getChildren(), currentList,
                            nextPid, sysMenuService, menuConvert);
                }
            }
        }
    }

}
