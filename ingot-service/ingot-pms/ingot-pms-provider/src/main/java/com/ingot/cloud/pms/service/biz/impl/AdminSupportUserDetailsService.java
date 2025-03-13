package com.ingot.cloud.pms.service.biz.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.transform.UserTrans;
import com.ingot.cloud.pms.common.BizUtils;
import com.ingot.cloud.pms.service.biz.SupportUserDetailsService;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.cloud.pms.social.SocialProcessorManager;
import com.ingot.framework.core.model.common.AllowTenantDTO;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.model.enums.SocialTypeEnum;
import com.ingot.framework.core.model.security.UserTypeEnum;
import com.ingot.framework.security.core.authority.IngotAuthorityUtils;
import com.ingot.framework.core.model.security.UserDetailsRequest;
import com.ingot.framework.core.model.security.UserDetailsResponse;
import com.ingot.framework.security.oauth2.core.IngotAuthorizationGrantType;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>Description  : AdminSupportUserDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/13.</p>
 * <p>Time         : 10:28 AM.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSupportUserDetailsService implements SupportUserDetailsService {
    private final SysTenantService sysTenantService;
    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SysAuthorityService sysAuthorityService;
    private final SysUserTenantService sysUserTenantService;
    private final SysApplicationTenantService sysApplicationTenantService;

    private final SocialProcessorManager socialProcessorManager;
    private final UserTrans userTrans;

    @Override
    public boolean support(UserDetailsRequest request) {
        return request.getUserType() == UserTypeEnum.ADMIN;
    }

    @Override
    public UserDetailsResponse getUserDetails(UserDetailsRequest request) {
        AuthorizationGrantType grantType = new AuthorizationGrantType(request.getGrantType());
        if (ObjectUtil.equals(IngotAuthorizationGrantType.PASSWORD, grantType)) {
            return getUserAuthDetails(request);
        }
        if (ObjectUtil.equals(IngotAuthorizationGrantType.SOCIAL, grantType)) {
            return getUserAuthDetailsSocial(request);
        }
        return null;
    }

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

    public UserDetailsResponse getUserAuthDetailsSocial(UserDetailsRequest request) {
        return TenantEnv.applyAs(request.getTenant(), () -> {
            SocialTypeEnum socialType = request.getSocialType();
            String socialCode = request.getSocialCode();
            String uniqueID = socialProcessorManager.getUniqueID(socialType, socialCode);
            return map(socialProcessorManager.getUserInfo(socialType, uniqueID), request.getUserType(), request.getTenant());
        });
    }

    private UserDetailsResponse map(SysUser user, UserTypeEnum userType, Long tenant) {
        return TenantEnv.applyAs(tenant, () -> Optional.ofNullable(user)
                .map(value -> {
                    List<AllowTenantDTO> allows = getTenantList(user);
                    value.setStatus(BizUtils.getUserStatus(allows, value.getStatus(), tenant));

                    UserDetailsResponse result = userTrans.toUserDetails(value);
                    result.setTenant(tenant);
                    result.setUserType(userType.getValue());
                    result.setAllows(allows);

                    // 查询拥有的角色
                    List<SysRole> roles = sysRoleService.getRolesOfUser(user.getId());

                    result.setRoles(loadRoles(roles, tenant));
                    return result;
                }).orElse(null));
    }

    private List<AllowTenantDTO> getTenantList(SysUser user) {
        // 1.获取可以访问的租户列表
        List<SysUserTenant> userTenantList = sysUserTenantService.getUserOrgs(user.getId());

        return BizUtils.getAllows(sysTenantService,
                userTenantList.stream()
                        .map(SysUserTenant::getTenantId).collect(Collectors.toSet()),
                (item) -> item.setMain(userTenantList.stream()
                        .anyMatch(t -> Objects.equals(t.getTenantId(), Long.parseLong(item.getId())) && t.getMain())));
    }

    private List<String> loadRoles(List<SysRole> roles, Long loginTenant) {
        if (CollUtil.isEmpty(roles)) {
            return CollUtil.empty(String.class);
        }
        List<String> roleCodes = roles.stream()
                .map(item -> {
                    if (loginTenant != null) {
                        return IngotAuthorityUtils.authorityWithTenant(item.getCode(), loginTenant);
                    }
                    return IngotAuthorityUtils.authorityWithTenant(item.getCode(), item.getTenantId());
                })
                .collect(Collectors.toList());
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
                        return IngotAuthorityUtils.authorityWithTenant(item.getCode(), loginTenant);
                    }
                    return IngotAuthorityUtils.authorityWithTenant(item.getCode(), item.getTenantId());
                }).collect(Collectors.toSet());

        roleCodes.addAll(authorityCodeList);
        return roleCodes;
    }
}
