package com.ingot.cloud.pms.service.biz.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.convert.UserConvert;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.service.biz.SupportUserDetailsService;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.cloud.pms.social.SocialProcessorManager;
import com.ingot.framework.commons.model.common.AllowTenantDTO;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.authority.InAuthorityUtils;
import com.ingot.framework.tenant.TenantEnv;
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
    private final SysRoleService sysRoleService;
    private final SysAuthorityService sysAuthorityService;
    private final SysUserTenantService sysUserTenantService;
    private final SysApplicationTenantService sysApplicationTenantService;

    private final SocialProcessorManager socialProcessorManager;
    private final UserConvert userConvert;

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
    public void setRoles(UserDetailsResponse result, SysUser user, Long loginTenant) {
        List<SysRole> roles = sysRoleService.getRolesOfUser(user.getId());
        List<String> roleCodes = getRoleCodes(roles, loginTenant);
        if (CollUtil.isEmpty(roleCodes)) {
            return;
        }

        // 角色拥有的权限
        List<SysAuthority> authorities = sysAuthorityService.getAuthorityByRoles(roles);

        // 查询所有组织的应用
        List<SysApplicationTenant> appList = TenantEnv.globalApply(() ->
                CollUtil.emptyIfNull(sysApplicationTenantService.list(
                        Wrappers.<SysApplicationTenant>lambdaQuery()
                                .eq(SysApplicationTenant::getStatus, CommonStatusEnum.LOCK))));
        List<SysAuthority> removeAuthorities = appList.stream()
                .filter(app -> authorities.stream()
                        .anyMatch(auth -> Objects.equals(auth.getId(), app.getAuthorityId())))
                .map(app -> authorities.stream()
                        .filter(auth -> Objects.equals(auth.getId(), app.getAuthorityId()))
                        .findFirst().orElse(null))
                .toList();

        // 过滤权限，去掉已经禁用应用的权限
        Set<String> authorityCodeList = authorities.stream()
                .filter(item -> removeAuthorities.stream()
                        .noneMatch(remove -> {
                            if (Objects.equals(remove.getId(), item.getId())) {
                                return true;
                            }

                            long orgId = loginTenant != null ? loginTenant : item.getTenantId();
                            return orgId == remove.getTenantId() && StrUtil.startWith(item.getCode(), remove.getCode());
                        }))
                .map(item -> {
                    if (loginTenant != null) {
                        return InAuthorityUtils.authorityWithTenant(item.getCode(), loginTenant);
                    }
                    return InAuthorityUtils.authorityWithTenant(item.getCode(), item.getTenantId());
                }).collect(Collectors.toSet());

        List<String> finalRoleCodes = new ArrayList<>(roleCodes);
        finalRoleCodes.addAll(authorityCodeList);
        result.setRoles(finalRoleCodes);
    }
}
