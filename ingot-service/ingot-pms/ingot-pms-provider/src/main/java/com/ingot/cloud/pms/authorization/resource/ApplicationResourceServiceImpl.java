package com.ingot.cloud.pms.authorization.resource;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.convert.ApplicationConvert;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.dto.application.*;
import com.ingot.cloud.pms.api.model.enums.*;
import com.ingot.cloud.pms.api.model.vo.application.AppDetailVO;
import com.ingot.cloud.pms.api.model.vo.application.AppPermissionTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.authorization.engine.PermissionMatcher;
import com.ingot.cloud.pms.core.BizMenuUtils;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.commons.constants.IDConstants;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.enums.PermissionTypeEnum;
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
    private final TenantAppConfigService tenantAppConfigService;
    private final BizRoleService bizRoleService;
    private final AssertionChecker assertionChecker;
    private final ApplicationConvert applicationConvert;

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
        rootPermission.setType(PermissionTypeEnum.API);
        rootPermission.setOrgType(orgType);
        rootPermission.setNodeType(PermissionNodeTypeEnum.GROUP);
        rootPermission.setSourceType(PermissionSourceTypeEnum.SYSTEM);
        rootPermission.setManaged(false);
        rootPermission.setPid(IDConstants.ROOT_TREE_ID);
        permissionService.createAndReturnId(rootPermission, false);

        PlatformApp app = new PlatformApp();
        app.setCode(code);
        app.setName(dto.getName());
        app.setAppType(orgType);
        app.setIcon(dto.getIcon());
        app.setIntro(dto.getIntro());
        app.setSort(dto.getSort() == null ? 999 : dto.getSort());
        app.setPermissionId(rootPermission.getId());
        appService.create(app);

        rootPermission.setAppId(app.getId());
        permissionService.update(rootPermission);

        // 如果App为组织类型，那么给组织管理员默认追加权限
        if (app.getAppType() == OrgTypeEnum.Tenant) {
            bizRoleService.orgManagerAssignPermissions(List.of(rootPermission.getId()), true);
        }
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
    }

    @Override
    public void patchAppStatus(long appId, AppStatusPatchDTO dto) {
        requireNonNull(dto, "ApplicationResourceServiceImpl.ParamNonNull");
        requireNonNull(dto.getStatus(), "ApplicationResourceServiceImpl.StatusNonNull");
        PlatformApp app = requireApp(appId);
        app.setStatus(dto.getStatus());
        appService.update(app);
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
        menuService.deleteByAppId(appId);
        TenantEnv.globalRun(() -> tenantAppConfigService.clearByAppId(appId));
        appService.delete(appId);

        log.warn("[ForceDeleteApp] 超级管理员强制删除应用 appId={}, code={}, 删除权限数={}",
                appId, app.getCode(), permissionIds.size());
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
        List<MenuTreeNodeVO> nodes = menuService.nodeList().stream()
                .filter(node -> Objects.equals(node.getAppId(), appId))
                .sorted(Comparator.comparing(MenuTreeNodeVO::getSort))
                .collect(Collectors.toList());
        return TreeUtil.build(nodes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMenu(long appId, AppMenuCreateDTO dto) {
        requireNonNull(dto, "ApplicationResourceServiceImpl.ParamNonNull");
        requireNotBlank(dto.getName(), "ApplicationResourceServiceImpl.NameNonNull");
        PlatformApp app = requireApp(appId);
        PlatformMenu menu = applicationConvert.toMenu(dto);
        if (menu.getSort() == null) {
            menu.setSort(999);
        }
        if (menu.getStatus() == null) {
            menu.setStatus(CommonStatusEnum.ENABLE);
        }
        menu.setAppId(appId);
        menu.setOrgType(app.getAppType());
        if (StrUtil.isBlank(menu.getPath())
                && (menu.getPid() == null || menu.getPid() <= IDConstants.ROOT_TREE_ID)) {
            menu.setPath(StrUtil.SLASH + app.getCode());
        }
        syncAccessMode(menu);
        validateParentMenu(appId, menu.getPid());

        // 菜单即应用：根级菜单且其编码与应用编码一致时，复用应用根权限，不再新建权限
        boolean isAppRootMenu = isRootLevelMenu(menu)
                && app.getPermissionId() != null
                && StrUtil.equals(BizMenuUtils.getMenuAuthorityCode(menu), app.getCode());
        if (isAppRootMenu) {
            menu.setPermissionId(app.getPermissionId());
            menuService.create(menu);
            return menu.getId();
        }

        Long permissionId = createManagedMenuPermission(app, menu);
        menu.setPermissionId(permissionId);
        menuService.create(menu);
        syncManagedPermissionSource(menu);
        return menu.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(long appId, long menuId, AppMenuUpdateDTO dto) {
        requireNonNull(dto, "ApplicationResourceServiceImpl.ParamNonNull");
        PlatformApp app = requireApp(appId);
        PlatformMenu current = requireMenu(appId, menuId);
        PlatformMenu menu = new PlatformMenu();
        menu.setId(menuId);
        applicationConvert.updateMenu(dto, menu);

        // 名称、状态变更同步到托管权限；共享应用根权限时不同步，避免篡改应用根权限
        boolean sharesAppRoot = Objects.equals(current.getPermissionId(), app.getPermissionId());
        if (!sharesAppRoot && (menu.getName() != null || menu.getStatus() != null)) {
            PlatformPermission authority = new PlatformPermission();
            authority.setId(current.getPermissionId());
            authority.setName(menu.getName());
            authority.setStatus(menu.getStatus());
            authority.setOrgType(current.getOrgType());
            permissionService.update(authority);
        }
        menuService.update(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(long appId, long menuId) {
        PlatformApp app = requireApp(appId);
        PlatformMenu current = requireMenu(appId, menuId);
        assertionChecker.checkOperation(menuService.count(Wrappers.<PlatformMenu>lambdaQuery()
                        .eq(PlatformMenu::getPid, menuId)) == 0,
                "PlatformMenuServiceImpl.ExistLeaf");

        // 菜单即应用：共享应用根权限时仅删菜单行，权限归应用由 deleteApp 管理
        if (Objects.equals(current.getPermissionId(), app.getPermissionId())) {
            menuService.delete(menuId);
            return;
        }

        assertionChecker.checkOperation(permissionService.count(Wrappers.<PlatformPermission>lambdaQuery()
                        .eq(PlatformPermission::getPid, current.getPermissionId())) == 0,
                "ApplicationResourceServiceImpl.MenuPermissionHasChildren");

        rolePermissionService.clearByPermissionId(current.getPermissionId());
        permissionService.delete(current.getPermissionId());
        menuService.delete(menuId);
    }

    @Override
    public List<AppPermissionTreeNodeVO> getPermissionTree(long appId) {
        requireApp(appId);
        List<AppPermissionTreeNodeVO> nodes = permissionService.list().stream()
                .filter(item -> Objects.equals(item.getAppId(), appId))
                .sorted(Comparator.comparing(PlatformPermission::getOrgType)
                        .thenComparing(PlatformPermission::getId))
                .map(permission -> {
                    AppPermissionTreeNodeVO node = applicationConvert.toPermissionTreeNode(permission);
                    node.setReadOnly(BooleanUtil.isTrue(permission.getManaged())
                            || permission.getNodeType() == PermissionNodeTypeEnum.NAVIGATION);
                    return node;
                })
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
        assertionChecker.checkOperation(dto.getNodeType() != PermissionNodeTypeEnum.NAVIGATION,
                "ApplicationResourceServiceImpl.CantCreateNavigation");

        PlatformPermission parent = requirePermission(appId, dto.getPid());
        validatePermissionCode(app, dto.getNodeType(), dto.getCode());

        PlatformPermission permission = new PlatformPermission();
        permission.setPid(parent.getId());
        permission.setName(dto.getName());
        permission.setCode(buildChildCode(parent, dto.getCode(), app.getCode()));
        permission.setType(PermissionTypeEnum.API);
        permission.setOrgType(parent.getOrgType());
        permission.setAppId(appId);
        permission.setNodeType(dto.getNodeType());
        permission.setSourceType(PermissionSourceTypeEnum.MANUAL);
        permission.setManaged(false);
        permission.setRemark(dto.getRemark());
        if (dto.getStatus() != null) {
            permission.setStatus(dto.getStatus());
        }
        return permissionService.createAndReturnId(permission, false);
    }

    @Override
    public void updatePermission(long appId, long permissionId, AppPermissionUpdateDTO dto) {
        requireNonNull(dto, "ApplicationResourceServiceImpl.ParamNonNull");
        PlatformPermission current = requirePermission(appId, permissionId);
        assertionChecker.checkOperation(!BooleanUtil.isTrue(current.getManaged()),
                "ApplicationResourceServiceImpl.CantUpdateManaged");
        assertionChecker.checkOperation(current.getNodeType() != PermissionNodeTypeEnum.NAVIGATION,
                "ApplicationResourceServiceImpl.CantUpdateNavigation");

        PlatformPermission permission = new PlatformPermission();
        permission.setId(permissionId);
        applicationConvert.updatePermission(dto, permission);
        permissionService.update(permission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermission(long appId, long permissionId) {
        PlatformApp app = requireApp(appId);
        PlatformPermission current = requirePermission(appId, permissionId);
        assertionChecker.checkOperation(!Objects.equals(app.getPermissionId(), permissionId),
                "ApplicationResourceServiceImpl.CantDeleteRoot");
        assertionChecker.checkOperation(!BooleanUtil.isTrue(current.getManaged()),
                "ApplicationResourceServiceImpl.CantDeleteManaged");
        assertionChecker.checkOperation(current.getNodeType() != PermissionNodeTypeEnum.NAVIGATION,
                "ApplicationResourceServiceImpl.CantDeleteNavigation");
        assertionChecker.checkOperation(rolePermissionService.count(Wrappers.<PlatformRolePermission>lambdaQuery()
                        .eq(PlatformRolePermission::getPermissionId, permissionId)) == 0,
                "ApplicationResourceServiceImpl.HasRoleBinding");

        rolePermissionService.clearByPermissionId(permissionId);
        permissionService.delete(permissionId);
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

    private Long createManagedMenuPermission(PlatformApp app, PlatformMenu menu) {
        PlatformPermission authority = new PlatformPermission();
        authority.setName(menu.getName());
        authority.setStatus(menu.getStatus() == null ? CommonStatusEnum.ENABLE : menu.getStatus());
        authority.setType(PermissionTypeEnum.MENU);
        authority.setOrgType(menu.getOrgType());
        authority.setAppId(app.getId());
        authority.setNodeType(PermissionNodeTypeEnum.NAVIGATION);
        authority.setSourceType(PermissionSourceTypeEnum.MENU);
        authority.setManaged(true);

        String menuCode = BizMenuUtils.getMenuAuthorityCode(menu);
        if (menu.getPid() != null && menu.getPid() > IDConstants.ROOT_TREE_ID) {
            PlatformMenu parent = menuService.getById(menu.getPid());
            if (parent != null) {
                authority.setPid(parent.getPermissionId());
                PlatformPermission parentPermission = permissionService.getById(parent.getPermissionId());
                authority.setCode(buildChildCode(parentPermission, menuCode, app.getCode()));
            } else {
                authority.setCode(menuCode);
            }
        } else if (app.getPermissionId() != null) {
            authority.setPid(app.getPermissionId());
            PlatformPermission root = permissionService.getById(app.getPermissionId());
            authority.setCode(buildChildCode(root, menuCode, app.getCode()));
        } else {
            authority.setCode(menuCode);
        }
        if (isDirectoryMenu(menu)) {
            authority.setCode(appendAntSubtreeSuffix(authority.getCode()));
        }
        return permissionService.createAndReturnId(authority, false);
    }

    private void syncManagedPermissionSource(PlatformMenu menu) {
        PlatformPermission permission = permissionService.getById(menu.getPermissionId());
        if (permission == null) {
            return;
        }
        permission.setSourceId(menu.getId());
        permission.setAppId(menu.getAppId());
        permissionService.update(permission);
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
     * 目录菜单对应的托管权限使用 Ant 子树通配 {@code :**}。
     */
    private boolean isDirectoryMenu(PlatformMenu menu) {
        return menu.getMenuType() == MenuTypeEnum.Directory;
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
