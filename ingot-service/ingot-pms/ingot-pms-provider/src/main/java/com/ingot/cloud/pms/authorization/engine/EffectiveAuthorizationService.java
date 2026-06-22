package com.ingot.cloud.pms.authorization.engine;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.pms.api.model.domain.PlatformApp;
import com.ingot.cloud.pms.api.model.domain.PlatformPermission;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.types.PermissionType;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.service.biz.BizAppService;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.domain.PlatformAppService;
import com.ingot.cloud.pms.service.domain.PlatformPermissionService;
import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.tenant.TenantContextHolder;
import com.ingot.framework.tenant.properties.TenantProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>有效授权计算服务，根据角色绑定与可访问应用范围解析用户最终权限。</p>
 *
 * <p>处理组织管理员通配授权，并过滤禁用或未授权应用下的权限。</p>
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
    private final TenantProperties tenantProperties;

    public EffectiveAuthorization resolve(List<String> roleCodes) {
        List<RoleType> roles = bizRoleService.getRolesByCodes(roleCodes);
        Set<String> exactCodes = new LinkedHashSet<>();
        Set<String> wildcardCodes = new LinkedHashSet<>();

        if (CollUtil.isNotEmpty(roles)) {
            List<PermissionType> boundPermissions = bizRoleService.getRolesPermissions(roles);
            for (PermissionType permission : boundPermissions) {
                appendGrant(permission.getCode(), exactCodes, wildcardCodes);
            }
            if (containsOrgAdmin(roles)) {
                bizAppService.getEnabledApps().stream()
                        .map(PlatformApp::getCode)
                        .filter(StrUtil::isNotBlank)
                        .map(code -> code + PermissionMatcher.ANT_SUBTREE_SUFFIX)
                        .forEach(code -> appendGrant(code, exactCodes, wildcardCodes));
            }
        }

        List<PlatformPermission> allPermissions = platformPermissionService.list().stream()
                .filter(item -> item.getStatus() == CommonStatusEnum.ENABLE)
                .toList();
        Set<Long> accessibleAppIds = resolveAccessibleAppIds();

        exactCodes.removeIf(code -> !isPermissionAccessible(code, allPermissions, accessibleAppIds));
        wildcardCodes.removeIf(code -> !isPermissionAccessible(code, allPermissions, accessibleAppIds));

        return EffectiveAuthorization.builder()
                .exactPermissionCodes(exactCodes)
                .wildcardPermissionCodes(wildcardCodes)
                .accessibleAppIds(accessibleAppIds)
                .build();
    }

    private Set<Long> resolveAccessibleAppIds() {
        Long tenantId = TenantContextHolder.get();
        // 如果是平台，那么所有应用都可以访问
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
        if (permission == null && PermissionMatcher.isWildcard(code)) {
            String appCode = code.substring(0, code.length() - 2);
            return platformAppService.list().stream()
                    .anyMatch(app -> accessibleAppIds.contains(app.getId())
                            && appCode.equals(app.getCode()));
        }
        if (permission == null || permission.getAppId() == null) {
            return true;
        }
        return accessibleAppIds.contains(permission.getAppId());
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

    private static boolean containsOrgAdmin(List<RoleType> roles) {
        return roles.stream()
                .anyMatch(role -> RoleConstants.ROLE_ORG_ADMIN_CODE.equals(role.getCode()));
    }
}
