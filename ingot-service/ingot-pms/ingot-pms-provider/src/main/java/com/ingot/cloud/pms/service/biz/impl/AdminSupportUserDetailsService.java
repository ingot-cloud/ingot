package com.ingot.cloud.pms.service.biz.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.transform.UserTrans;
import com.ingot.cloud.pms.common.BizUtils;
import com.ingot.cloud.pms.service.biz.SupportUserDetailsService;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.cloud.pms.social.SocialProcessorManager;
import com.ingot.framework.core.model.common.AllowTenantDTO;
import com.ingot.framework.core.model.enums.SocialTypeEnums;
import com.ingot.framework.security.common.constants.UserType;
import com.ingot.framework.security.core.userdetails.UserDetailsRequest;
import com.ingot.framework.security.core.userdetails.UserDetailsResponse;
import com.ingot.framework.security.oauth2.core.IngotAuthorizationGrantType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private final SysUserDeptService sysUserDeptService;

    private final SocialProcessorManager socialProcessorManager;
    private final UserTrans userTrans;

    @Override
    public boolean support(UserDetailsRequest request) {
        return request.getUserType() == UserType.ADMIN;
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
        SysUser user = sysUserService.getOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username));
        return map(user, request.getUserType());
    }

    public UserDetailsResponse getUserAuthDetailsSocial(UserDetailsRequest request) {
        SocialTypeEnums socialType = request.getSocialType();
        String socialCode = request.getSocialCode();
        String uniqueID = socialProcessorManager.getUniqueID(socialType, socialCode);
        return map(socialProcessorManager.getUserInfo(socialType, uniqueID), request.getUserType());
    }

    private UserDetailsResponse map(SysUser user, UserType userType) {
        return Optional.ofNullable(user)
                .map(value -> {
                    List<AllowTenantDTO> allows = getTenantList(user);
                    value.setStatus(BizUtils.getUserStatus(allows, value.getStatus()));

                    UserDetailsResponse result = userTrans.toUserDetails(value);
                    result.setUserType(userType.getValue());
                    result.setAllows(allows);

                    Long defaultTenantId = CollUtil.emptyIfNull(allows)
                            .stream()
                            .filter(AllowTenantDTO::getMain)
                            .map(AllowTenantDTO::getId)
                            .findFirst()
                            .orElse(null);
                    long deptId = 0;
                    if (defaultTenantId != null) {
                        SysUserDept userDept = sysUserDeptService.getByUserIdAndTenant(user.getId(), defaultTenantId);
                        if (userDept != null) {
                            deptId = userDept.getDeptId();
                        }
                    }
                    // 查询拥有的角色
                    List<SysRole> roles = sysRoleService.getAllRolesOfUser(user.getId(), deptId);

                    setRoles(result, roles);
                    return result;
                }).orElse(null);
    }

    private List<AllowTenantDTO> getTenantList(SysUser user) {
        // 1.获取可以访问的租户列表
        List<SysUserTenant> userTenantList = sysUserTenantService.list(
                Wrappers.<SysUserTenant>lambdaQuery()
                        .eq(SysUserTenant::getUserId, user.getId()));

        return BizUtils.getAllows(sysTenantService,
                userTenantList.stream()
                        .map(SysUserTenant::getTenantId).collect(Collectors.toSet()),
                (item) -> item.setMain(userTenantList.stream()
                        .anyMatch(t -> Objects.equals(t.getTenantId(), item.getId()) && t.getMain())));
    }

    private void setRoles(UserDetailsResponse result, List<SysRole> roles) {
        List<String> roleCodes = roles.stream()
                .map(SysRole::getCode)
                .collect(Collectors.toList());
        // 拥有的权限
        Set<String> authorities = sysAuthorityService.getAuthorityByRoles(roles)
                .stream().map(SysAuthority::getCode).collect(Collectors.toSet());
        roleCodes.addAll(authorities);
        result.setRoles(roleCodes);
    }
}
