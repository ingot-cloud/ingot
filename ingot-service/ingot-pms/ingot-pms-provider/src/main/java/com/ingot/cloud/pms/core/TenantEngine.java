package com.ingot.cloud.pms.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.dto.org.CreateOrgDTO;
import com.ingot.cloud.pms.api.model.enums.AuthorityTypeEnums;
import com.ingot.cloud.pms.api.model.enums.DeptRoleScopeEnum;
import com.ingot.cloud.pms.api.model.enums.RoleTypeEnums;
import com.ingot.cloud.pms.api.model.transform.AuthorityTrans;
import com.ingot.cloud.pms.api.model.transform.MenuTrans;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.service.biz.BizDeptService;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.core.constants.IDConstants;
import com.ingot.framework.core.model.common.RelationDTO;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.core.utils.tree.TreeNode;
import com.ingot.framework.core.utils.tree.TreeUtils;
import com.ingot.framework.security.common.constants.RoleConstants;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * <p>Description  : TenantEngine.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/8/1.</p>
 * <p>Time         : 4:25 PM.</p>
 */
@Component
@RequiredArgsConstructor
public class TenantEngine {
    private final SysTenantService sysTenantService;
    private final SysDeptService sysDeptService;
    private final SysRoleService sysRoleService;
    private final SysRoleGroupService sysRoleGroupService;
    private final SysAuthorityService sysAuthorityService;
    private final SysUserService sysUserService;
    private final SysUserTenantService sysUserTenantService;
    private final BizDeptService bizDeptService;
    private final SysUserDeptService sysUserDeptService;
    private final SysRoleUserService sysRoleUserService;
    private final SysRoleAuthorityService sysRoleAuthorityService;
    private final SysMenuService sysMenuService;
    private final BizIdGen bizIdGen;
    private final AuthorityTrans authorityTrans;
    private final MenuTrans menuTrans;

    /**
     * 创建租户
     */
    public SysTenant createTenant(CreateOrgDTO params) {
        String orgCode = bizIdGen.genOrgCode();
        SysTenant tenant = new SysTenant();
        tenant.setName(params.getName());
        tenant.setAvatar(params.getAvatar());
        tenant.setCode(orgCode);
        sysTenantService.createTenant(tenant);
        return tenant;
    }

    /**
     * 创建租户部门
     */
    public SysDept createTenantDept(SysTenant tenant) {
        return TenantEnv.applyAs(tenant.getId(), () -> {
            SysDept dept = new SysDept();
            dept.setName(tenant.getName());
            dept.setScope(DeptRoleScopeEnum.CURRENT_CHILD);
            dept.setMainFlag(Boolean.TRUE);
            sysDeptService.createDept(dept);
            return dept;
        });
    }

    /**
     * 创建租户角色
     */
    public List<SysRole> createTenantRoles(SysTenant tenant) {
        List<SysRole> templateRoles = sysRoleService.list(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getType, RoleTypeEnums.Tenant));
        List<SysRoleGroup> templateRoleGroups = sysRoleGroupService.list(Wrappers.<SysRoleGroup>lambdaQuery()
                .eq(SysRoleGroup::getType, RoleTypeEnums.Tenant));

