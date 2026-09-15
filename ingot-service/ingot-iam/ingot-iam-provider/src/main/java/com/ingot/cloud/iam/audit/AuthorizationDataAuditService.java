package com.ingot.cloud.iam.audit;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.api.model.domain.PlatformApp;
import com.ingot.cloud.iam.api.model.domain.PlatformMenu;
import com.ingot.cloud.iam.api.model.domain.PlatformMenuPermission;
import com.ingot.cloud.iam.api.model.domain.PlatformPermission;
import com.ingot.cloud.iam.api.model.domain.PlatformRole;
import com.ingot.cloud.iam.api.model.domain.TenantRolePermissionPrivate;
import com.ingot.cloud.iam.api.model.domain.TenantRolePrivate;
import com.ingot.cloud.iam.api.model.domain.TenantRoleUserPrivate;
import com.ingot.cloud.iam.api.model.enums.AccessModeEnum;
import com.ingot.cloud.iam.api.model.enums.AuthorizationAuditCategoryEnum;
import com.ingot.cloud.iam.api.model.enums.MenuTypeEnum;
import com.ingot.cloud.iam.api.model.enums.PermissionNodeTypeEnum;
import com.ingot.cloud.iam.api.model.vo.authorization.AuthorizationAuditIssueVO;
import com.ingot.cloud.iam.api.model.vo.authorization.AuthorizationAuditReportVO;
import com.ingot.cloud.iam.service.domain.PlatformAppService;
import com.ingot.cloud.iam.service.domain.PlatformMenuPermissionService;
import com.ingot.cloud.iam.service.domain.PlatformMenuService;
import com.ingot.cloud.iam.service.domain.PlatformPermissionService;
import com.ingot.cloud.iam.service.domain.PlatformRolePermissionService;
import com.ingot.cloud.iam.service.domain.PlatformRoleService;
import com.ingot.cloud.iam.service.domain.TenantRolePermissionPrivateService;
import com.ingot.cloud.iam.service.domain.TenantRolePrivateService;
import com.ingot.cloud.iam.service.domain.TenantRoleUserPrivateService;
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
    private final PlatformMenuPermissionService platformMenuPermissionService;
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

        Map<Long, List<PlatformMenuPermission>> linksByMenu = platformMenuPermissionService.list().stream()
                .collect(Collectors.groupingBy(PlatformMenuPermission::getMenuId));

        for (PlatformMenu menu : menus) {
            Long pid = menu.getPid();
            if (pid != null && pid > IDConstants.ROOT_TREE_ID && !menuById.containsKey(pid)) {
                issues.add(issue(AuthorizationAuditCategoryEnum.INVALID_MENU_PARENT,
                        "platform_menu", menu.getId(),
                        "菜单父节点不存在: pid=" + pid,
                        "修复菜单父级或恢复缺失父菜单"));
            }
            if (menu.getMenuType() != MenuTypeEnum.Menu || menu.getAccessMode() != AccessModeEnum.PERMISSION) {
                continue;
            }
            List<PlatformMenuPermission> links = linksByMenu.getOrDefault(menu.getId(), List.of());
            if (links.isEmpty()) {
                issues.add(issue(AuthorizationAuditCategoryEnum.MENU_PERMISSION_MISMATCH,
                        "platform_menu", menu.getId(),
                        "受保护页面未关联具体权限",
                        "在 platform_menu_permission 绑定同应用 ACTION"));
                continue;
            }
            for (PlatformMenuPermission link : links) {
                PlatformPermission permission = permissionById.get(link.getPermissionId());
                if (permission == null) {
                    issues.add(issue(AuthorizationAuditCategoryEnum.MENU_PERMISSION_MISMATCH,
                            "platform_menu", menu.getId(),
                            "菜单关联权限不存在: permissionId=" + link.getPermissionId(),
                            "修复可见性关联或恢复缺失权限"));
                } else if (permission.getNodeType() != PermissionNodeTypeEnum.ACTION
                        || (permission.getCode() != null && permission.getCode().contains("*"))) {
                    issues.add(issue(AuthorizationAuditCategoryEnum.MENU_PERMISSION_MISMATCH,
                            "platform_menu", menu.getId(),
                            "可见性关联必须是非通配 ACTION: permissionId=" + link.getPermissionId(),
                            "改为关联具体操作权限"));
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

        for (PlatformApp app : apps) {
            PlatformPermission root = app.getPermissionId() == null
                    ? null
                    : permissionById.get(app.getPermissionId());
            if (root == null || root.getNodeType() != PermissionNodeTypeEnum.GROUP) {
                issues.add(issue(AuthorizationAuditCategoryEnum.APP_ROOT_REFERENCE_MISSING,
                        "platform_app", app.getId(),
                        "应用缺少根 GROUP 权限: appId=" + app.getId(),
                        "确认 platform_app.permission_id 指向同应用 GROUP"));
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
