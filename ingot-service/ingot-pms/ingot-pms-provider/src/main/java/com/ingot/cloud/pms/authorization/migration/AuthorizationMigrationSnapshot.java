package com.ingot.cloud.pms.authorization.migration;

import java.util.List;
import java.util.Map;

import com.ingot.cloud.pms.api.model.domain.PlatformApp;
import com.ingot.cloud.pms.api.model.domain.PlatformMenu;
import com.ingot.cloud.pms.api.model.domain.PlatformPermission;
import com.ingot.cloud.pms.api.model.domain.PlatformRole;
import com.ingot.cloud.pms.api.model.domain.TenantDept;
import com.ingot.cloud.pms.api.model.domain.TenantRolePrivate;
import com.ingot.cloud.pms.api.model.domain.TenantRoleUserPrivate;

/**
 * <p>迁移分析输入快照，只包含未删除的在线行。</p>
 *
 * @param apps                    应用
 * @param menus                   菜单
 * @param permissions             权限
 * @param roleUsers               角色任职
 * @param depts                   部门
 * @param platformRoles           平台角色
 * @param tenantRoles             租户角色
 * @param menuLegacyPermissionIds 迁移前菜单旧单关联 permission_id，表列删除后仅分析器使用
 * @author jy
 * @since 1.0.0
 */
public record AuthorizationMigrationSnapshot(
        List<PlatformApp> apps,
        List<PlatformMenu> menus,
        List<PlatformPermission> permissions,
        List<TenantRoleUserPrivate> roleUsers,
        List<TenantDept> depts,
        List<PlatformRole> platformRoles,
        List<TenantRolePrivate> tenantRoles,
        Map<Long, Long> menuLegacyPermissionIds
) {
}
