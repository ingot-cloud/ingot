package com.ingot.cloud.pms.core.org;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysApplication;
import com.ingot.cloud.pms.api.model.domain.SysApplicationTenant;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.transform.AuthorityTrans;
import com.ingot.cloud.pms.api.model.transform.MenuTrans;
import com.ingot.cloud.pms.api.model.type.AuthorityType;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.core.MenuUtils;
import com.ingot.cloud.pms.service.domain.SysApplicationTenantService;
import com.ingot.cloud.pms.service.domain.SysAuthorityService;
import com.ingot.cloud.pms.service.domain.SysMenuService;
import com.ingot.cloud.pms.service.domain.SysRoleAuthorityService;
import com.ingot.framework.core.constants.IDConstants;
import com.ingot.framework.core.model.common.RelationDTO;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.core.utils.tree.TreeNode;
import com.ingot.framework.core.utils.tree.TreeUtils;
import com.ingot.framework.tenant.TenantEnv;
import com.ingot.framework.tenant.properties.TenantProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>Description  : TenantUtils.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/11/23.</p>
 * <p>Time         : 16:45.</p>
 */
@Slf4j
public class TenantUtils {


    public static List<MenuTreeNodeVO> filterMenus(List<MenuTreeNodeVO> allMenuNodeList, List<? extends AuthorityType> authorities) {
        List<MenuTreeNodeVO> nodeList = MenuUtils.filterMenus(allMenuNodeList, authorities);
        nodeList.stream()
                .filter(item -> authorities.stream()
                        .noneMatch(authority -> item.getAuthorityId().equals(authority.getId())))
                .forEach(item -> item.setAuthorityId(null));
        return nodeList;
    }

