package com.ingot.cloud.pms.identity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.ingot.cloud.pms.api.model.convert.UserConvert;
import com.ingot.cloud.pms.api.model.domain.MetaApp;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.domain.SysUserTenant;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.api.model.types.UserTenantType;
import com.ingot.cloud.pms.common.BizUtils;
import com.ingot.cloud.pms.service.biz.BizAppService;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.cloud.pms.service.domain.SysUserTenantService;
import com.ingot.framework.commons.model.common.TenantMainDTO;
import com.ingot.framework.commons.model.enums.UserStatusEnum;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.authority.InAuthorityUtils;
import com.ingot.framework.tenant.TenantEnv;

/**
 * <p>Description  : IdentityUtil.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/3.</p>
 * <p>Time         : 15:52.</p>
 */
public class IdentityUtil {
    /**
     * 映射用户信息
     *
     * @param user                 用户信息
     * @param userType             用户类型
     * @param tenant               租户ID
     * @param sysTenantService     租户服务
     * @param sysUserTenantService 用户租户服务
     * @param bizUserService       用户服务
     * @param bizAppService        应用服务
     * @param bizRoleService       角色服务
     * @return 用户信息
     */
    public static UserDetailsResponse map(SysUser user,
                                          UserTypeEnum userType,
                                          Long tenant,
                                          SysTenantService sysTenantService,
                                          SysUserTenantService sysUserTenantService,
                                          BizUserService bizUserService,
                                          BizAppService bizAppService,
                                          BizRoleService bizRoleService) {
        return TenantEnv.applyAs(tenant, () -> Optional.ofNullable(user)
                .map(value -> {
                    List<TenantMainDTO> allows = getAllowTenants(user, sysTenantService, sysUserTenantService);
                    UserStatusEnum userStatus = BizUtils.getUserStatus(allows, value.getStatus(), tenant);
                    value.setStatus(userStatus);

                    UserDetailsResponse result = UserConvert.INSTANCE.toUserDetails(value);
                    result.setTenant(tenant);
                    result.setUserType(userType.getValue());
                    result.setAllows(allows);

                    // 如果已经被禁用那么直接返回
                    if (userStatus == UserStatusEnum.LOCK) {
                        return result;
                    }

                    // 设置用户Scope
                    List<String> scopes = new ArrayList<>();

                    // 确认登录的租户不为空，那么查询用户在当前租户下的Scope
                    if (tenant != null) {
                        scopes.addAll(getScopes(tenant, user, bizUserService, bizAppService, bizRoleService));
                    } else {
                        scopes.addAll(allows.stream()
                                .flatMap(org ->
                                        TenantEnv.applyAs(Long.parseLong(org.getId()),
                                                        () -> getScopes(Long.parseLong(org.getId()), user,
                                                                bizUserService, bizAppService, bizRoleService))
                                                .stream())
                                .toList());
                    }
                    result.setScopes(scopes);
                    return result;
                }).orElse(null));
    }

    private static List<TenantMainDTO> getAllowTenants(SysUser user,
                                                       SysTenantService sysTenantService,
                                                       SysUserTenantService sysUserTenantService) {
        // 1.获取可以访问的租户列表
        List<SysUserTenant> userTenantList = sysUserTenantService.getUserOrgs(user.getId());
        if (CollUtil.isEmpty(userTenantList)) {
            return ListUtil.empty();
        }
        return BizUtils.getTenants(sysTenantService,
                userTenantList.stream()
                        .map(UserTenantType::getTenantId).collect(Collectors.toSet()),
                (item) -> item.setMain(userTenantList.stream()
                        .anyMatch(t ->
                                Objects.equals(t.getTenantId(), Long.parseLong(item.getId())) && t.getMain())));
    }

    private static List<String> getScopes(Long tenant,
                                          SysUser user,
                                          BizUserService bizUserService,
                                          BizAppService bizAppService,
                                          BizRoleService bizRoleService) {
        // 查询所有角色
        List<RoleType> roles = bizUserService.getUserRoles(user.getId());
        if (CollUtil.isEmpty(roles)) {
            return ListUtil.empty();
        }
        // InAuthorityUtils.authorityWithTenant 包装角色编码
        List<String> scopes = new ArrayList<>(getRoleCodes(roles, tenant));
        // 查询组织不可用应用
        List<MetaApp> disabledApps = bizAppService.getDisabledApps();
        List<String> authorities = bizRoleService.getRolesPermissions(roles).stream()
                .filter(auth -> disabledApps.stream()
                        .noneMatch(app -> Objects.equals(auth.getId(), app.getPermissionId())))
                .map(auth -> InAuthorityUtils.authorityWithTenant(auth.getCode(), tenant))
                .toList();
        scopes.addAll(authorities);

        return scopes;
    }

    private static List<String> getRoleCodes(List<? extends RoleType> roles, Long loginTenant) {
        if (CollUtil.isEmpty(roles)) {
            return ListUtil.empty();
        }
        return roles.stream()
                .map(item ->
                        InAuthorityUtils.authorityWithTenant(item.getCode(), loginTenant))
                .toList();
    }
}
