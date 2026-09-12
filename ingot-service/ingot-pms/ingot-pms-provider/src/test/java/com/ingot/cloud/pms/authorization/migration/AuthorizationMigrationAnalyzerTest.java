package com.ingot.cloud.pms.authorization.migration;

import java.util.List;
import java.util.Map;

import com.ingot.cloud.pms.api.model.domain.PlatformApp;
import com.ingot.cloud.pms.api.model.domain.PlatformMenu;
import com.ingot.cloud.pms.api.model.domain.PlatformPermission;
import com.ingot.cloud.pms.api.model.domain.TenantDept;
import com.ingot.cloud.pms.api.model.domain.TenantRoleUserPrivate;
import com.ingot.cloud.pms.api.model.enums.AccessModeEnum;
import com.ingot.cloud.pms.api.model.enums.MenuTypeEnum;
import com.ingot.cloud.pms.api.model.enums.MigrationBlockReasonEnum;
import com.ingot.cloud.pms.api.model.enums.PermissionNodeTypeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationMigrationAnalyzerTest {

    @Test
    void sameRoleDifferentDeptIsNotDuplicate() {
        TenantRoleUserPrivate a = binding(1L, 10L, 100L, 1L);
        TenantRoleUserPrivate b = binding(2L, 10L, 100L, 2L);
        MigrationReport report = AuthorizationMigrationAnalyzer.analyze(snapshot(
                List.of(), List.of(), List.of(), List.of(a, b), List.of(dept(1L, 1L), dept(2L, 1L)),
                Map.of()));
        assertTrue(report.canApply());
        assertTrue(report.blocking().stream().noneMatch(item ->
                item.reason() == MigrationBlockReasonEnum.DUPLICATE_DEPT_BINDING));
    }

    @Test
    void sameRoleSameNullDeptIsDuplicate() {
        TenantRoleUserPrivate a = binding(1L, 10L, 100L, null);
        TenantRoleUserPrivate b = binding(2L, 10L, 100L, null);
        MigrationReport report = AuthorizationMigrationAnalyzer.analyze(snapshot(
                List.of(), List.of(), List.of(), List.of(a, b), List.of(), Map.of()));
        assertFalse(report.canApply());
        assertEquals(MigrationBlockReasonEnum.DUPLICATE_DEPT_BINDING, report.blocking().getFirst().reason());
    }

    @Test
    void exactActionPageLinksMenu() {
        PlatformApp app = app(1L, "demo", 9L);
        PlatformMenu menu = menu(11L, 1L, MenuTypeEnum.Menu, AccessModeEnum.PERMISSION);
        PlatformPermission permission = permission(21L, 1L, "demo:home", PermissionNodeTypeEnum.ACTION);
        MigrationReport report = AuthorizationMigrationAnalyzer.analyze(snapshot(
                List.of(app), List.of(menu), List.of(permission), List.of(), List.of(),
                Map.of(11L, 21L)));
        assertTrue(report.canApply());
        assertTrue(report.changes().stream().anyMatch(change ->
                MigrationMappedChange.MENU_PERMISSION_LINK.equals(change.type())));
    }

    @Test
    void wildcardPageReusesQueryAction() {
        PlatformApp app = app(1L, "org:contacts", 9L);
        PlatformMenu menu = menu(11L, 1L, MenuTypeEnum.Menu, AccessModeEnum.PERMISSION);
        PlatformPermission wildcard = permission(21L, 1L, "org:contacts:user:**", PermissionNodeTypeEnum.GROUP);
        PlatformPermission query = permission(22L, 1L, "org:contacts:user:query", PermissionNodeTypeEnum.ACTION);
        MigrationReport report = AuthorizationMigrationAnalyzer.analyze(snapshot(
                List.of(app), List.of(menu), List.of(wildcard, query), List.of(), List.of(),
                Map.of(11L, 21L)));
        assertTrue(report.canApply());
        assertTrue(report.changes().stream().anyMatch(change ->
                "org:contacts:user:query".equals(change.detail())
                        && MigrationMappedChange.MENU_PERMISSION_LINK.equals(change.type())));
        assertTrue(report.changes().stream().noneMatch(change ->
                MigrationMappedChange.CREATE_VIEW_PERMISSION.equals(change.type())));
    }

    @Test
    void wildcardPageCreatesViewUnlessCodeCollides() {
        PlatformApp app = app(1L, "org:contacts", 9L);
        PlatformMenu menu = menu(11L, 1L, MenuTypeEnum.Menu, AccessModeEnum.PERMISSION);
        PlatformPermission wildcard = permission(21L, 1L, "org:contacts:user:**", PermissionNodeTypeEnum.GROUP);
        MigrationReport ok = AuthorizationMigrationAnalyzer.analyze(snapshot(
                List.of(app), List.of(menu), List.of(wildcard), List.of(), List.of(),
                Map.of(11L, 21L)));
        assertTrue(ok.canApply());
        assertTrue(ok.changes().stream().anyMatch(change ->
                "org:contacts:user:view".equals(change.detail())
                        && MigrationMappedChange.CREATE_VIEW_PERMISSION.equals(change.type())));

        PlatformPermission collision = permission(22L, 1L, "org:contacts:user:view", PermissionNodeTypeEnum.GROUP);
        MigrationReport blocked = AuthorizationMigrationAnalyzer.analyze(snapshot(
                List.of(app), List.of(menu), List.of(wildcard, collision), List.of(), List.of(),
                Map.of(11L, 21L)));
        assertFalse(blocked.canApply());
        assertEquals(MigrationBlockReasonEnum.VIEW_CODE_COLLISION, blocked.blocking().getFirst().reason());
    }

    @Test
    void buttonMenuIsRemovedAndPermissionKept() {
        PlatformApp app = app(1L, "org:contacts", 9L);
        PlatformMenu button = menu(12L, 1L, MenuTypeEnum.Button, AccessModeEnum.PERMISSION);
        PlatformPermission permission = permission(22L, 1L, "org:contacts:user:query", PermissionNodeTypeEnum.ACTION);
        MigrationReport report = AuthorizationMigrationAnalyzer.analyze(snapshot(
                List.of(app), List.of(button), List.of(permission), List.of(), List.of(),
                Map.of(12L, 22L)));
        assertTrue(report.canApply());
        assertTrue(report.changes().stream().anyMatch(change ->
                MigrationMappedChange.REMOVE_BUTTON_MENU.equals(change.type())));
        assertTrue(report.changes().stream().noneMatch(change ->
                MigrationMappedChange.MENU_PERMISSION_LINK.equals(change.type())));
    }

    private static AuthorizationMigrationSnapshot snapshot(List<PlatformApp> apps,
                                                           List<PlatformMenu> menus,
                                                           List<PlatformPermission> permissions,
                                                           List<TenantRoleUserPrivate> roleUsers,
                                                           List<TenantDept> depts,
                                                           Map<Long, Long> legacy) {
        return new AuthorizationMigrationSnapshot(
                apps, menus, permissions, roleUsers, depts, List.of(), List.of(), legacy);
    }

    private static PlatformApp app(long id, String code, long permissionId) {
        PlatformApp app = new PlatformApp();
        app.setId(id);
        app.setCode(code);
        app.setPermissionId(permissionId);
        return app;
    }

    private static PlatformMenu menu(long id, long appId, MenuTypeEnum type, AccessModeEnum accessMode) {
        PlatformMenu menu = new PlatformMenu();
        menu.setId(id);
        menu.setAppId(appId);
        menu.setMenuType(type);
        menu.setAccessMode(accessMode);
        return menu;
    }

    private static PlatformPermission permission(long id, long appId, String code, PermissionNodeTypeEnum nodeType) {
        PlatformPermission permission = new PlatformPermission();
        permission.setId(id);
        permission.setAppId(appId);
        permission.setCode(code);
        permission.setNodeType(nodeType);
        return permission;
    }

    private static TenantRoleUserPrivate binding(long id, long userId, long roleId, Long deptId) {
        TenantRoleUserPrivate row = new TenantRoleUserPrivate();
        row.setId(id);
        row.setTenantId(1L);
        row.setUserId(userId);
        row.setRoleId(roleId);
        row.setPlatformRole(true);
        row.setDeptId(deptId);
        return row;
    }

    private static TenantDept dept(long id, long tenantId) {
        TenantDept dept = new TenantDept();
        dept.setId(id);
        dept.setTenantId(tenantId);
        return dept;
    }
}
