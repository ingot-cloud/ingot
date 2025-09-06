package com.ingot.cloud.pms.service.biz.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.AppRole;
import com.ingot.cloud.pms.api.model.domain.AppUser;
import com.ingot.cloud.pms.api.model.domain.AppUserTenant;
import com.ingot.cloud.pms.api.model.convert.UserConvert;
import com.ingot.cloud.pms.service.biz.SupportUserDetailsService;
import com.ingot.cloud.pms.service.domain.AppRoleService;
import com.ingot.cloud.pms.service.domain.AppUserService;
import com.ingot.cloud.pms.service.domain.AppUserTenantService;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.cloud.pms.social.SocialProcessorManager;
import com.ingot.framework.commons.model.common.AllowTenantDTO;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>Description  : AppSupportUserDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/13.</p>
 * <p>Time         : 10:31 AM.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppSupportUserDetailsService implements SupportUserDetailsService<AppUser> {
    private final SysTenantService sysTenantService;
    private final AppUserService appUserService;
    private final AppRoleService appRoleService;
    private final AppUserTenantService appUserTenantService;

    private final SocialProcessorManager socialProcessorManager;
    private final UserConvert userConvert;

    @Override
    public boolean support(UserDetailsRequest request) {
        return request.getUserType() == UserTypeEnum.APP;
    }

    @Override
    public UserDetailsResponse getUserDetails(UserDetailsRequest request) {
        return commonGetUserDetails(request, socialProcessorManager);
    }

    public UserDetailsResponse getUserAuthDetails(UserDetailsRequest request) {
        String username = request.getUsername();
        // 1.作为手机号查询
        AppUser user = appUserService.getOne(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getPhone, username));
        if (user != null) {
            return map(user, request.getUserType(), request.getTenant());
        }
        // 2.作为用户名
        user = appUserService.getOne(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getUsername, username));
        return map(user, request.getUserType(), request.getTenant());
    }

    @Override
    public List<AllowTenantDTO> getAllowTenants(AppUser user) {
        // 1.获取可以访问的租户列表
        List<AppUserTenant> userTenantList = appUserTenantService.list(
                Wrappers.<AppUserTenant>lambdaQuery()
                        .eq(AppUserTenant::getUserId, user.getId()));
        return getAllowTenantList(user, userTenantList, sysTenantService);
    }

    @Override
    public UserDetailsResponse userToUserDetailsResponse(AppUser user) {
        return userConvert.toUserDetails(user);
    }

    @Override
    public void setRoles(UserDetailsResponse result, AppUser user, Long loginTenant) {
        List<AppRole> roles = appRoleService.getRolesOfUser(user.getId());
        List<String> roleCodes = getRoleCodes(roles, loginTenant);
        if (CollUtil.isEmpty(roleCodes)) {
            return;
        }
        result.setRoles(roleCodes);
    }
}
