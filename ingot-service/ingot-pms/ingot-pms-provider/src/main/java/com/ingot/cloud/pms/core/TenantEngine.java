package com.ingot.cloud.pms.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.dto.org.CreateOrgDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserDTO;
import com.ingot.cloud.pms.api.model.enums.AuthorityTypeEnums;
import com.ingot.cloud.pms.api.model.enums.DeptRoleScopeEnum;
import com.ingot.cloud.pms.api.model.enums.RoleTypeEnums;
import com.ingot.cloud.pms.api.model.transform.AuthorityTrans;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.cloud.pms.service.biz.BizDeptService;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.core.constants.IDConstants;
import com.ingot.framework.core.model.common.RelationDTO;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.core.utils.tree.TreeNode;
import com.ingot.framework.core.utils.tree.TreeUtils;
import com.ingot.framework.security.common.constants.RoleConstants;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
    private final BizIdGen bizIdGen;
    private final AuthorityTrans authorityTrans;

    public SysTenant createTenant(CreateOrgDTO params) {
        String orgCode = bizIdGen.genOrgCode();
        SysTenant tenant = new SysTenant();
        tenant.setName(params.getName());
        tenant.setAvatar(params.getAvatar());
        tenant.setCode(orgCode);
        sysTenantService.createTenant(tenant);
        return tenant;
    }

    public SysDept createDept(SysTenant tenant) {
        return TenantEnv.applyAs(tenant.getId(), () -> {
            SysDept dept = new SysDept();
            dept.setName(tenant.getName());
            dept.setScope(DeptRoleScopeEnum.CURRENT_CHILD);
            dept.setMainFlag(Boolean.TRUE);
            sysDeptService.createDept(dept);
            return dept;
        });
    }

    public List<SysRole> createRoles(SysTenant tenant) {
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

    public List<SysAuthority> createAuthority(SysTenant tenant) {
        List<AuthorityTreeNodeVO> templateAuthorities = sysAuthorityService.list(Wrappers.<SysAuthority>lambdaQuery()
                        .eq(SysAuthority::getType, AuthorityTypeEnums.Tenant))
                .stream().map(authorityTrans::to).toList();
        List<AuthorityTreeNodeVO> tree = TreeUtils.build(templateAuthorities);

        return TenantEnv.applyAs(tenant.getId(), () -> {
            List<SysAuthority> orgAuthorities = new ArrayList<>();
            createAuthorityFn(orgAuthorities, tree, IDConstants.ROOT_TREE_ID);
            return orgAuthorities;
        });
    }

    private void createAuthorityFn(List<SysAuthority> collect,
                                   List<? extends TreeNode<Long>> tree, long pid) {

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

                if (CollUtil.isNotEmpty(node.getChildren())) {
                    createAuthorityFn(collect, node.getChildren(), item.getId());
                }
            }
        }
    }

    public void createUser(CreateOrgDTO params, SysTenant tenant, List<SysRole> roles, SysDept dept) {
        TenantEnv.runAs(tenant.getId(), () -> {
            SysRole role = roles.stream()
                    .filter(item -> StrUtil.equals(item.getCode(), RoleConstants.ROLE_MANAGER_CODE))
                    .findFirst().orElseThrow();

            // 如果已经存在注册用户，那么直接关联新组织信息
            SysUser user = sysUserService.getOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getPhone, params.getPhone()));
            if (user != null) {
                // 加入租户
                sysUserTenantService.joinTenant(user.getId());
                // 设置部门
                bizDeptService.setUserDeptsEnsureMainDept(user.getId(), List.of(dept.getId()));
                // 设置角色
                sysRoleUserService.setUserRoles(user.getId(), List.of(role.getId()));
                return;
            }

            UserDTO userDTO = new UserDTO();
            userDTO.setDeptIds(List.of(dept.getId()));
            userDTO.setRoleIds(List.of(role.getId()));
            userDTO.setInitPwd(Boolean.TRUE);
            userDTO.setUsername(params.getPhone());
            userDTO.setPhone(params.getPhone());
            userDTO.setNewPassword(params.getPhone());
            userDTO.setNickname(params.getNickname());
            sysUserService.createUser(userDTO);
        });
    }

    public void roleBindAuthorities(List<SysRole> roles, List<SysAuthority> authorities) {
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
    public void removeUserRelation(long id) {
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
    }

    public void removeTenantAndDept(long id) {
        sysTenantService.removeTenantById(id);
        sysDeptService.remove(Wrappers.lambdaQuery());
    }

    public void removeAuthorityAndRole(long id) {
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
    }
}
