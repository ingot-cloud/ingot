package com.ingot.cloud.pms.service.biz.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.convert.UserConvert;
import com.ingot.cloud.pms.api.model.domain.MetaApp;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.domain.SysUserTenant;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.service.biz.BizAppService;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.biz.SupportUserDetailsService;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.cloud.pms.service.domain.SysUserTenantService;
import com.ingot.cloud.pms.social.SocialProcessorManager;
import com.ingot.framework.commons.model.common.AllowTenantDTO;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.authority.InAuthorityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : AdminSupportUserDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/13.</p>
 * <p>Time         : 10:28 AM.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSupportUserDetailsService implements SupportUserDetailsService<SysUser> {
    private final SysTenantService sysTenantService;
    private final SysUserService sysUserService;
    private final SysUserTenantService sysUserTenantService;

    private final BizAppService bizAppService;
    private final BizRoleService bizRoleService;

    private final SocialProcessorManager socialProcessorManager;
    private final UserConvert userConvert;
    private final BizUserService bizUserService;

    @Override
    public boolean support(UserDetailsRequest request) {
        return request.getUserType() == UserTypeEnum.ADMIN;
    }

    @Override
    public UserDetailsResponse getUserDetails(UserDetailsRequest request) {
        return commonGetUserDetails(request, socialProcessorManager);
    }

    @Override
    public UserDetailsResponse getUserAuthDetails(UserDetailsRequest request) {
        String username = request.getUsername();
        // 1.作为手机号查询
        SysUser user = sysUserService.getOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getPhone, username));
        if (user != null) {
            return map(user, request.getUserType(), request.getTenant());
        }
        // 2.作为用户名查询
        user = sysUserService.getOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username));
        return map(user, request.getUserType(), request.getTenant());
    }

    @Override
    public List<AllowTenantDTO> getAllowTenants(SysUser user) {
        // 1.获取可以访问的租户列表
        List<SysUserTenant> userTenantList = sysUserTenantService.getUserOrgs(user.getId());
        return getAllowTenantList(user, userTenantList, sysTenantService);
    }

    @Override
    public UserDetailsResponse userToUserDetailsResponse(SysUser user) {
        return userConvert.toUserDetails(user);
    }

    @Override
    public List<String> getScopes(Long tenant, SysUser user) {
        // 查询所有角色
        List<RoleType> roles = bizUserService.getUserRoles(user.getId());
        if (CollUtil.isEmpty(roles)) {
            return ListUtil.empty();
        }
        // InAuthorityUtils.authorityWithTenant 包装角色编码
        List<String> scopes = new ArrayList<>(getRoleCodes(roles, tenant));
        // 查询组织不可用应用
        List<MetaApp> disabledApps = bizAppService.getDisabledApps();
        List<String> authorities = bizRoleService.getRolesAuthorities(roles).stream()
                .filter(auth -> disabledApps.stream()
                        .noneMatch(app -> Objects.equals(auth.getId(), app.getAuthorityId())))
                .map(auth -> InAuthorityUtils.authorityWithTenant(auth.getCode(), tenant))
                .toList();
        scopes.addAll(authorities);

        return scopes;
    }
}
