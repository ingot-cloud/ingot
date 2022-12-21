package com.ingot.cloud.pms.service.biz.impl;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysRoleUser;
import com.ingot.cloud.pms.service.biz.UserOpsChecker;
import com.ingot.cloud.pms.service.domain.SysRoleService;
import com.ingot.cloud.pms.service.domain.SysRoleUserService;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.security.common.utils.RoleUtils;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : UserOpsCheckerImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/21.</p>
 * <p>Time         : 7:10 PM.</p>
 */
@Service
@RequiredArgsConstructor
public class UserOpsCheckerImpl implements UserOpsChecker {
    private final SysRoleService sysRoleService;
    private final SysRoleUserService sysRoleUserService;

    private final AssertionChecker assertI18nService;

    @Override
    public void removeUser(long id) {
        long userId = SecurityAuthContext.getUser().getId();
        assertI18nService.checkOperation(userId != id, "UserOpsCheckerImpl.RemoveSelfFailed");

        Optional<SysRole> admin = getAdmin(id);
        if (!admin.isPresent()) {
            return;
        }

        // 如果删除的用户是admin用户，那么至少保留一个admin用户
        assertI18nService.checkOperation(
                sysRoleUserService.count(
                        Wrappers.<SysRoleUser>lambdaQuery()
                                .eq(SysRoleUser::getRoleId, admin.get().getId())) > 1,
                "UserOpsCheckerImpl.RemoveAdminFailed");
    }

    @Override
    public void disableUser(long id) {
        long userId = SecurityAuthContext.getUser().getId();
        assertI18nService.checkOperation(userId != id, "UserOpsCheckerImpl.DisableSelfFailed");

        Optional<SysRole> admin = getAdmin(id);
        if (!admin.isPresent()) {
            return;
        }

        // 如果禁用的用户是admin用户，那么至少保留一个admin用户
        assertI18nService.checkOperation(
                sysRoleUserService.count(
                        Wrappers.<SysRoleUser>lambdaQuery()
                                .eq(SysRoleUser::getRoleId, admin.get().getId())) > 1,
                "UserOpsCheckerImpl.DisableAdminFailed");
    }

    private Optional<SysRole> getAdmin(long id) {
        List<SysRole> roles = sysRoleService.getRolesOfUser(id);
        return roles.stream()
                .filter(item -> RoleUtils.isAdmin(item.getCode()))
                .findFirst();
    }
}
