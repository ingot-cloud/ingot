package com.ingot.cloud.pms.authorization.migration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.pms.api.model.constants.DeptBindingConstants;
import com.ingot.cloud.pms.api.model.domain.PlatformApp;
import com.ingot.cloud.pms.api.model.domain.PlatformMenu;
import com.ingot.cloud.pms.api.model.domain.PlatformPermission;
import com.ingot.cloud.pms.api.model.domain.TenantDept;
import com.ingot.cloud.pms.api.model.domain.TenantRoleUserPrivate;
import com.ingot.cloud.pms.api.model.enums.AccessModeEnum;
import com.ingot.cloud.pms.api.model.enums.MenuTypeEnum;
import com.ingot.cloud.pms.api.model.enums.MigrationBlockReasonEnum;
import com.ingot.cloud.pms.api.model.enums.PermissionNodeTypeEnum;
import com.ingot.cloud.pms.authorization.engine.PermissionMatcher;

/**
 * <p>按 DESIGN 映射规则分析授权迁移，只报告阻断与批准变化，不猜测修复。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class AuthorizationMigrationAnalyzer {

    /** 页面入口优先复用的查询权限后缀。 */
    public static final String QUERY_SUFFIX = ":query";

    /** 无 query 可复用时的页面入口后缀。 */
    public static final String VIEW_SUFFIX = ":view";

    private AuthorizationMigrationAnalyzer() {
    }

    /**
     * 分析快照并产出报告。存在阻断时 {@link MigrationReport#canApply()} 为假。
     *
     * @param snapshot 未删除的在线数据
     * @return 阻断清单与批准映射
     */
    public static MigrationReport analyze(AuthorizationMigrationSnapshot snapshot) {
        List<MigrationBlockingIssue> blocking = new ArrayList<>();
        List<MigrationMappedChange> changes = new ArrayList<>();

        Map<Long, PlatformPermission> permissions = indexPermissions(snapshot.permissions());
        Map<Long, TenantDept> depts = indexDepts(snapshot.depts());
        Set<String> permissionCodes = snapshot.permissions().stream()
                .map(PlatformPermission::getCode)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(HashSet::new));

        Map<Long, Long> legacyPermissionIds = snapshot.menuLegacyPermissionIds() == null
                ? Map.of()
                : snapshot.menuLegacyPermissionIds();

        collectDuplicateBindings(snapshot.roleUsers(), blocking);
        collectCrossTenantDept(snapshot.roleUsers(), depts, blocking);
        collectMissingAppId(snapshot.menus(), snapshot.permissions(), blocking);
        collectUnknownSource(snapshot.menus(), snapshot.permissions(), legacyPermissionIds, blocking);
        collectWildcardCollisions(snapshot.permissions(), permissionCodes, blocking, changes);
        collectMenuMappings(snapshot.menus(), permissions, permissionCodes, legacyPermissionIds, blocking, changes);
        collectAppDefaultOpen(snapshot.apps(), changes);

        return new MigrationReport(List.copyOf(blocking), List.copyOf(changes));
    }

    private static void collectDuplicateBindings(List<TenantRoleUserPrivate> roleUsers,
                                                 List<MigrationBlockingIssue> blocking) {
        Map<String, Integer> counts = new HashMap<>();
        for (TenantRoleUserPrivate row : CollUtil.emptyIfNull(roleUsers)) {
            String key = row.getTenantId() + ":" + row.getUserId() + ":"
                    + Boolean.TRUE.equals(row.getPlatformRole()) + ":"
                    + row.getRoleId() + ":" + DeptBindingConstants.normalizeDeptId(row.getDeptId());
            counts.merge(key, 1, Integer::sum);
        }
        counts.forEach((key, count) -> {
            if (count > 1) {
                blocking.add(new MigrationBlockingIssue(
                        MigrationBlockReasonEnum.DUPLICATE_DEPT_BINDING,
                        key,
                        "重复任职 " + count + " 行"));
            }
        });
    }

    private static void collectCrossTenantDept(List<TenantRoleUserPrivate> roleUsers,
                                               Map<Long, TenantDept> depts,
                                               List<MigrationBlockingIssue> blocking) {
        for (TenantRoleUserPrivate row : CollUtil.emptyIfNull(roleUsers)) {
            if (row.getDeptId() == null) {
                continue;
            }
            TenantDept dept = depts.get(row.getDeptId());
            if (dept == null) {
                blocking.add(new MigrationBlockingIssue(
                        MigrationBlockReasonEnum.DANGLING_RELATION,
                        String.valueOf(row.getId()),
                        "任职部门不存在: " + row.getDeptId()));
                continue;
            }
            if (!Objects.equals(dept.getTenantId(), row.getTenantId())) {
                blocking.add(new MigrationBlockingIssue(
                        MigrationBlockReasonEnum.CROSS_TENANT_DEPT,
                        String.valueOf(row.getId()),
                        "部门 " + row.getDeptId() + " 不属于租户 " + row.getTenantId()));
            }
        }
    }

    private static void collectMissingAppId(List<PlatformMenu> menus,
                                            List<PlatformPermission> permissions,
                                            List<MigrationBlockingIssue> blocking) {
        for (PlatformMenu menu : CollUtil.emptyIfNull(menus)) {
            if (menu.getAppId() == null) {
                blocking.add(new MigrationBlockingIssue(
                        MigrationBlockReasonEnum.MISSING_APP_ID,
                        "menu:" + menu.getId(),
                        "菜单缺少 app_id"));
            }
        }
        for (PlatformPermission permission : CollUtil.emptyIfNull(permissions)) {
            if (permission.getAppId() == null) {
                blocking.add(new MigrationBlockingIssue(
                        MigrationBlockReasonEnum.MISSING_APP_ID,
                        "permission:" + permission.getId(),
                        "权限缺少 app_id"));
            }
        }
    }

    private static void collectUnknownSource(List<PlatformMenu> menus,
                                             List<PlatformPermission> permissions,
                                             Map<Long, Long> legacyPermissionIds,
                                             List<MigrationBlockingIssue> blocking) {
        Map<Long, PlatformPermission> permissionMap = indexPermissions(permissions);
        for (PlatformMenu menu : CollUtil.emptyIfNull(menus)) {
            Long permissionId = legacyPermissionIds.get(menu.getId());
            if (permissionId == null || permissionId == 0L) {
                continue;
            }
            if (!permissionMap.containsKey(permissionId)) {
                blocking.add(new MigrationBlockingIssue(
                        MigrationBlockReasonEnum.UNKNOWN_SOURCE,
                        "menu:" + menu.getId(),
                        "菜单 permission_id 悬空: " + permissionId));
            }
        }
    }

    private static void collectWildcardCollisions(List<PlatformPermission> permissions,
                                                  Set<String> permissionCodes,
                                                  List<MigrationBlockingIssue> blocking,
                                                  List<MigrationMappedChange> changes) {
        for (PlatformPermission permission : CollUtil.emptyIfNull(permissions)) {
            if (!PermissionMatcher.isSingleWildcard(permission.getCode())) {
                continue;
            }
            String converted = PermissionMatcher.wildcardPathPrefix(permission.getCode())
                    + PermissionMatcher.ANT_SUBTREE_SUFFIX;
            if (permissionCodes.contains(converted) && !converted.equals(permission.getCode())) {
                blocking.add(new MigrationBlockingIssue(
                        MigrationBlockReasonEnum.WILDCARD_CODE_COLLISION,
                        "permission:" + permission.getId(),
                        permission.getCode() + " -> " + converted));
            } else {
                changes.add(new MigrationMappedChange(
                        MigrationMappedChange.WILDCARD_TO_ANT,
                        "permission:" + permission.getId(),
                        permission.getCode() + " -> " + converted));
            }
        }
    }

    private static void collectMenuMappings(List<PlatformMenu> menus,
                                            Map<Long, PlatformPermission> permissions,
                                            Set<String> permissionCodes,
                                            Map<Long, Long> legacyPermissionIds,
                                            List<MigrationBlockingIssue> blocking,
                                            List<MigrationMappedChange> changes) {
        Set<String> reservedViewCodes = new HashSet<>();
        Map<String, PlatformPermission> permissionByCode = permissions.values().stream()
                .filter(item -> StrUtil.isNotBlank(item.getCode()))
                .collect(Collectors.toMap(PlatformPermission::getCode, item -> item, (a, b) -> a));
        for (PlatformMenu menu : CollUtil.emptyIfNull(menus)) {
            PlatformPermission permission = permissions.get(legacyPermissionIds.get(menu.getId()));
            if (menu.getMenuType() == MenuTypeEnum.Button) {
                changes.add(new MigrationMappedChange(
                        MigrationMappedChange.REMOVE_BUTTON_MENU,
                        "menu:" + menu.getId(),
                        "按钮伪路由删除，权限保留为 ACTION"));
                continue;
            }
            if (menu.getMenuType() == MenuTypeEnum.Directory
                    || menu.getAccessMode() == AccessModeEnum.OPEN) {
                continue;
            }
            if (permission == null) {
                continue;
            }
            if (PermissionMatcher.isWildcard(permission.getCode())) {
                convertWildcardPage(menu, permission, permissionByCode, permissionCodes, reservedViewCodes, blocking, changes);
            } else {
                changes.add(new MigrationMappedChange(
                        MigrationMappedChange.MENU_PERMISSION_LINK,
                        "menu:" + menu.getId(),
                        String.valueOf(permission.getId())));
            }
        }
    }

    private static void convertWildcardPage(PlatformMenu menu,
                                            PlatformPermission permission,
                                            Map<String, PlatformPermission> permissionByCode,
                                            Set<String> permissionCodes,
                                            Set<String> reservedViewCodes,
                                            List<MigrationBlockingIssue> blocking,
                                            List<MigrationMappedChange> changes) {
        String namespace = PermissionMatcher.wildcardPathPrefix(permission.getCode());
        PlatformPermission query = permissionByCode.get(namespace + QUERY_SUFFIX);
        if (isReusableAction(query)) {
            changes.add(new MigrationMappedChange(
                    MigrationMappedChange.MENU_PERMISSION_LINK,
                    "menu:" + menu.getId(),
                    query.getCode()));
            return;
        }
        String viewCode = namespace + VIEW_SUFFIX;
        PlatformPermission view = permissionByCode.get(viewCode);
        if (isReusableAction(view)) {
            changes.add(new MigrationMappedChange(
                    MigrationMappedChange.MENU_PERMISSION_LINK,
                    "menu:" + menu.getId(),
                    view.getCode()));
            return;
        }
        if (permissionCodes.contains(viewCode) || !reservedViewCodes.add(viewCode)) {
            blocking.add(new MigrationBlockingIssue(
                    MigrationBlockReasonEnum.VIEW_CODE_COLLISION,
                    "menu:" + menu.getId(),
                    viewCode));
            return;
        }
        changes.add(new MigrationMappedChange(
                MigrationMappedChange.CREATE_VIEW_PERMISSION,
                "menu:" + menu.getId(),
                viewCode));
        changes.add(new MigrationMappedChange(
                MigrationMappedChange.MENU_PERMISSION_LINK,
                "menu:" + menu.getId(),
                viewCode));
    }

    private static boolean isReusableAction(PlatformPermission permission) {
        return permission != null
                && permission.getNodeType() == PermissionNodeTypeEnum.ACTION
                && !PermissionMatcher.isWildcard(permission.getCode());
    }

    private static void collectAppDefaultOpen(List<PlatformApp> apps, List<MigrationMappedChange> changes) {
        for (PlatformApp app : CollUtil.emptyIfNull(apps)) {
            changes.add(new MigrationMappedChange(
                    MigrationMappedChange.APP_DEFAULT_OPEN,
                    "app:" + app.getId(),
                    app.getCode()));
        }
    }

    private static Map<Long, PlatformPermission> indexPermissions(List<PlatformPermission> permissions) {
        Map<Long, PlatformPermission> index = new HashMap<>();
        for (PlatformPermission permission : CollUtil.emptyIfNull(permissions)) {
            index.put(permission.getId(), permission);
        }
        return index;
    }

    private static Map<Long, TenantDept> indexDepts(List<TenantDept> depts) {
        Map<Long, TenantDept> index = new HashMap<>();
        for (TenantDept dept : CollUtil.emptyIfNull(depts)) {
            index.put(dept.getId(), dept);
        }
        return index;
    }
}
