package com.ingot.cloud.pms.web.v1.platform.config;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.PlatformApp;
import com.ingot.cloud.pms.api.model.dto.application.*;
import com.ingot.cloud.pms.api.model.vo.application.AppDetailVO;
import com.ingot.cloud.pms.api.model.vo.application.AppPermissionTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.authorization.resource.ApplicationResourceService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>应用中心化 REST 入口，提供应用及其菜单、权限的统一管理接口（/v1/platform/config/apps）。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "平台应用资源管理", description = "应用中心化：应用 / 菜单 / 权限")
@RequestMapping("/v1/platform/config/apps")
@RequiredArgsConstructor
public class PlatformApplicationAPI implements RShortcuts {
    private final ApplicationResourceService applicationResourceService;

    @AdminOrHasAnyAuthority({"platform:config:app:query"})
    @GetMapping("/page")
    @Operation(summary = "应用分页列表")
    public R<IPage<PlatformApp>> page(Page<PlatformApp> page, PlatformApp condition) {
        return ok(applicationResourceService.pageApps(page, condition));
    }

    @AdminOrHasAnyAuthority({"platform:config:app:query"})
    @GetMapping("/{appId}")
    @Operation(summary = "应用详情")
    public R<AppDetailVO> detail(@Parameter(description = "应用 ID") @PathVariable Long appId) {
        return ok(applicationResourceService.getAppDetail(appId));
    }

    @AdminOrHasAnyAuthority({"platform:config:app:create"})
    @PostMapping
    @Operation(summary = "创建应用", description = "自动创建 app_code:** 根权限，不要求 menuId")
    public R<Long> create(@RequestBody AppCreateDTO dto) {
        return ok(applicationResourceService.createApp(dto));
    }

    @AdminOrHasAnyAuthority({"platform:config:app:update"})
    @PutMapping("/{appId}")
    @Operation(summary = "更新应用基本信息", description = "应用编码不可修改")
    public R<?> update(@PathVariable Long appId, @RequestBody AppUpdateDTO dto) {
        applicationResourceService.updateApp(appId, dto);
        return ok();
    }

    @AdminOrHasAnyAuthority({"platform:config:app:update"})
    @PatchMapping("/{appId}/status")
    @Operation(summary = "启用/禁用应用")
    public R<?> patchStatus(@PathVariable Long appId, @RequestBody AppStatusPatchDTO dto) {
        applicationResourceService.patchAppStatus(appId, dto);
        return ok();
    }

    @AdminOrHasAnyAuthority({"platform:config:app:delete"})
    @DeleteMapping("/{appId}")
    @Operation(summary = "删除应用",
            description = "默认存在菜单、子权限、租户授权或角色绑定时拒绝；force=true 为超级管理员强制级联删除，"
                    + "但存在租户授权时仍拒绝")
    public R<?> delete(@PathVariable Long appId,
                       @Parameter(description = "是否强制级联删除（仅超级管理员）")
                       @RequestParam(name = "force", defaultValue = "false") boolean force) {
        applicationResourceService.deleteApp(appId, force);
        return ok();
    }

    @AdminOrHasAnyAuthority({"platform:config:app:menu:query"})
    @GetMapping("/{appId}/menus/tree")
    @Operation(summary = "应用菜单树")
    public R<List<MenuTreeNodeVO>> menuTree(@PathVariable Long appId) {
        return ok(applicationResourceService.getMenuTree(appId));
    }

    @AdminOrHasAnyAuthority({"platform:config:app:menu:create"})
    @PostMapping("/{appId}/menus")
    @Operation(summary = "创建应用菜单", description = "自动创建托管 NAVIGATION 权限；目录类型追加 :**")
    public R<Long> createMenu(@PathVariable Long appId, @RequestBody AppMenuCreateDTO dto) {
        return ok(applicationResourceService.createMenu(appId, dto));
    }

    @AdminOrHasAnyAuthority({"platform:config:app:menu:update"})
    @PutMapping("/{appId}/menus/{menuId}")
    @Operation(summary = "更新应用菜单")
    public R<?> updateMenu(@PathVariable Long appId,
                           @PathVariable Long menuId,
                           @RequestBody AppMenuUpdateDTO dto) {
        applicationResourceService.updateMenu(appId, menuId, dto);
        return ok();
    }

    @AdminOrHasAnyAuthority({"platform:config:app:menu:delete"})
    @DeleteMapping("/{appId}/menus/{menuId}")
    @Operation(summary = "删除应用菜单")
    public R<?> deleteMenu(@PathVariable Long appId, @PathVariable Long menuId) {
        applicationResourceService.deleteMenu(appId, menuId);
        return ok();
    }

    @AdminOrHasAnyAuthority({"platform:config:app:permission:query"})
    @GetMapping("/{appId}/permissions/tree")
    @Operation(summary = "应用权限树", description = "托管 NAVIGATION 权限标记 managed/readOnly")
    public R<List<AppPermissionTreeNodeVO>> permissionTree(@PathVariable Long appId) {
        return ok(applicationResourceService.getPermissionTree(appId));
    }

    @AdminOrHasAnyAuthority({"platform:config:app:permission:create"})
    @PostMapping("/{appId}/permissions")
    @Operation(summary = "创建应用权限", description = "仅 GROUP / ACTION；GROUP 编码须以 :* 结尾")
    public R<Long> createPermission(@PathVariable Long appId,
                                    @RequestBody AppPermissionCreateDTO dto) {
        return ok(applicationResourceService.createPermission(appId, dto));
    }

    @AdminOrHasAnyAuthority({"platform:config:app:permission:update"})
    @PutMapping("/{appId}/permissions/{permissionId}")
    @Operation(summary = "更新应用权限", description = "托管权限只读")
    public R<?> updatePermission(@PathVariable Long appId,
                                 @PathVariable Long permissionId,
                                 @RequestBody AppPermissionUpdateDTO dto) {
        applicationResourceService.updatePermission(appId, permissionId, dto);
        return ok();
    }

    @AdminOrHasAnyAuthority({"platform:config:app:permission:delete"})
    @DeleteMapping("/{appId}/permissions/{permissionId}")
    @Operation(summary = "删除应用权限", description = "不可删除根权限与托管权限")
    public R<?> deletePermission(@PathVariable Long appId, @PathVariable Long permissionId) {
        applicationResourceService.deletePermission(appId, permissionId);
        return ok();
    }
}