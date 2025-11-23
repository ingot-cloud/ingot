package com.ingot.cloud.pms.service.biz.impl;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.TenantRoleUserPrivate;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.biz.UserOpsChecker;
import com.ingot.cloud.pms.service.domain.TenantRoleUserPrivateService;
import com.ingot.framework.commons.utils.RoleUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
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
    private final BizUserService bizUserService;
    private final TenantRoleUserPrivateService tenantRoleUserPrivateService;

    private final AssertionChecker assertionChecker;

    @Override
    public void removeUser(long userId) {
        long operatorId = SecurityAuthContext.getUser().getId();
        assertionChecker.checkOperation(userId != operatorId,
                "UserOpsCheckerImpl.RemoveSelfFailed");

        Optional<RoleType> admin = getAdmin(userId);
        if (admin.isEmpty()) {
            return;
        }

        // 如果删除的用户是admin用户，那么至少保留一个admin用户
        assertionChecker.checkOperation(
                tenantRoleUserPrivateService.count(
                        Wrappers.<TenantRoleUserPrivate>lambdaQuery()
                                .eq(TenantRoleUserPrivate::getRoleId, admin.get().getId())) > 1,
                "UserOpsCheckerImpl.RemoveAdminFailed");
    }

    @Override
    public void disableUser(long id) {
        long userId = SecurityAuthContext.getUser().getId();
        assertionChecker.checkOperation(userId != id, "UserOpsCheckerImpl.DisableSelfFailed");

        Optional<RoleType> admin = getAdmin(userId);
        if (admin.isEmpty()) {
            return;
        }

        // 如果禁用的用户是admin用户，那么至少保留一个admin用户
        assertionChecker.checkOperation(
                tenantRoleUserPrivateService.count(
                        Wrappers.<TenantRoleUserPrivate>lambdaQuery()
                                .eq(TenantRoleUserPrivate::getRoleId, admin.get().getId())) > 1,
                "UserOpsCheckerImpl.DisableAdminFailed");
    }

    private Optional<RoleType> getAdmin(long userId) {
        List<RoleType> roles = bizUserService.getUserRoles(userId);
        return roles.stream()
                .filter(item -> RoleUtil.isAdmin(item.getCode()))
                .findFirst();
    }
}