        return TenantEnv.applyAs(tenant.getId(), () -> {
            List<Long> templateGroupIds = templateRoleGroups.stream().map(SysRoleGroup::getId).toList();
            List<SysRoleGroup> orgRoleGroups = templateRoleGroups.stream()
                    .map(item -> {
                        SysRoleGroup group = new SysRoleGroup();
                        group.setName(item.getName());
                        group.setType(item.getType());
                        group.setSort(item.getSort());
                        return group;
                    }).toList();
            sysRoleGroupService.saveBatch(orgRoleGroups);

            List<SysRole> orgRoles = templateRoles.stream()
                    .map(item -> {
                        SysRole role = new SysRole();
                        role.setGroupId(orgRoleGroups.get(templateGroupIds.indexOf(item.getGroupId())).getId());
                        role.setName(item.getName());
                        role.setCode(item.getCode());
                        role.setType(item.getType());
                        role.setStatus(item.getStatus());
                        role.setCreatedAt(DateUtils.now());
                        return role;
                    }).toList();
            sysRoleService.saveBatch(orgRoles);
            return orgRoles;
        });
    }

    /**
     * 创建组合权限和菜单
     */
    public List<SysAuthority> createTenantAuthorityAndMenu(SysTenant tenant) {
        List<SysAuthority> authorities = sysAuthorityService.list(Wrappers.<SysAuthority>lambdaQuery()
                .eq(SysAuthority::getType, AuthorityTypeEnums.Tenant));
        List<AuthorityTreeNodeVO> templateAuthorities = authorities.stream()
                .map(authorityTrans::to).toList();
        List<AuthorityTreeNodeVO> tree = TreeUtils.build(templateAuthorities);

        // 获取菜单
        List<MenuTreeNodeVO> templateMenus = filterMenus(sysMenuService.nodeList(), authorities);

        return TenantEnv.applyAs(tenant.getId(), () -> {
            List<SysAuthority> orgAuthorities = new ArrayList<>();
            createAuthorityFn(orgAuthorities, tree, IDConstants.ROOT_TREE_ID, templateMenus);

            // 创建菜单
            List<MenuTreeNodeVO> menuTree = TreeUtils.build(templateMenus);
            createMenuFn(menuTree, IDConstants.ROOT_TREE_ID);

            return orgAuthorities;
        });
    }

    private List<MenuTreeNodeVO> filterMenus(List<MenuTreeNodeVO> allMenuNodeList, List<SysAuthority> authorities) {
        List<MenuTreeNodeVO> nodeList = allMenuNodeList.stream()
                .filter(node -> node.getAuthorityId() == null || node.getAuthorityId() == 0 ||
                        authorities.stream()
                                .anyMatch(authority -> node.getAuthorityId().equals(authority.getId())))
                .filter(node -> node.getStatus() == CommonStatusEnum.ENABLE)
                .sorted(Comparator.comparingInt(MenuTreeNodeVO::getSort))
                .toList();

        // 如果过滤后的列表中存在父节点，并且不在当前列表中，那么需要增加
        List<MenuTreeNodeVO> copy = new ArrayList<>(nodeList);
        copy.stream()
                .filter(node -> node.getPid() != IDConstants.ROOT_TREE_ID)
                .forEach(node -> {
                    if (nodeList.stream().noneMatch(item -> ObjectUtil.equals(item.getId(), node.getPid()))) {
                        allMenuNodeList.stream()
                                .filter(item -> ObjectUtil.equals(item.getId(), node.getPid()))
                                .findFirst()
                                .ifPresent(nodeList::add);
                    }
                });

        nodeList.stream()
                .filter(item -> authorities.stream()
                        .noneMatch(authority -> item.getAuthorityId().equals(authority.getId())))
                .forEach(item -> item.setAuthorityId(null));

        return nodeList;
    }

    private void createMenuFn(List<? extends TreeNode<Long>> tree, long pid) {

        for (TreeNode<Long> node : tree) {
            if (node instanceof MenuTreeNodeVO menuNode) {
                SysMenu item = menuTrans.to(menuNode);
                item.setId(null);
                item.setPid(pid);
                item.setUpdatedAt(null);
                item.setDeletedAt(null);
                sysMenuService.save(item);

                if (CollUtil.isNotEmpty(node.getChildren())) {
                    createMenuFn(node.getChildren(), item.getId());
                }
            }
        }
    }

    private void createAuthorityFn(List<SysAuthority> collect,
                                   List<? extends TreeNode<Long>> tree,
                                   long pid,
                                   List<MenuTreeNodeVO> replaceAuthorityIdMenus) {

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
                    createAuthorityFn(collect, node.getChildren(), item.getId(), replaceAuthorityIdMenus);
                }
            }
        }
    }

    /**
     * 创建租户管理员
     */
    public void createTenantUser(CreateOrgDTO params, SysTenant tenant, List<SysRole> roles, SysDept dept) {
        TenantEnv.runAs(tenant.getId(), () -> {
            SysRole role = roles.stream()
                    .filter(item -> StrUtil.equals(item.getCode(), RoleConstants.ROLE_MANAGER_CODE))
                    .findFirst().orElseThrow();

            // 如果已经存在注册用户，那么直接关联新组织信息
            SysUser user = sysUserService.getOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getPhone, params.getPhone()));
            if (user == null) {
                user = new SysUser();
                user.setInitPwd(Boolean.TRUE);
                user.setUsername(params.getPhone());
                user.setPhone(params.getPhone());
                user.setPassword(params.getPhone());
                user.setNickname(params.getNickname());
                sysUserService.createUser(user);
            }

            // 加入租户
            sysUserTenantService.joinTenant(user.getId());
            // 设置部门
            sysDeptService.setDepts(user.getId(), List.of(dept.getId()));
            // 设置主角色
            sysRoleUserService.setUserRoles(user.getId(), List.of(role.getId()));
        });
    }

    /**
     * 租户默认角色关联权限
     */
    public void tenantRoleBindAuthorities(List<SysRole> roles, List<SysAuthority> authorities) {
        // 默认直给管理员角色绑定组织最高权限
        SysRole role = roles.stream()
                .filter(item -> StrUtil.equals(item.getCode(), RoleConstants.ROLE_MANAGER_CODE))
                .findFirst().orElseThrow();

        List<Long> bindIds = authorities.stream()
                .filter(item -> item.getPid() == IDConstants.ROOT_TREE_ID)
                .map(SysAuthority::getId).toList();

        RelationDTO<Long, Long> params = new RelationDTO<>();
        params.setId(role.getId());
        params.setBindIds(bindIds);
        sysRoleAuthorityService.roleBindAuthorities(params);
    }

    /**
     * 移除用户关联该组织，以及组织下的部门和角色
     *
     * @param id 组织ID
     */
    public void removeTenantUserRelation(long id) {
        TenantEnv.runAs(id, () -> {
            List<Long> userIdList = sysUserTenantService.list(
                            Wrappers.<SysUserTenant>lambdaQuery()
                                    .eq(SysUserTenant::getTenantId, id))
                    .stream().map(SysUserTenant::getUserId).toList();

            // 取消关联组织
            sysUserTenantService.remove(Wrappers.<SysUserTenant>lambdaQuery()
                    .eq(SysUserTenant::getTenantId, id));

            // 取消关联部门
            sysUserDeptService.remove(Wrappers.<SysUserDept>lambdaQuery()
                    .in(SysUserDept::getUserId, userIdList));

            // 取消关联角色
            sysRoleUserService.remove(Wrappers.<SysRoleUser>lambdaQuery()
                    .in(SysRoleUser::getUserId, userIdList));
        });
    }

    /**
     * 移除租户和部门
     *
     * @param id 租户ID
     */
    public void removeTenantAndDept(long id) {
        TenantEnv.runAs(id, () -> {
            sysTenantService.removeTenantById(id);
            sysDeptService.remove(Wrappers.lambdaQuery());
        });
    }

    /**
     * 移除租户权限和角色
     *
     * @param id 租户ID
     */
    public void removeTenantAuthorityAndRole(long id) {
        TenantEnv.runAs(id, () -> {
            List<Long> roleIds = sysRoleService.list(Wrappers.<SysRole>lambdaQuery()
                    .eq(SysRole::getTenantId, id)).stream().map(SysRole::getId).toList();

            sysRoleService.remove(Wrappers.<SysRole>lambdaQuery()
                    .eq(SysRole::getTenantId, id));

            sysRoleGroupService.remove(Wrappers.<SysRoleGroup>lambdaQuery()
                    .eq(SysRoleGroup::getTenantId, id));

            sysRoleAuthorityService.remove(Wrappers.<SysRoleAuthority>lambdaQuery()
                    .in(SysRoleAuthority::getRoleId, roleIds));

            sysAuthorityService.remove(Wrappers.<SysAuthority>lambdaQuery()
                    .eq(SysAuthority::getTenantId, id));
        });
    }
}
