package com.ingot.cloud.pms.core;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.transform.MenuTrans;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.service.domain.SysAuthorityService;
import com.ingot.cloud.pms.service.domain.SysMenuService;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.core.utils.tree.TreeNode;
import com.ingot.framework.tenant.TenantEnv;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>Description  : TenantUtils.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/11/23.</p>
 * <p>Time         : 16:45.</p>
 */
public class TenantUtils {


    public static List<MenuTreeNodeVO> filterMenus(List<MenuTreeNodeVO> allMenuNodeList, List<SysAuthority> authorities) {
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
     * @param tree           节点列表
     * @param pid            父ID
     * @param menuTrans      对象转换类
     * @param sysMenuService menu service
     */
    public static void createMenuFn(List<? extends TreeNode<Long>> tree, long pid,
                                    MenuTrans menuTrans, SysMenuService sysMenuService) {

        for (TreeNode<Long> node : tree) {
            if (node instanceof MenuTreeNodeVO menuNode) {
                SysMenu item = menuTrans.to(menuNode);
                item.setId(null);
                item.setPid(pid);
                item.setUpdatedAt(null);
                item.setDeletedAt(null);
                sysMenuService.save(item);

                if (CollUtil.isNotEmpty(node.getChildren())) {
                    createMenuFn(node.getChildren(), item.getId(), menuTrans, sysMenuService);
                }
            }
        }
    }

    /**
     * 创建权限，并且收集创建的权限
     *
     * @param collect                 收集列表
     * @param tree                    指定节点类比
     * @param pid                     父ID
     * @param replaceAuthorityIdMenus 需要替换成新创建权限ID的菜单列表
     * @param sysAuthorityService     权限服务
     */
    public static void createAuthorityAndCollectFn(List<SysAuthority> collect,
                                                   List<? extends TreeNode<Long>> tree,
                                                   long pid,
                                                   List<MenuTreeNodeVO> replaceAuthorityIdMenus,
                                                   SysAuthorityService sysAuthorityService) {

        for (TreeNode<Long> node : tree) {
            if (node instanceof AuthorityTreeNodeVO authNode) {
                SysAuthority item = new SysAuthority();
                item.setPid(pid);
                item.setName(authNode.getName());
                item.setCode(authNode.getCode());
                item.setType(authNode.getType());
                item.setStatus(authNode.getStatus());
                item.setRemark(authNode.getRemark());
                item.setCreatedAt(DateUtils.now());
                sysAuthorityService.save(item);
                collect.add(item);

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
     * 获取指定组织指定菜单列表，根据所给菜单，查询该菜单以及其所有子菜单
     *
     * @param orgId   组织ID
     * @param menuId  菜单ID
     * @param service 菜单服务
     * @return 菜单列表
     */
    public static List<SysMenu> getTargetMenus(long orgId, long menuId, SysMenuService service) {
        return TenantEnv.applyAs(orgId, () -> {
            List<SysMenu> list = new ArrayList<>();

            SysMenu menu = service.getById(menuId);
            list.add(menu);

            List<SysMenu> children = service.list(Wrappers.<SysMenu>lambdaQuery()
                    .in(SysMenu::getPid, menu.getId()));
            if (CollUtil.isNotEmpty(children)) {
                children.forEach(itemMenu -> list.addAll(getTargetMenus(orgId, itemMenu.getId(), service)));
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
    public static List<SysAuthority> getTargetAuthorities(long orgId, long authorityId, SysAuthorityService service) {
        return TenantEnv.applyAs(orgId, () -> {
            List<SysAuthority> list = new ArrayList<>();

            SysAuthority authority = service.getById(authorityId);
            list.add(authority);

            List<SysAuthority> children = service.list(Wrappers.<SysAuthority>lambdaQuery()
                    .in(SysAuthority::getPid, authority.getId()));
            if (CollUtil.isNotEmpty(children)) {
                children.forEach(itemMenu -> list.addAll(getTargetAuthorities(orgId, itemMenu.getId(), service)));
            }

            return list;
        });
    }
}
