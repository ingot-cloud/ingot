package com.ingot.cloud.pms.identity;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.service.biz.BizAppService;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.cloud.pms.service.domain.SysUserTenantService;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserIdentityTypeEnum;
import com.ingot.framework.security.core.identity.UserIdentityResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : UsernameIdentityResolver.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/3.</p>
 * <p>Time         : 15:24.</p>
 */
@Service
@RequiredArgsConstructor
public class UsernameIdentityResolver implements UserIdentityResolver {
    private final SysUserService sysUserService;
    private final SysTenantService sysTenantService;
    private final SysUserTenantService sysUserTenantService;

    private final BizAppService bizAppService;
    private final BizRoleService bizRoleService;
    private final BizUserService bizUserService;

    @Override
    public boolean supports(UserIdentityTypeEnum type) {
        return type == UserIdentityTypeEnum.USERNAME;
    }

    @Override
    public UserDetailsResponse load(UserDetailsRequest request) {
        String username = request.getUsername();
        // 1.作为手机号查询
        SysUser user = sysUserService.getOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getPhone, username));
        if (user != null) {
            return IdentityUtil.map(user, request.getUserType(), request.getTenant(),
                    sysTenantService, sysUserTenantService, bizUserService, bizAppService, bizRoleService);
        }
        // 2.作为用户名查询
        user = sysUserService.getOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username));
        return IdentityUtil.map(user, request.getUserType(), request.getTenant(),
                sysTenantService, sysUserTenantService, bizUserService, bizAppService, bizRoleService);
    }


}
