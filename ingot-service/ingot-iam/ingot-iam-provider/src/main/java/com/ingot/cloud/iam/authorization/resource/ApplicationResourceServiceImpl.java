package com.ingot.cloud.iam.authorization.resource;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.api.model.convert.ApplicationConvert;
import com.ingot.cloud.iam.api.model.domain.*;
import com.ingot.cloud.iam.api.model.dto.application.*;
import com.ingot.cloud.iam.api.model.enums.*;
import com.ingot.cloud.iam.api.model.vo.application.AppDetailVO;
import com.ingot.cloud.iam.api.model.vo.application.AppPermissionTreeNodeVO;
import com.ingot.cloud.iam.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.iam.authorization.engine.PermissionMatcher;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.service.biz.BizRoleService;
import com.ingot.cloud.iam.service.domain.*;
import com.ingot.framework.commons.constants.IDConstants;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.utils.RoleUtil;
import com.ingot.framework.commons.utils.tree.TreeUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>{@link ApplicationResourceService} 默认实现，承载应用中心化资源的写入与查询逻辑。</p>
 *
 * <p>校验统一通过 {@code assertionChecker} 在服务层完成；对象映射委托 {@link ApplicationConvert}，
 * 更新仅覆盖请求中显式传入的非空字段。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationResourceServiceImpl implements ApplicationResourceService {
    private final PlatformAppService appService;
    private final PlatformMenuService menuService;
    private final PlatformPermissionService permissionService;
    private final PlatformRolePermissionService rolePermissionService;
    private final PlatformMenuPermissionService menuPermissionService;
    private final PlatformResourceService resourceService;
    private final PlatformRoleDataRuleService platformRoleDataRuleService;
    private final TenantRoleDataRulePrivateService tenantRoleDataRuleService;
    private final TenantRolePermissionPrivateService tenantRolePermissionService;
    private final TenantAppConfigService tenantAppConfigService;
    private final BizRoleService bizRoleService;
    private final AssertionChecker assertionChecker;
    private final ApplicationConvert applicationConvert;
    private final AuthorizationChangeNotifier authorizationChangeNotifier;

    @Override
    public IPage<PlatformApp> pageApps(Page<PlatformApp> page, PlatformApp condition) {
        return appService.conditionPage(page, condition);
    }

    @Override
    public AppDetailVO getAppDetail(long appId) {
        PlatformApp app = requireApp(appId);
        AppDetailVO vo = applicationConvert.toDetail(app);

        if (app.getPermissionId() != null) {
            PlatformPermission root = permissionService.getById(app.getPermissionId());
            if (root != null) {
                vo.setRootPermissionCode(root.getCode());
            }
        }
        vo.setMenuCount(menuService.count(Wrappers.<PlatformMenu>lambdaQuery()
                .eq(PlatformMenu::getAppId, appId)));
        vo.setPermissionCount(permissionService.count(Wrappers.<PlatformPermission>lambdaQuery()
                .eq(PlatformPermission::getAppId, appId)));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createApp(AppCreateDTO dto) {
        requireNonNull(dto, "ApplicationResourceServiceImpl.ParamNonNull");
        requireNotBlank(dto.getName(), "ApplicationResourceServiceImpl.NameNonNull");
        requireNonNull(dto.getAppType(), "ApplicationResourceServiceImpl.AppTypeNonNull");

        String code = normalizeAppCode(dto.getCode());
        assertionChecker.checkOperation(StrUtil.isNotBlank(code),
                "ApplicationResourceServiceImpl.CodeNonNull");
        assertionChecker.checkOperation(appService.count(Wrappers.<PlatformApp>lambdaQuery()
                        .eq(PlatformApp::getCode, code)) == 0,
                "ApplicationResourceServiceImpl.ExistCode");

        String rootCode = code + PermissionMatcher.ANT_SUBTREE_SUFFIX;
        assertionChecker.checkOperation(permissionService.count(Wrappers.<PlatformPermission>lambdaQuery()
                        .eq(PlatformPermission::getCode, rootCode)) == 0,
                "ApplicationResourceServiceImpl.ExistPermissionCode");

        OrgTypeEnum orgType = dto.getAppType();

        PlatformPermission rootPermission = new PlatformPermission();
        rootPermission.setName(dto.getName());
        rootPermission.setCode(rootCode);
        rootPermission.setOrgType(orgType);
        rootPermission.setNodeType(PermissionNodeTypeEnum.GROUP);
        rootPermission.setPid(IDConstants.ROOT_TREE_ID);
        permissionService.createAndReturnId(rootPermission, false);

        PlatformApp app = new PlatformApp();
        app.setCode(code);
        app.setName(dto.getName());
        app.setAppType(orgType);
        app.setIcon(dto.getIcon());
        app.setIntro(dto.getIntro());
        app.setSort(dto.getSort() == null ? 999 : dto.getSort());
        app.setDefaultAccessMode(dto.getDefaultAccessMode() == null
                ? AppDefaultAccessModeEnum.OPEN
                : dto.getDefaultAccessMode());
        app.setPermissionId(rootPermission.getId());
        appService.create(app);

        rootPermission.setAppId(app.getId());
        permissionService.update(rootPermission);

        // 如果App为组织类型，那么给组织管理员默认追加权限
        if (app.getAppType() == OrgTypeEnum.Tenant) {
            bizRoleService.orgManagerAssignPermissions(List.of(rootPermission.getId()), true);
        }
        authorizationChangeNotifier.markAll();
        return app.getId();
    }

    @Override
    public void updateApp(long appId, AppUpdateDTO dto) {
        requireNonNull(dto, "ApplicationResourceServiceImpl.ParamNonNull");
        PlatformApp app = requireApp(appId);
        if (StrUtil.isNotEmpty(dto.getName())) {
            PlatformPermission rootPermission = new PlatformPermission();
            rootPermission.setId(app.getPermissionId());
            rootPermission.setName(dto.getName());
            permissionService.update(rootPermission);
        }
        applicationConvert.updateApp(dto, app);
        appService.update(app);
        authorizationChangeNotifier.markAll();
    }

    @Override
    public void patchAppStatus(long appId, AppStatusPatchDTO dto) {
        requireNonNull(dto, "ApplicationResourceServiceImpl.ParamNonNull");
        requireNonNull(dto.getStatus(), "ApplicationResourceServiceImpl.StatusNonNull");
        PlatformApp app = requireApp(appId);
        app.setStatus(dto.getStatus());
        appService.update(app);
        authorizationChangeNotifier.markAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteApp(long appId, boolean force) {
        PlatformApp app = requireApp(appId);
        if (force) {
            requireSuperAdmin();
            forceCascadeDeleteApp(app, appId);
            return;
        }

        assertionChecker.checkOperation(menuService.count(Wrappers.<PlatformMenu>lambdaQuery()
                        .eq(PlatformMenu::getAppId, appId)) == 0,
                "ApplicationResourceServiceImpl.HasMenus");
        assertionChecker.checkOperation(permissionService.count(Wrappers.<PlatformPermission>lambdaQuery()
                        .eq(PlatformPermission::getAppId, appId)
                        .ne(app.getPermissionId() != null, PlatformPermission::getId, app.getPermissionId())) == 0,
                "ApplicationResourceServiceImpl.HasPermissions");
        assertionChecker.checkOperation(tenantAppConfigService.count(Wrappers.<TenantAppConfig>lambdaQuery()
                        .eq(TenantAppConfig::getAppId, appId)) == 0,
                "ApplicationResourceServiceImpl.HasTenantAuth");


        if (app.getPermissionId() != null) {
            bizRoleService.orgManagerAssignPermissions(List.of(app.getPermissionId()), false);
            List<Long> permissionIds = permissionService.list(Wrappers.<PlatformPermission>lambdaQuery()
                            .eq(PlatformPermission::getAppId, appId)
                            .select(PlatformPermission::getId))
                    .stream()
                    .map(PlatformPermission::getId)
                    .toList();
            if (!permissionIds.isEmpty()) {
                assertionChecker.checkOperation(rolePermissionService.count(Wrappers.<PlatformRolePermission>lambdaQuery()
                                .in(PlatformRolePermission::getPermissionId, permissionIds)) == 0,
                        "ApplicationResourceServiceImpl.HasRoleBinding");
            }
            permissionService.delete(app.getPermissionId());
        }

        appService.delete(appId);
        authorizationChangeNotifier.markAll();
    }

    /**
     * 超级管理员强制级联删除应用：清除应用全部权限的平台角色绑定，
     * 解绑组织管理员根权限，并按 {@code app_id} 批量删除全部权限与菜单。
     *
     * @param app   应用
     * @param appId 应用ID
     */
    private void forceCascadeDeleteApp(PlatformApp app, long appId) {
        List<Long> permissionIds = permissionService.list(Wrappers.<PlatformPermission>lambdaQuery()
                        .eq(PlatformPermission::getAppId, appId)
                        .select(PlatformPermission::getId))
                .stream()
                .map(PlatformPermission::getId)
                .toList();

        if (app.getPermissionId() != null) {
            bizRoleService.orgManagerAssignPermissions(List.of(app.getPermissionId()), false);
        }
        if (CollUtil.isNotEmpty(permissionIds)) {
            rolePermissionService.clearByPermissionIds(permissionIds);
        }

        permissionService.deleteByAppId(appId);
        List<PlatformMenu> menus = menuService.list(Wrappers.<PlatformMenu>lambdaQuery()
                .eq(PlatformMenu::getAppId, appId)
                .select(PlatformMenu::getId));
        for (PlatformMenu menu : menus) {
            menuPermissionService.clearByMenuId(menu.getId());
        }
        resourceService.remove(Wrappers.<PlatformResource>lambdaQuery()
                .eq(PlatformResource::getAppId, appId));
        menuService.deleteByAppId(appId);
        TenantEnv.globalRun(() -> tenantAppConfigService.clearByAppId(appId));
        appService.delete(appId);

        log.warn("[ForceDeleteApp] 超级管理员强制删除应用 appId={}, code={}, 删除权限数={}",
                appId, app.getCode(), permissionIds.size());
        authorizationChangeNotifier.markAll();
    }

    /**
     * 校验当前登录用户为超级管理员（{@code ROLE_ADMIN_CODE}），否则拒绝操作。
     */
    private void requireSuperAdmin() {
        InUser user = SecurityAuthContext.getUser();
        boolean admin = user != null && user.getRoleCodeList() != null
                && user.getRoleCodeList().stream().anyMatch(RoleUtil::isAdmin);
        assertionChecker.checkOperation(admin,
                "ApplicationResourceServiceImpl.ForceDeleteRequireAdmin");
    }

    @Override
    public List<MenuTreeNodeVO> getMenuTree(long appId) {
        requireApp(appId);
        Map<Long, List<Long>> permissionIds = menuPermissionService.mapPermissionIds();
        List<MenuTreeNodeVO> nodes = menuService.nodeList().stream()
                .filter(node -> Objects.equals(node.getAppId(), appId))
                .filter(node -> node.getMenuType() != MenuTypeEnum.Button)
                .peek(node -> {
                    List<Long> ids = permissionIds.getOrDefault(node.getId(), List.of());
                    node.setPermissionIds(ids);
                })
                .sorted(Comparator.comparing(MenuTreeNodeVO::getSort))
                .collect(Collectors.toList());
        return TreeUtil.build(nodes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMenu(long appId, AppMenuCreateDTO dto) {
        requireNonNull(dto, "ApplicationResourceServiceImpl.ParamNonNull");
        requireNotBlank(dto.getName(), "ApplicationResourceServiceImpl.NameNonNull");
        assertionChecker.checkOperation(dto.getMenuType() != MenuTypeEnum.Button,
                "ApplicationResourceServiceImpl.CantCreateButtonMenu");
        PlatformApp app = requireApp(appId);
        PlatformMenu menu = applicationConvert.toMenu(dto);
        if (menu.getSort() == null) {
            menu.setSort(999);
        }
        if (menu.getStatus() == null) {
            menu.setStatus(CommonStatusEnum.ENABLE);
        }
        if (menu.getPermissionMatchMode() == null) {
            menu.setPermissionMatchMode(PermissionMatchModeEnum.ANY);
        }
        menu.setAppId(appId);
        menu.setOrgType(app.getAppType());
        if (StrUtil.isBlank(menu.getPath())
                && (menu.getPid() == null || menu.getPid() <= IDConstants.ROOT_TREE_ID)) {
            menu.setPath(StrUtil.SLASH + app.getCode());
        }
        syncAccessMode(menu);
        validateParentMenu(appId, menu.getPid());
        List<Long> permissionIds = validateMenuPermissions(appId, menu, dto.getPermissionIds());
        menuService.create(menu);
        menuPermissionService.replace(menu.getId(), permissionIds);
        return menu.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(long appId, long menuId, AppMenuUpdateDTO dto) {
        requireNonNull(dto, "ApplicationResourceServiceImpl.ParamNonNull");
        requireApp(appId);
        requireMenu(appId, menuId);
        PlatformMenu menu = new PlatformMenu();
        menu.setId(menuId);
        applicationConvert.updateMenu(dto, menu);
        if (dto.getPermissionIds() != null || dto.getPermissionMatchMode() != null || dto.getAccessMode() != null) {
            PlatformMenu current = requireMenu(appId, menuId);
            if (dto.getAccessMode() != null) {
                current.setAccessMode(dto.getAccessMode());
            }
            if (dto.getMenuType() != null) {
                current.setMenuType(dto.getMenuType());
            }
            if (dto.getPermissionMatchMode() != null) {
                current.setPermissionMatchMode(dto.getPermissionMatchMode());
            }
            List<Long> permissionIds = dto.getPermissionIds() != null
                    ? dto.getPermissionIds()
                    : menuPermissionService.listPermissionIds(menuId);
            validateMenuPermissions(appId, current, permissionIds);
            menuPermissionService.replace(menuId, permissionIds);
        }
        menuService.update(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(long appId, long menuId) {
        requireApp(appId);
        requireMenu(appId, menuId);
        assertionChecker.checkOperation(menuService.count(Wrappers.<PlatformMenu>lambdaQuery()
                        .eq(PlatformMenu::getPid, menuId)) == 0,
                "PlatformMenuServiceImpl.ExistLeaf");
        menuPermissionService.clearByMenuId(menuId);
        menuService.delete(menuId);
    }

    @Override
    public List<AppPermissionTreeNodeVO> getPermissionTree(long appId) {
        requireApp(appId);
        List<AppPermissionTreeNodeVO> nodes = permissionService.list().stream()
                .filter(item -> Objects.equals(item.getAppId(), appId))
                .sorted(Comparator.comparing(PlatformPermission::getOrgType)
                        .thenComparing(PlatformPermission::getId))
                .map(applicationConvert::toPermissionTreeNode)
                .collect(Collectors.toList());
        List<AppPermissionTreeNodeVO> tree = TreeUtil.build(nodes);
        TreeUtil.compensate(tree, nodes);
        return tree;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPermission(long appId, AppPermissionCreateDTO dto) {
        requireNonNull(dto, "ApplicationResourceServiceImpl.ParamNonNull");
        requireNonNull(dto.getPid(), "ApplicationResourceServiceImpl.PidNonNull");
        requireNotBlank(dto.getName(), "ApplicationResourceServiceImpl.NameNonNull");
        requireNonNull(dto.getNodeType(), "ApplicationResourceServiceImpl.NodeTypeNonNull");
        PlatformApp app = requireApp(appId);
        PlatformPermission parent = requirePermission(appId, dto.getPid());
        validatePermissionCode(app, dto.getNodeType(), dto.getCode());

        PlatformPermission permission = new PlatformPermission();
        permission.setPid(parent.getId());
        permission.setName(dto.getName());
        permission.setCode(buildChildCode(parent, dto.getCode(), app.getCode()));
        permission.setOrgType(parent.getOrgType());
        permission.setAppId(appId);
        permission.setNodeType(dto.getNodeType());
        permission.setResourceId(resolveResourceId(appId, dto.getResourceId()));
        permission.setRemark(dto.getRemark());
        if (dto.getStatus() != null) {
            permission.setStatus(dto.getStatus());
        }
        Long id = permissionService.createAndReturnId(permission, false);
        authorizationChangeNotifier.markAll();
        return id;
    }

    @Override
    public void updatePermission(long appId, long permissionId, AppPermissionUpdateDTO dto) {
        requireNonNull(dto, "ApplicationResourceServiceImpl.ParamNonNull");
        requirePermission(appId, permissionId);
        PlatformPermission permission = new PlatformPermission();
        permission.setId(permissionId);
        applicationConvert.updatePermission(dto, permission);
        permissionService.update(permission);
        authorizationChangeNotifier.markAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermission(long appId, long permissionId) {
        PlatformApp app = requireApp(appId);
        requirePermission(appId, permissionId);
        assertionChecker.checkOperation(!Objects.equals(app.getPermissionId(), permissionId),
                "ApplicationResourceServiceImpl.CantDeleteRoot");
        assertionChecker.checkOperation(menuPermissionService.count(Wrappers.<PlatformMenuPermission>lambdaQuery()
                        .eq(PlatformMenuPermission::getPermissionId, permissionId)) == 0,
                "ApplicationResourceServiceImpl.PermissionInUse");
        assertionChecker.checkOperation(rolePermissionService.count(Wrappers.<PlatformRolePermission>lambdaQuery()
                        .eq(PlatformRolePermission::getPermissionId, permissionId)) == 0,
                "ApplicationResourceServiceImpl.HasRoleBinding");
        Long tenantRefs = TenantEnv.globalApply(() -> tenantRolePermissionService.count(
                Wrappers.<TenantRolePermissionPrivate>lambdaQuery()
                        .eq(TenantRolePermissionPrivate::getPermissionId, permissionId)));
        assertionChecker.checkOperation(tenantRefs == 0, "ApplicationResourceServiceImpl.PermissionInUse");

        rolePermissionService.clearByPermissionId(permissionId);
        permissionService.delete(permissionId);
        authorizationChangeNotifier.markAll();
    }

    @Override
    public List<PlatformResource> listResources(long appId) {
        requireApp(appId);
        return resourceService.list(Wrappers.<PlatformResource>lambdaQuery()
                .eq(PlatformResource::getAppId, appId)
                .orderByAsc(PlatformResource::getCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createResource(long appId, AppResourceCreateDTO dto) {
        requireNonNull(dto, "ApplicationResourceServiceImpl.ParamNonNull");
        requireNotBlank(dto.getCode(), "ApplicationResourceServiceImpl.CodeNonNull");
        requireNotBlank(dto.getName(), "ApplicationResourceServiceImpl.NameNonNull");
        requireApp(appId);
        assertionChecker.checkOperation(resourceService.count(Wrappers.<PlatformResource>lambdaQuery()
                        .eq(PlatformResource::getAppId, appId)
                        .eq(PlatformResource::getCode, dto.getCode().trim())) == 0,
                "ApplicationResourceServiceImpl.ExistResourceCode");
        PlatformResource resource = new PlatformResource();
        resource.setAppId(appId);
        resource.setCode(dto.getCode().trim());
        resource.setName(dto.getName());
        resource.setStatus(dto.getStatus() == null ? CommonStatusEnum.ENABLE : dto.getStatus());
        resourceService.save(resource);
        authorizationChangeNotifier.markAll();
        return resource.getId();
    }

    @Override
    public void updateResource(long appId, long resourceId, AppResourceUpdateDTO dto) {
        requireNonNull(dto, "ApplicationResourceServiceImpl.ParamNonNull");
        requireApp(appId);
        PlatformResource resource = requireResource(appId, resourceId);
        if (StrUtil.isNotBlank(dto.getName())) {
            resource.setName(dto.getName());
        }
        if (dto.getStatus() != null) {
            resource.setStatus(dto.getStatus());
        }
        resourceService.updateById(resource);
        authorizationChangeNotifier.markAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResource(long appId, long resourceId) {
        requireApp(appId);
        requireResource(appId, resourceId);
        assertionChecker.checkOperation(permissionService.count(Wrappers.<PlatformPermission>lambdaQuery()
                        .eq(PlatformPermission::getResourceId, resourceId)) == 0,
                "ApplicationResourceServiceImpl.ResourceInUse");
        assertionChecker.checkOperation(platformRoleDataRuleService.count(Wrappers.<PlatformRoleDataRule>lambdaQuery()
                        .eq(PlatformRoleDataRule::getResourceId, resourceId)) == 0,
                "ApplicationResourceServiceImpl.ResourceInUse");
        Long tenantRules = TenantEnv.globalApply(() -> tenantRoleDataRuleService.count(
                Wrappers.<TenantRoleDataRulePrivate>lambdaQuery()
                        .eq(TenantRoleDataRulePrivate::getResourceId, resourceId)));
        assertionChecker.checkOperation(tenantRules == 0, "ApplicationResourceServiceImpl.ResourceInUse");
        resourceService.removeById(resourceId);
        authorizationChangeNotifier.markAll();
    }

    private PlatformResource requireResource(long appId, long resourceId) {
        PlatformResource resource = resourceService.getById(resourceId);
        assertionChecker.checkOperation(resource != null, "ApplicationResourceServiceImpl.ResourceNonExist");
        assert resource != null;
        assertionChecker.checkOperation(Objects.equals(resource.getAppId(), appId),
                "ApplicationResourceServiceImpl.ResourceAppMismatch");
        return resource;
    }

    private PlatformApp requireApp(long appId) {
        PlatformApp app = appService.getById(appId);
        assertionChecker.checkOperation(app != null, "ApplicationResourceServiceImpl.AppNonExist");
        return app;
    }

    private PlatformMenu requireMenu(long appId, long menuId) {
        PlatformMenu menu = menuService.getById(menuId);
        assertionChecker.checkOperation(menu != null, "PlatformMenuServiceImpl.NonExist");
        assert menu != null;
        assertionChecker.checkOperation(Objects.equals(menu.getAppId(), appId),
                "ApplicationResourceServiceImpl.MenuAppMismatch");
        return menu;
    }

    private PlatformPermission requirePermission(long appId, long permissionId) {
        PlatformPermission permission = permissionService.getById(permissionId);
        assertionChecker.checkOperation(permission != null, "BizPlatformAuthorityServiceImpl.NotExist");
        assert permission != null;
        assertionChecker.checkOperation(Objects.equals(permission.getAppId(), appId),
                "ApplicationResourceServiceImpl.PermissionAppMismatch");
        return permission;
    }

    /**
     * 判断菜单是否为根级（无父级或父级为根节点）。
     *
     * @param menu 菜单
     * @return 是否根级菜单
     */
    private boolean isRootLevelMenu(PlatformMenu menu) {
        return menu.getPid() == null || menu.getPid() <= IDConstants.ROOT_TREE_ID;
    }

    private List<Long> validateMenuPermissions(long appId, PlatformMenu menu, List<Long> permissionIds) {
        boolean protectedPage = menu.getMenuType() == MenuTypeEnum.Menu
                && menu.getAccessMode() == AccessModeEnum.PERMISSION;
        boolean allowEmpty = menu.getMenuType() == MenuTypeEnum.Directory
                || menu.getAccessMode() == AccessModeEnum.OPEN;
        if (protectedPage) {
            assertionChecker.checkOperation(CollUtil.isNotEmpty(permissionIds),
                    "ApplicationResourceServiceImpl.ProtectedMenuRequiresPermissions");
        } else if (allowEmpty) {
            assertionChecker.checkOperation(CollUtil.isEmpty(permissionIds),
                    "ApplicationResourceServiceImpl.OpenOrDirectoryMenuNoPermissions");
            return List.of();
        } else {
            assertionChecker.checkOperation(CollUtil.isEmpty(permissionIds),
                    "ApplicationResourceServiceImpl.OpenOrDirectoryMenuNoPermissions");
            return List.of();
        }
        List<Long> distinct = permissionIds.stream().distinct().toList();
        for (Long permissionId : distinct) {
            PlatformPermission permission = requirePermission(appId, permissionId);
            assertionChecker.checkOperation(permission.getNodeType() == PermissionNodeTypeEnum.ACTION,
                    "ApplicationResourceServiceImpl.MenuPermissionMustAction");
            assertionChecker.checkOperation(permission.getStatus() == CommonStatusEnum.ENABLE,
                    "ApplicationResourceServiceImpl.MenuPermissionDisabled");
        }
        return distinct;
    }

    private Long resolveResourceId(long appId, Long resourceId) {
        if (resourceId == null) {
            return null;
        }
        PlatformResource resource = resourceService.getById(resourceId);
        assertionChecker.checkOperation(resource != null, "ApplicationResourceServiceImpl.ResourceNonExist");
        assert resource != null;
        assertionChecker.checkOperation(Objects.equals(resource.getAppId(), appId),
                "ApplicationResourceServiceImpl.ResourceAppMismatch");
        return resourceId;
    }

    private void validateParentMenu(long appId, Long pid) {
        if (pid == null || pid <= IDConstants.ROOT_TREE_ID) {
            return;
        }
        PlatformMenu parent = menuService.getById(pid);
        assertionChecker.checkOperation(parent != null, "PlatformMenuServiceImpl.ParentNonExist");
        assert parent != null;
        assertionChecker.checkOperation(Objects.equals(parent.getAppId(), appId),
                "ApplicationResourceServiceImpl.MenuAppMismatch");
    }

    private void syncAccessMode(PlatformMenu menu) {
        // access_mode 为单一来源；应用接口未显式指定时默认开放
        if (menu.getAccessMode() == null) {
            menu.setAccessMode(AccessModeEnum.OPEN);
        }
    }

    private String normalizeAppCode(String code) {
        if (code == null) {
            return null;
        }
        return StrUtil.trim(code);
    }

    private void requireNonNull(Object value, String messageKey) {
        assertionChecker.checkOperation(value != null, messageKey);
    }

    private void requireNotBlank(String value, String messageKey) {
        assertionChecker.checkOperation(StrUtil.isNotBlank(value), messageKey);
    }

    private void validatePermissionCode(PlatformApp app, PermissionNodeTypeEnum nodeType, String code) {
        assertionChecker.checkOperation(StrUtil.isNotBlank(code),
                "ApplicationResourceServiceImpl.CodeNonNull");
        if (nodeType == PermissionNodeTypeEnum.GROUP) {
            assertionChecker.checkOperation(code.endsWith(PermissionMatcher.ANT_SUBTREE_SUFFIX),
                    "ApplicationResourceServiceImpl.GroupCodeMustWildcard");
        } else if (nodeType == PermissionNodeTypeEnum.ACTION) {
            assertionChecker.checkOperation(!code.endsWith(PermissionMatcher.ANT_SUBTREE_SUFFIX)
                            && !code.endsWith(PermissionMatcher.SINGLE_WILDCARD_SUFFIX),
                    "ApplicationResourceServiceImpl.ActionCodeMustExact");
        }
    }

    private String buildChildCode(PlatformPermission parent, String code, String appCode) {
        if (StrUtil.isBlank(code)) {
            return code;
        }
        String namespace = appCode + StrUtil.COLON;
        // 如果菜单是命名空间开头，或者为权限的权限编码开头，那么直接返回
        if (code.startsWith(namespace) || (parent != null && code.startsWith(parent.getCode()))) {
            return code;
        }
        if (parent != null && PermissionMatcher.isWildcard(parent.getCode())) {
            String prefix = PermissionMatcher.wildcardPathPrefix(parent.getCode());
            return prefix + StrUtil.COLON + code;
        }
        if (parent != null) {
            return parent.getCode() + StrUtil.COLON + code;
        }
        return namespace + code;
    }

    /**
     * 为权限编码追加 {@code :**} 后缀（已存在则跳过）。
     */
    private String appendAntSubtreeSuffix(String code) {
        if (StrUtil.isBlank(code) || PermissionMatcher.isAntSubtreeWildcard(code)) {
            return code;
        }
        return code + PermissionMatcher.ANT_SUBTREE_SUFFIX;
    }
}
