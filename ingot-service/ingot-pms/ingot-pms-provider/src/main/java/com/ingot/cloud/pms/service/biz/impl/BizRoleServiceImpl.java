package com.ingot.cloud.pms.service.biz.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysRoleGroup;
import com.ingot.cloud.pms.api.model.domain.SysRoleUser;
import com.ingot.cloud.pms.api.model.dto.authority.AuthorityFilterDTO;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnums;
import com.ingot.cloud.pms.api.model.transform.AuthorityTrans;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.cloud.pms.core.AuthorityUtils;
import com.ingot.cloud.pms.core.org.TenantOps;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.core.model.common.RelationDTO;
import com.ingot.framework.core.utils.tree.TreeNode;
import com.ingot.framework.core.utils.tree.TreeUtils;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.security.common.constants.RoleConstants;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.tenant.TenantContextHolder;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

/**
 * <p>Description  : BizRoleServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/21.</p>
 * <p>Time         : 8:58 AM.</p>
 */
@Service
@RequiredArgsConstructor
public class BizRoleServiceImpl implements BizRoleService {
    private final SysRoleUserService sysRoleUserService;
    private final SysRoleService sysRoleService;
    private final SysRoleGroupService sysRoleGroupService;
    private final SysRoleAuthorityService sysRoleAuthorityService;
    private final SysApplicationTenantService sysApplicationTenantService;
    private final SysAuthorityService sysAuthorityService;

    private final TenantOps tenantOps;
    private final AssertionChecker assertionChecker;
    private final AuthorityTrans authorityTrans;

    @Override
    public List<AuthorityTreeNodeVO> getOrgAuthority(long orgId) {
        return TenantEnv.applyAs(orgId, () -> {
            List<AuthorityTreeNodeVO> authorities = AuthorityUtils.getOrgAuthorities(
                    orgId, sysApplicationTenantService, sysAuthorityService, authorityTrans);
            return TreeUtils.build(authorities);
        });
    }

    @Override
    public List<AuthorityTreeNodeVO> getOrgRoleAuthorities(long roleId, AuthorityFilterDTO condition) {
        List<SysAuthority> authorities = sysRoleAuthorityService.getAuthoritiesByRole(roleId);
        List<SysAuthority> finallyAuthorities = AuthorityUtils.filterOrgLockAuthority(
                authorities, sysApplicationTenantService);

        return AuthorityUtils.mapTree(finallyAuthorities, condition, authorityTrans);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void orgRoleBindAuthorities(RelationDTO<Long, Long> params) {
        long roleId = params.getId();
        List<Long> bindList = params.getBindIds();
        SysRole role = sysRoleService.getById(roleId);
        assertionChecker.checkOperation(!StrUtil.equals(role.getCode(), RoleConstants.ROLE_ORG_ADMIN_CODE),
                "BizRoleServiceImpl.CantOperateManager");

        List<Long> authorities = CollUtil.emptyIfNull(AuthorityUtils.getOrgAuthorities(
                        TenantContextHolder.get(), sysApplicationTenantService, sysAuthorityService, authorityTrans))
                .stream().map(TreeNode::getId).toList();

        if (CollUtil.isNotEmpty(bindList)) {
            boolean canBind = new HashSet<>(authorities).containsAll(bindList);
            assertionChecker.checkOperation(canBind, "BizRoleServiceImpl.CantBindAndUnBindAuth");
        }

        // 更新选取
        sysRoleAuthorityService.roleBindAuthorities(params);
    }

    @Override
    public void orgRoleBindUsers(RelationDTO<Long, Long> params) {
        SysRole managerRole = sysRoleService.getRoleByCode(RoleConstants.ROLE_ORG_ADMIN_CODE);
        long roleId = params.getId();
        assertionChecker.checkOperation(roleId != managerRole.getId(),
                "BizRoleServiceImpl.CantOperateManager");

        sysRoleUserService.roleBindUsers(params);
    }

    @Override
    public void setOrgUserRoles(long userId, List<Long> roles) {
        // 如果操作的自己，那么需要判断角色
        long opsUserId = SecurityAuthContext.getUser().getId();
        if (opsUserId == userId) {
            ensureRoles(userId, roles, RoleConstants.ROLE_ADMIN_CODE);
            ensureRoles(userId, roles, RoleConstants.ROLE_ORG_ADMIN_CODE);
        }

        sysRoleUserService.setUserRoles(userId, roles);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRoleEffectOrg(SysRole role, boolean isAdmin) {
        sysRoleService.createRole(role, isAdmin);
        if (role.getType() == OrgTypeEnums.Tenant) {
            tenantOps.createRole(role);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRoleEffectOrg(SysRole role, boolean isAdmin) {
        sysRoleService.updateRoleById(role, isAdmin);
        SysRole current = sysRoleService.getById(role.getId());
        if (current.getType() == OrgTypeEnums.Tenant) {
            tenantOps.updateRole(role);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRoleEffectOrg(long id, boolean isAdmin) {
        SysRole current = sysRoleService.getById(id);
        sysRoleService.removeRoleById(id, isAdmin);
        if (current.getType() == OrgTypeEnums.Tenant) {
            tenantOps.removeRole(current);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRoleGroupEffectOrg(SysRoleGroup group, boolean isAdmin) {
        sysRoleService.createGroup(group, isAdmin);
        if (group.getType() == OrgTypeEnums.Tenant) {
            tenantOps.createRoleGroup(group);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRoleGroupEffectOrg(SysRoleGroup group, boolean isAdmin) {
        sysRoleService.updateGroup(group, isAdmin);
        SysRoleGroup current = sysRoleGroupService.getById(group.getId());
        if (current.getType() == OrgTypeEnums.Tenant) {
            tenantOps.updateRoleGroup(group);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRoleGroupEffectOrg(long id, boolean isAdmin) {
        SysRoleGroup current = sysRoleGroupService.getById(id);
        sysRoleService.deleteGroup(id, isAdmin);
        if (current.getType() == OrgTypeEnums.Tenant) {
            tenantOps.removeRoleGroup(current);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void roleBindAuthoritiesEffectOrg(RelationDTO<Long, Long> params) {
        sysRoleAuthorityService.roleBindAuthorities(params);
        SysRole current = sysRoleService.getById(params.getId());
        if (current.getType() == OrgTypeEnums.Tenant) {
            tenantOps.roleBindAuthorities(params, current);
        }
    }

    private void ensureRoles(long userId, List<Long> roles, String roleCode) {
        // 如果当前组织包含指定角色，那么需要判断该用户是否有当前指定角色，如果有则确保该角色不被删除
        SysRole ensureRole = sysRoleService.getRoleByCode(roleCode);
        if (ensureRole != null) {
            long count = sysRoleUserService.count(Wrappers.<SysRoleUser>lambdaQuery()
                    .eq(SysRoleUser::getUserId, userId)
                    .eq(SysRoleUser::getRoleId, ensureRole.getId()));
            if (count > 0) {
                roles.add(ensureRole.getId());
            }
        }
    }
}
