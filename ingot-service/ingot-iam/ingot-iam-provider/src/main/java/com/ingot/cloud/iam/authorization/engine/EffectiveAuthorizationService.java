package com.ingot.cloud.iam.authorization.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.iam.api.model.domain.PlatformApp;
import com.ingot.cloud.iam.api.model.domain.PlatformPermission;
import com.ingot.cloud.iam.api.model.domain.TenantRoleUserPrivate;
import com.ingot.cloud.iam.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.iam.api.model.types.PermissionType;
import com.ingot.cloud.iam.api.model.types.RoleType;
import com.ingot.cloud.iam.service.biz.BizAppService;
import com.ingot.cloud.iam.service.biz.BizRoleService;
import com.ingot.cloud.iam.service.domain.PlatformAppService;
import com.ingot.cloud.iam.service.domain.PlatformPermissionService;
import com.ingot.cloud.iam.service.domain.PlatformRoleService;
import com.ingot.cloud.iam.service.domain.TenantRolePrivateService;
import com.ingot.cloud.iam.service.domain.TenantRoleUserPrivateService;
import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.tenant.TenantContextHolder;
import com.ingot.framework.tenant.TenantEnv;
import com.ingot.framework.tenant.properties.TenantProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>按租户成员身份解析有效授权，保留角色绑定部门，并按应用可访问性过滤具体权限。</p>
 *
 * <p>租户管理员只动态获得本租户有效应用内能力，不能穿透平台运营域。菜单、登录补全与委派校验共用本解析。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class EffectiveAuthorizationService {

    private final BizRoleService bizRoleService;
    private final BizAppService bizAppService;
    private final PlatformPermissionService platformPermissionService;
    private final PlatformAppService platformAppService;
    private final PlatformRoleService platformRoleService;
    private final TenantRolePrivateService tenantRolePrivateService;
    private final TenantRoleUserPrivateService tenantRoleUserPrivateService;
    private final TenantProperties tenantProperties;

    /**
     * 在指定租户上下文解析用户有效授权。
     *
     * @param tenantId 租户 ID
     * @param userId   用户 ID
     * @return 有效授权，不含身份时返回空授予
     */
    public EffectiveAuthorization resolve(long tenantId, long userId) {
        return TenantEnv.applyAs(tenantId, () -> resolveInCurrentTenant(userId));
    }

    /**
     * 在当前租户上下文解析用户有效授权。
     *
     * @param userId 用户 ID
     * @return 有效授权
     */
    public EffectiveAuthorization resolve(long userId) {
        return resolveInCurrentTenant(userId);
    }

    /**
     * 仅按角色编码解析，不保留部门绑定。仅用于无身份预览，正式判定应走 {@link #resolve(long, long)}。
     *
     * @param roleCodes 角色编码
     * @return 有效授权
     */
    public EffectiveAuthorization resolve(List<String> roleCodes) {
        List<RoleType> roles = bizRoleService.getRolesByCodes(roleCodes);
        List<RoleBinding> bindings = CollUtil.emptyIfNull(roles).stream()
                .filter(role -> role.getStatus() == CommonStatusEnum.ENABLE)
                .map(role -> RoleBinding.builder()
                        .roleId(role.getId())
                        .platformRole(role.getPlatformRole())
                        .roleCode(role.getCode())
                        .deptId(null)
                        .filterDept(BooleanUtil.isTrue(role.getFilterDept()))
                        .build())
                .toList();
        return build(bindings, uniqueRoles(bindings));
    }

    private EffectiveAuthorization resolveInCurrentTenant(long userId) {
        List<TenantRoleUserPrivate> rows = tenantRoleUserPrivateService.getUserRoles(userId);
        List<RoleBinding> bindings = new ArrayList<>();
        for (TenantRoleUserPrivate row : CollUtil.emptyIfNull(rows)) {
            RoleType role = loadEnabledRole(row);
            if (role == null) {
                continue;
            }
            bindings.add(RoleBinding.builder()
                    .roleId(role.getId())
                    .platformRole(role.getPlatformRole())
                    .roleCode(role.getCode())
                    .deptId(row.getDeptId())
                    .filterDept(BooleanUtil.isTrue(role.getFilterDept()))
                    .build());
        }
        return build(bindings, uniqueRoles(bindings));
    }

    private EffectiveAuthorization build(List<RoleBinding> bindings, List<RoleType> roles) {
        Set<String> exactCodes = new LinkedHashSet<>();
        Set<String> wildcardCodes = new LinkedHashSet<>();
        boolean orgAdmin = bindings.stream()
                .anyMatch(binding -> RoleConstants.ROLE_ORG_ADMIN_CODE.equals(binding.getRoleCode()));

        if (CollUtil.isNotEmpty(roles)) {
            List<PermissionType> boundPermissions = bizRoleService.getRolesPermissions(roles);
            for (PermissionType permission : boundPermissions) {
                appendGrant(permission.getCode(), exactCodes, wildcardCodes);
            }
        }

        Set<Long> accessibleAppIds = resolveAccessibleAppIds();
        if (orgAdmin) {
            bizAppService.getEnabledApps().stream()
                    .filter(app -> app.getAppType() == null || app.getAppType() == OrgTypeEnum.Tenant)
                    .map(PlatformApp::getCode)
                    .filter(StrUtil::isNotBlank)
                    .map(code -> code + PermissionMatcher.ANT_SUBTREE_SUFFIX)
                    .forEach(code -> appendGrant(code, exactCodes, wildcardCodes));
        }

        List<PlatformPermission> enabledPermissions = platformPermissionService.list().stream()
                .filter(item -> item.getStatus() == CommonStatusEnum.ENABLE)
                .toList();

        exactCodes.removeIf(code -> !isPermissionAccessible(code, enabledPermissions, accessibleAppIds));
        wildcardCodes.removeIf(code -> !isPermissionAccessible(code, enabledPermissions, accessibleAppIds));

        Set<String> concreteCodes = new LinkedHashSet<>();
        for (PlatformPermission permission : enabledPermissions) {
            if (PermissionMatcher.isWildcard(permission.getCode())) {
                continue;
            }
            if (!isPermissionAccessible(permission.getCode(), enabledPermissions, accessibleAppIds)) {
                continue;
            }
            if (hasGrant(exactCodes, wildcardCodes, permission.getCode())) {
                concreteCodes.add(permission.getCode());
            }
        }

        return EffectiveAuthorization.builder()
                .roleBindings(List.copyOf(bindings))
                .exactPermissionCodes(exactCodes)
                .wildcardPermissionCodes(wildcardCodes)
                .concretePermissionCodes(concreteCodes)
                .accessibleAppIds(accessibleAppIds)
                .orgAdmin(orgAdmin)
                .build();
    }

    private RoleType loadEnabledRole(TenantRoleUserPrivate row) {
        RoleType role = BooleanUtil.isTrue(row.getPlatformRole())
                ? platformRoleService.getById(row.getRoleId())
                : tenantRolePrivateService.getById(row.getRoleId());
        if (role == null || role.getStatus() != CommonStatusEnum.ENABLE) {
            return null;
        }
        return role;
    }

    private List<RoleType> uniqueRoles(List<RoleBinding> bindings) {
        Map<Long, RoleType> unique = new LinkedHashMap<>();
        for (RoleBinding binding : bindings) {
            if (unique.containsKey(binding.getRoleId())) {
                continue;
            }
            RoleType role = binding.isPlatformRole()
                    ? platformRoleService.getById(binding.getRoleId())
                    : tenantRolePrivateService.getById(binding.getRoleId());
            if (role != null) {
                unique.put(binding.getRoleId(), role);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private Set<Long> resolveAccessibleAppIds() {
        Long tenantId = TenantContextHolder.get();
        if (Objects.equals(tenantId, tenantProperties.getDefaultId())) {
            return platformAppService.list().stream()
                    .filter(app -> app.getStatus() == CommonStatusEnum.ENABLE)
                    .map(PlatformApp::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return bizAppService.getEnabledApps().stream()
                .filter(app -> app.getAppType() == null || app.getAppType() == OrgTypeEnum.Tenant)
                .map(PlatformApp::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isPermissionAccessible(String code,
                                           List<PlatformPermission> permissions,
                                           Set<Long> accessibleAppIds) {
        PlatformPermission permission = permissions.stream()
                .filter(item -> Objects.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
        if (permission != null) {
            if (permission.getAppId() == null) {
                return true;
            }
            return accessibleAppIds.contains(permission.getAppId());
        }
        if (!PermissionMatcher.isWildcard(code)) {
            return false;
        }
        String appCode = PermissionMatcher.wildcardPathPrefix(code);
        return platformAppService.list().stream()
                .anyMatch(app -> accessibleAppIds.contains(app.getId())
                        && appCode.equals(app.getCode()));
    }

    private static boolean hasGrant(Set<String> exactCodes, Set<String> wildcardCodes, String code) {
        if (exactCodes.contains(code)) {
            return true;
        }
        for (String wildcard : wildcardCodes) {
            if (PermissionMatcher.matches(wildcard, code)) {
                return true;
            }
        }
        return false;
    }

    private static void appendGrant(String code, Set<String> exactCodes, Set<String> wildcardCodes) {
        if (StrUtil.isBlank(code)) {
            return;
        }
        if (PermissionMatcher.isWildcard(code)) {
            wildcardCodes.add(code);
        } else {
            exactCodes.add(code);
        }
    }
}
