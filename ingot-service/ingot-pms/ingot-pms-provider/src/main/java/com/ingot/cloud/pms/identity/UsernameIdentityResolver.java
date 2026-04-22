package com.ingot.cloud.pms.identity;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.service.biz.BizAppService;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.cloud.pms.service.domain.SysUserTenantService;
import com.ingot.framework.account.web.support.AuthContextSupport;
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
    private final AuthContextSupport authContextSupport;

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
        if (user == null) {
            // 2.作为用户名查询
            user = sysUserService.getOne(Wrappers.<SysUser>lambdaQuery()
                    .eq(SysUser::getUsername, username));
        }
        UserDetailsResponse response = IdentityUtil.map(user, request.getUserType(), request.getTenant(),
                sysTenantService, sysUserTenantService, bizUserService, bizAppService, bizRoleService);
        // 用户名/密码登录：由账号域共享工具填充认证上下文
        // - 硬过期位 credentialsNonExpired：用于阻断登录
        // - meta：锁定到期时间 / 失败计数 / 阈值 / 提示节奏，用于 Auth 侧生成友好提示
        if (user != null) {
            authContextSupport.fill(response, user.getId(), request.getUserType());
        }
        return response;
    }
}