    /**
     * 根据当前菜单，遍历创建新菜单
     *
     * @param collect        收集
     * @param tree           节点列表
     * @param pid            父ID
     * @param menuTrans      对象转换类
     * @param sysMenuService menu service
     */
    public static void createMenuFn(List<SysMenu> collect,
                                    List<? extends TreeNode<Long>> tree,
                                    long pid,
                                    MenuTrans menuTrans,
                                    SysMenuService sysMenuService) {

        for (TreeNode<Long> node : tree) {
            if (node instanceof MenuTreeNodeVO menuNode) {
                SysMenu item = menuTrans.to(menuNode);
                item.setId(null);
                item.setPid(pid);
                item.setUpdatedAt(null);
                item.setDeletedAt(null);
                item.setCreatedAt(DateUtils.now());
                sysMenuService.save(item);
                if (collect != null) {
                    collect.add(item);
                }

                if (CollUtil.isNotEmpty(node.getChildren())) {
                    createMenuFn(collect, node.getChildren(), item.getId(), menuTrans, sysMenuService);
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
        item.setCreatedAt(DateUtils.now());
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
                                     MenuTrans menuTrans,
                                     SysMenuService service) {
        SysMenu item = menuTrans.copy(menu);
        item.setId(null);
        item.setPid(pid);
        item.setUpdatedAt(null);
        item.setDeletedAt(null);
        item.setCreatedAt(DateUtils.now());
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
                                                 MenuTrans menuTrans,
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

                menu = createMenu(pid, templateMenuList.get(i), menuTrans, service);
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
     * @param menuTrans 菜单转换
     * @return 菜单列表
     */
    public static List<MenuTreeNodeVO> getTargetMenus(long orgId,
                                                      long menuId,
                                                      SysMenuService service,
                                                      MenuTrans menuTrans) {
        return TenantEnv.applyAs(orgId, () -> {
            List<MenuTreeNodeVO> list = new ArrayList<>();

            SysMenu menu = service.getById(menuId);
            list.add(menuTrans.to(menu));

            List<SysMenu> children = service.list(Wrappers.<SysMenu>lambdaQuery()
                    .eq(SysMenu::getPid, menu.getId()));
            if (CollUtil.isNotEmpty(children)) {
                children.forEach(itemMenu -> list.addAll(getTargetMenus(orgId, itemMenu.getId(), service, menuTrans)));
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
                                                                 AuthorityTrans authorityTrans) {
        return TenantEnv.applyAs(orgId, () -> {
            List<AuthorityTreeNodeVO> list = new ArrayList<>();

            SysAuthority authority = service.getById(authorityId);
            list.add(authorityTrans.to(authority));

            List<SysAuthority> children = service.list(Wrappers.<SysAuthority>lambdaQuery()
                    .eq(SysAuthority::getPid, authority.getId()));
            if (CollUtil.isNotEmpty(children)) {
                children.forEach(itemMenu -> list.addAll(getTargetAuthorities(orgId, itemMenu.getId(), service, authorityTrans)));
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
            // 比如列表中有 a.b, a, b.c 那么只会绑定a和b.c
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
     * @param roleId      角色ID
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
                                             MenuTrans menuTrans,
                                             AuthorityTrans authorityTrans,
                                             SysAuthorityService sysAuthorityService,
                                             SysMenuService sysMenuService) {
        LoadAppInfo result = new LoadAppInfo();

        long rootMenu = application.getMenuId();
        long rootAuthority = application.getAuthorityId();
        List<MenuTreeNodeVO> menuList = TenantUtils.getTargetMenus(
                tenantProperties.getDefaultId(), rootMenu, sysMenuService, menuTrans);
        List<AuthorityTreeNodeVO> authorityList = TenantUtils.getTargetAuthorities(
                tenantProperties.getDefaultId(), rootAuthority, sysAuthorityService, authorityTrans);

        long rootAuthorityParentId = authorityList.get(0).getPid();
        long rootMenuParentId = menuList.get(0).getPid();
        List<AuthorityTreeNodeVO> authorityTree = TreeUtils.build(authorityList, rootAuthorityParentId);
        List<SysAuthority> authorityParentTemplateList = TenantUtils.getAuthorityParentList(
                tenantProperties.getDefaultId(), rootAuthorityParentId, sysAuthorityService);
        List<SysMenu> menuParentTemplateList = TenantUtils.getMenuParentList(
                tenantProperties.getDefaultId(), rootMenuParentId, sysMenuService);

        result.setMenuList(menuList);
        result.setAuthorityTree(authorityTree);
        result.setRootAuthorityParentId(rootAuthorityParentId);
        result.setRootMenuParentId(rootMenuParentId);
        result.setAuthorityParentTemplateList(authorityParentTemplateList);
        result.setMenuParentTemplateList(menuParentTemplateList);
        return result;
    }

    public static List<SysAuthority> createAppAndReturnAuthority(long orgId,
                                                                 SysApplication application,
                                                                 LoadAppInfo loadAppInfo,
                                                                 MenuTrans menuTrans,
                                                                 SysAuthorityService sysAuthorityService,
                                                                 SysMenuService sysMenuService,
                                                                 SysApplicationTenantService sysApplicationTenantService) {
        List<MenuTreeNodeVO> menuList = loadAppInfo.getMenuList();
        List<AuthorityTreeNodeVO> authorityTree = loadAppInfo.getAuthorityTree();
        long rootAuthorityParentId = loadAppInfo.getRootAuthorityParentId();
        long rootMenuParentId = loadAppInfo.getRootMenuParentId();
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
                orgId, menuParentTemplateList, menuTrans, sysMenuService);
        // 创建菜单
        List<MenuTreeNodeVO> menuTree = TreeUtils.build(menuList, rootMenuParentId);
        List<SysMenu> menuCollect = new ArrayList<>();
        TenantUtils.createMenuFn(menuCollect, menuTree, menuParentId, menuTrans, sysMenuService);

        SysApplicationTenant applicationTenant = new SysApplicationTenant();
        applicationTenant.setAppId(application.getId());
        applicationTenant.setMenuId(menuCollect.get(0).getId());
        applicationTenant.setAuthorityId(authorityCollect.get(0).getId());
        // 模版应用是默认应用，并且是可用的情况，才给应用设置可用
        applicationTenant.setStatus(BooleanUtil.isTrue(application.getDefaultApp())
                && application.getStatus() == CommonStatusEnum.ENABLE ? CommonStatusEnum.ENABLE : CommonStatusEnum.LOCK);
        applicationTenant.setCreatedAt(DateUtils.now());
        sysApplicationTenantService.save(applicationTenant);

        return authorityCollect;
    }
}
