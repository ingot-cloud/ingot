package com.ingot.cloud.pms.identity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.BooleanUtil;
import com.ingot.cloud.pms.api.model.convert.UserConvert;
import com.ingot.cloud.pms.api.model.domain.PlatformApp;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.domain.SysUserTenant;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.api.model.types.UserTenantType;
import com.ingot.cloud.pms.common.BizUtils;
import com.ingot.cloud.pms.service.biz.BizAppService;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.biz.BizUserDeptService;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.cloud.pms.service.domain.SysUserTenantService;
import com.ingot.framework.commons.constants.PermissionConstants;
import com.ingot.framework.commons.model.common.TenantMainDTO;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.authority.InAuthorityUtils;
import com.ingot.framework.tenant.TenantEnv;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : IdentityUtil.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/3.</p>
 * <p>Time         : 15:52.</p>
 */
@Slf4j
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
     * @param bizUserDeptService   用户部门服务
     * @return 用户信息
     */
    public static UserDetailsResponse map(SysUser user,
                                          UserTypeEnum userType,
                                          Long tenant,
                                          SysTenantService sysTenantService,
                                          SysUserTenantService sysUserTenantService,
                                          BizUserService bizUserService,
                                          BizAppService bizAppService,
                                          BizRoleService bizRoleService,
                                          BizUserDeptService bizUserDeptService) {
        return TenantEnv.applyAs(tenant, () -> Optional.ofNullable(user)
                .map(value -> {
                    List<TenantMainDTO> allows = getAllowTenants(user, sysTenantService, sysUserTenantService);

                    // 租户维度可访问性：allows 不为空，且登录 tenant 在允许列表内
                    boolean tenantAccessible = CollUtil.isNotEmpty(allows)
                            && (tenant == null || allows.stream()
                            .anyMatch(item -> Long.parseLong(item.getId()) == tenant));

                    // 账号维度：来自 sys_user.enabled / sys_user.locked
                    boolean userEnabled = Boolean.TRUE.equals(value.getEnabled()) && tenantAccessible;
                    boolean userLocked = Boolean.TRUE.equals(value.getLocked());

                    UserDetailsResponse result = UserConvert.INSTANCE.toUserDetails(value);
                    result.setTenant(tenant);
                    result.setUserType(userType.getValue());
                    result.setAllows(allows);
                    result.setEnabled(userEnabled);
                    result.setLocked(userLocked);

                    // 如果账号不可用（禁用或锁定）则不需要查询 scope，直接返回
                    if (!userEnabled || userLocked) {
                        return result;
                    }

                    // 设置用户 Scope
                    List<String> scopes = new ArrayList<>();
                    // 强制修改密码
                    if (BooleanUtil.isTrue(user.getMustChangePwd())) {
                        scopes.add(PermissionConstants.INIT_PASSWORD);
                        result.setScopes(scopes);
                        return result;
                    }

                    // 确认登录的租户不为空，那么查询用户在当前租户下的 Scope 与部门
                    if (tenant != null) {
                        scopes.addAll(getScopes(tenant, user, bizUserService, bizAppService, bizRoleService));
                        result.setDeptIds(getUserDeptIds(user, bizUserDeptService));
                    } else {
                        // 未指定租户：把所有 allow 租户的 scope 串扁平，部门按租户聚合到 Map
                        Map<Long, List<Long>> tenantDeptIds = new LinkedHashMap<>();
                        for (TenantMainDTO org : allows) {
                            Long t = Long.parseLong(org.getId());
                            List<Long> deptIds = TenantEnv.applyAs(t, () -> {
                                scopes.addAll(getScopes(t, user, bizUserService, bizAppService, bizRoleService));
                                return getUserDeptIds(user, bizUserDeptService);
                            });
                            tenantDeptIds.put(t, deptIds);
                        }
                        result.setTenantDeptIds(tenantDeptIds);
                    }
                    result.setScopes(scopes);
                    return result;
                }).orElse(null));
    }

    private static List<Long> getUserDeptIds(SysUser user, BizUserDeptService bizUserDeptService) {
        List<Long> deptIds = bizUserDeptService.getDeptIds(user.getId());
        return deptIds == null ? List.of() : deptIds;
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
        List<PlatformApp> disabledApps = bizAppService.getDisabledApps();
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
