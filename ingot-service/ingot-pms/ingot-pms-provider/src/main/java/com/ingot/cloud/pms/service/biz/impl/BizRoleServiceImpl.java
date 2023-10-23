package com.ingot.cloud.pms.service.biz.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysRoleUser;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.domain.SysRoleService;
import com.ingot.cloud.pms.service.domain.SysRoleUserService;
import com.ingot.framework.core.model.common.RelationDTO;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.security.common.constants.RoleConstants;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    private final AssertionChecker assertionChecker;

    @Override
    public void orgRoleBindUsers(RelationDTO<Long, Long> params) {
        SysRole managerRole = sysRoleService.getRoleByCode(RoleConstants.ROLE_MANAGER_CODE);
        long roleId = params.getId();
        assertionChecker.checkOperation(roleId != managerRole.getId(),
                "BizRoleServiceImpl.CantBindAndRemoveManager");

        sysRoleUserService.roleBindUsers(params);
    }

    @Override
    public void setOrgUserRoles(long userId, List<Long> roles) {
        // 如果操作的自己，那么需要判断角色
        long opsUserId = SecurityAuthContext.getUser().getId();
        if (opsUserId == userId) {
            ensureRoles(userId, roles, RoleConstants.ROLE_ADMIN_CODE);
            ensureRoles(userId, roles, RoleConstants.ROLE_MANAGER_CODE);
        }

        sysRoleUserService.setUserRoles(userId, roles);
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
