package com.ingot.cloud.pms.audit;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.PlatformApp;
import com.ingot.cloud.pms.api.model.domain.PlatformMenu;
import com.ingot.cloud.pms.api.model.domain.PlatformPermission;
import com.ingot.cloud.pms.api.model.domain.PlatformRole;
import com.ingot.cloud.pms.api.model.domain.TenantRolePermissionPrivate;
import com.ingot.cloud.pms.api.model.domain.TenantRolePrivate;
import com.ingot.cloud.pms.api.model.domain.TenantRoleUserPrivate;
import com.ingot.cloud.pms.api.model.enums.AuthorizationAuditCategoryEnum;
import com.ingot.cloud.pms.api.model.enums.PermissionSourceTypeEnum;
import com.ingot.cloud.pms.api.model.vo.authorization.AuthorizationAuditIssueVO;
import com.ingot.cloud.pms.api.model.vo.authorization.AuthorizationAuditReportVO;
import com.ingot.cloud.pms.service.domain.PlatformAppService;
import com.ingot.cloud.pms.service.domain.PlatformMenuService;
import com.ingot.cloud.pms.service.domain.PlatformPermissionService;
import com.ingot.cloud.pms.service.domain.PlatformRolePermissionService;
import com.ingot.cloud.pms.service.domain.PlatformRoleService;
import com.ingot.cloud.pms.service.domain.TenantRolePermissionPrivateService;
import com.ingot.cloud.pms.service.domain.TenantRolePrivateService;
import com.ingot.cloud.pms.service.domain.TenantRoleUserPrivateService;
import com.ingot.framework.commons.constants.IDConstants;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>授权相关数据只读审计服务，扫描菜单/权限/应用/角色关系的完整性缺陷。</p>
 *
 * <p>仅产出问题报告，不修改任何业务数据。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AuthorizationDataAuditService {

    private final PlatformMenuService platformMenuService;
    private final PlatformPermissionService platformPermissionService;
    private final PlatformAppService platformAppService;
    private final PlatformRoleService platformRoleService;
    private final PlatformRolePermissionService platformRolePermissionService;
    private final TenantRolePermissionPrivateService tenantRolePermissionPrivateService;
    private final TenantRolePrivateService tenantRolePrivateService;
    private final TenantRoleUserPrivateService tenantRoleUserPrivateService;

    public AuthorizationAuditReportVO audit(Long tenantId) {
        List<AuthorizationAuditIssueVO> issues = new ArrayList<>();
        issues.addAll(auditPlatformResources());
        if (tenantId != null) {
            issues.addAll(TenantEnv.applyAs(tenantId, () -> auditTenantPrivateRelations(tenantId)));
        } else {
            issues.addAll(TenantEnv.globalApply(this::auditTenantPrivateRelationsGlobal));
        }
        Map<AuthorizationAuditCategoryEnum, Integer> counts = new EnumMap<>(AuthorizationAuditCategoryEnum.class);
        for (AuthorizationAuditIssueVO issue : issues) {
            counts.merge(issue.getCategory(), 1, Integer::sum);
        }
        return AuthorizationAuditReportVO.builder()
                .totalIssues(issues.size())
                .countsByCategory(counts)
                .issues(issues)
                .build();
    }

    private List<AuthorizationAuditIssueVO> auditPlatformResources() {
        List<AuthorizationAuditIssueVO> issues = new ArrayList<>();
        List<PlatformMenu> menus = platformMenuService.list();
        List<PlatformPermission> permissions = platformPermissionService.list();
        List<PlatformApp> apps = platformAppService.list();

        Map<Long, PlatformMenu> menuById = menus.stream()
                .collect(Collectors.toMap(PlatformMenu::getId, item -> item, (a, b) -> a));
        Map<Long, PlatformPermission> permissionById = permissions.stream()
                .collect(Collectors.toMap(PlatformPermission::getId, item -> item, (a, b) -> a));

        for (PlatformMenu menu : menus) {
            Long pid = menu.getPid();
            if (pid != null && pid > IDConstants.ROOT_TREE_ID && !menuById.containsKey(pid)) {
                issues.add(issue(AuthorizationAuditCategoryEnum.INVALID_MENU_PARENT,
                        "platform_menu", menu.getId(),
                        "菜单父节点不存在: pid=" + pid,
                        "修复菜单父级或恢复缺失父菜单"));
            }
            if (menu.getPermissionId() == null || menu.getPermissionId() <= 0) {
                issues.add(issue(AuthorizationAuditCategoryEnum.MENU_PERMISSION_MISMATCH,
                        "platform_menu", menu.getId(),
                        "菜单未关联权限",
                        "为菜单创建或绑定托管权限"));
                continue;
            }
            PlatformPermission permission = permissionById.get(menu.getPermissionId());
            if (permission == null) {
                issues.add(issue(AuthorizationAuditCategoryEnum.MENU_PERMISSION_MISMATCH,
                        "platform_menu", menu.getId(),
                        "菜单关联权限不存在: permissionId=" + menu.getPermissionId(),
                        "修复 permission_id 或恢复缺失权限"));
                continue;
            }
            if (pid != null && pid > IDConstants.ROOT_TREE_ID) {
                PlatformMenu parentMenu = menuById.get(pid);
                if (parentMenu != null && parentMenu.getPermissionId() != null && permission.getPid() != null
                        && !Objects.equals(parentMenu.getPermissionId(), permission.getPid())) {
                    issues.add(issue(AuthorizationAuditCategoryEnum.MENU_PERMISSION_PARENT_MISMATCH,
                            "platform_menu", menu.getId(),
                            "菜单权限父级与菜单父级不一致",
                            "同步菜单权限 pid 与父菜单 permission_id"));
                }
            }
        }

        Map<String, List<PlatformPermission>> permissionsByCode = permissions.stream()
                .collect(Collectors.groupingBy(PlatformPermission::getCode));
        permissionsByCode.forEach((code, items) -> {
            if (items.size() > 1) {
                issues.add(issue(AuthorizationAuditCategoryEnum.DUPLICATE_PERMISSION_CODE,
                        "platform_permission", items.get(0).getId(),
                        "权限编码重复: code=" + code + ", count=" + items.size(),
                        "合并重复权限并修正角色/菜单引用"));
            }
        });

        platformRolePermissionService.list().forEach(item -> {
            if (!permissionById.containsKey(item.getPermissionId())) {
                issues.add(issue(AuthorizationAuditCategoryEnum.ROLE_PERMISSION_ORPHAN,
                        "platform_role_permission", item.getId(),
                        "平台角色权限引用不存在: permissionId=" + item.getPermissionId(),
                        "清理无效 platform_role_permission 记录"));
            }
        });

        Set<Long> appIdsWithSystemRoot = permissions.stream()
                .filter(item -> item.getAppId() != null
                        && item.getSourceType() == PermissionSourceTypeEnum.SYSTEM
                        && (item.getPid() == null || item.getPid() <= IDConstants.ROOT_TREE_ID))
                .map(PlatformPermission::getAppId)
                .collect(Collectors.toSet());
        for (PlatformApp app : apps) {
            if (!appIdsWithSystemRoot.contains(app.getId())) {
                issues.add(issue(AuthorizationAuditCategoryEnum.APP_ROOT_REFERENCE_MISSING,
                        "platform_app", app.getId(),
                        "应用缺少系统根权限: appId=" + app.getId(),
                        "确认应用根权限 app_id/source_type/pid 是否正确"));
            }
        }

        return issues;
    }

    private List<AuthorizationAuditIssueVO> auditTenantPrivateRelationsGlobal() {
        List<AuthorizationAuditIssueVO> issues = new ArrayList<>();
        Set<Long> platformRoleIds = platformRoleService.list().stream()
                .map(PlatformRole::getId)
                .collect(Collectors.toSet());
        Set<Long> tenantRoleIds = tenantRolePrivateService.list().stream()
                .map(TenantRolePrivate::getId)
                .collect(Collectors.toSet());
        Set<Long> permissionIds = platformPermissionService.list().stream()
                .map(PlatformPermission::getId)
                .collect(Collectors.toSet());

        for (TenantRolePermissionPrivate relation : tenantRolePermissionPrivateService.list()) {
            issues.addAll(validateTenantPrivateRelation(relation.getTenantId(), relation.getRoleId(),
                    relation.getPlatformRole(), relation.getPermissionId(),
                    platformRoleIds, tenantRoleIds, permissionIds,
                    "tenant_role_permission_private", relation.getId()));
        }
        for (TenantRoleUserPrivate relation : tenantRoleUserPrivateService.list()) {
            if (relation.getTenantId() == null) {
                issues.add(issue(AuthorizationAuditCategoryEnum.TENANT_PRIVATE_RELATION_INCOMPLETE,
                        "tenant_role_user_private", relation.getId(),
                        "租户角色用户关系缺少 tenantId",
                        "补全 tenant_id"));
            }
            if (relation.getRoleId() == null) {
                issues.add(issue(AuthorizationAuditCategoryEnum.TENANT_PRIVATE_RELATION_INCOMPLETE,
                        "tenant_role_user_private", relation.getId(),
                        "租户角色用户关系缺少 roleId",
                        "补全 role_id"));
            }
            if (relation.getUserId() == null) {
                issues.add(issue(AuthorizationAuditCategoryEnum.TENANT_PRIVATE_RELATION_INCOMPLETE,
                        "tenant_role_user_private", relation.getId(),
                        "租户角色用户关系缺少 userId",
                        "补全 user_id"));
            }
            if (relation.getPlatformRole() != null) {
                boolean platform = Boolean.TRUE.equals(relation.getPlatformRole());
                if (platform && !platformRoleIds.contains(relation.getRoleId())) {
                    issues.add(issue(AuthorizationAuditCategoryEnum.ROLE_SOURCE_FLAG_MISMATCH,
                            "tenant_role_user_private", relation.getId(),
                            "platformRole=true 但 roleId 不在 platform_role",
                            "修正 platformRole 或 role_id"));
                }
                if (!platform && !tenantRoleIds.contains(relation.getRoleId())) {
                    issues.add(issue(AuthorizationAuditCategoryEnum.ROLE_SOURCE_FLAG_MISMATCH,
                            "tenant_role_user_private", relation.getId(),
                            "platformRole=false 但 roleId 不在 tenant_role_private",
                            "修正 platformRole 或 role_id"));
                }
            }
        }
        return issues;
    }

    private List<AuthorizationAuditIssueVO> auditTenantPrivateRelations(Long tenantId) {
        List<AuthorizationAuditIssueVO> issues = new ArrayList<>();
        Set<Long> platformRoleIds = platformRoleService.list().stream()
                .map(PlatformRole::getId)
                .collect(Collectors.toSet());
        Set<Long> tenantRoleIds = tenantRolePrivateService.list().stream()
                .map(TenantRolePrivate::getId)
                .collect(Collectors.toSet());
        Set<Long> permissionIds = platformPermissionService.list().stream()
                .map(PlatformPermission::getId)
                .collect(Collectors.toSet());

        tenantRolePermissionPrivateService.list(Wrappers.<TenantRolePermissionPrivate>lambdaQuery()
                        .eq(TenantRolePermissionPrivate::getTenantId, tenantId))
                .forEach(relation -> issues.addAll(validateTenantPrivateRelation(
                        relation.getTenantId(), relation.getRoleId(), relation.getPlatformRole(),
                        relation.getPermissionId(), platformRoleIds, tenantRoleIds, permissionIds,
                        "tenant_role_permission_private", relation.getId())));
        return issues;
    }

    private List<AuthorizationAuditIssueVO> validateTenantPrivateRelation(Long tenantId,
                                                                          Long roleId,
                                                                          Boolean platformRole,
                                                                          Long permissionId,
                                                                          Set<Long> platformRoleIds,
                                                                          Set<Long> tenantRoleIds,
                                                                          Set<Long> permissionIds,
                                                                          String entityType,
                                                                          Long entityId) {
        List<AuthorizationAuditIssueVO> issues = new ArrayList<>();
        if (tenantId == null) {
            issues.add(issue(AuthorizationAuditCategoryEnum.TENANT_PRIVATE_RELATION_INCOMPLETE,
                    entityType, entityId, "缺少 tenantId", "补全 tenant_id"));
        }
        if (roleId == null) {
            issues.add(issue(AuthorizationAuditCategoryEnum.TENANT_PRIVATE_RELATION_INCOMPLETE,
                    entityType, entityId, "缺少 roleId", "补全 role_id"));
        }
        if (permissionId == null) {
            issues.add(issue(AuthorizationAuditCategoryEnum.TENANT_PRIVATE_RELATION_INCOMPLETE,
                    entityType, entityId, "缺少 permissionId", "补全 permission_id"));
        } else if (!permissionIds.contains(permissionId)) {
            issues.add(issue(AuthorizationAuditCategoryEnum.ROLE_PERMISSION_ORPHAN,
                    entityType, entityId,
                    "租户私有权限引用不存在: permissionId=" + permissionId,
                    "清理无效 tenant_role_permission_private 记录"));
        }
        if (platformRole != null && roleId != null) {
            boolean platform = Boolean.TRUE.equals(platformRole);
            if (platform && !platformRoleIds.contains(roleId)) {
                issues.add(issue(AuthorizationAuditCategoryEnum.ROLE_SOURCE_FLAG_MISMATCH,
                        entityType, entityId,
                        "platformRole=true 但 roleId 不在 platform_role",
                        "修正 platformRole 或 role_id"));
            }
            if (!platform && !tenantRoleIds.contains(roleId)) {
                issues.add(issue(AuthorizationAuditCategoryEnum.ROLE_SOURCE_FLAG_MISMATCH,
                        entityType, entityId,
                        "platformRole=false 但 roleId 不在 tenant_role_private",
                        "修正 platformRole 或 role_id"));
            }
        }
        return issues;
    }

    private AuthorizationAuditIssueVO issue(AuthorizationAuditCategoryEnum category,
                                            String entityType,
                                            Long entityId,
                                            String message,
                                            String suggestion) {
        return AuthorizationAuditIssueVO.builder()
                .category(category)
                .entityType(entityType)
                .entityId(entityId)
                .message(message)
                .suggestion(suggestion)
                .build();
    }
}
