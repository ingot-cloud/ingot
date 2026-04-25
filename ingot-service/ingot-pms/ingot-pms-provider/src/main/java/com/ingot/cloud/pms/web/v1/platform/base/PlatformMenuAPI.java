package com.ingot.cloud.pms.web.v1.platform.base;

import com.ingot.cloud.pms.api.model.domain.PlatformMenu;
import com.ingot.cloud.pms.service.biz.BizPlatformMenuService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : PlatformMenuAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/13.</p>
 * <p>Time         : 17:16.</p>
 */
@RestController
@Tag(description = "PlatformMenu", name = "平台菜单管理模块")
@RequestMapping(value = "/v1/platform/base/menu")
@RequiredArgsConstructor
public class PlatformMenuAPI implements RShortcuts {
    private final BizPlatformMenuService menuService;

    @AdminOrHasAnyAuthority({"meta:menu:query"})
    @GetMapping("/tree")
    @Operation(summary = "菜单树", description = "菜单树列表")
    public R<?> tree(PlatformMenu filter) {
        return ok(menuService.treeList(filter));
    }

    @AdminOrHasAnyAuthority({"platform:base:menu:create"})
    @PostMapping
    @Operation(summary = "创建菜单", description = "创建菜单")
    public R<?> create(@Validated(Group.Create.class) @RequestBody PlatformMenu params) {
        menuService.create(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"platform:base:menu:update"})
    @PutMapping
    @Operation(summary = "更新菜单", description = "更新菜单")
    public R<?> update(@Validated(Group.Update.class) @RequestBody PlatformMenu params) {
        menuService.update(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"platform:base:menu:delete"})
    @DeleteMapping("/{id}")
    @Operation(summary = "删除菜单", description = "删除菜单")
    public R<?> removeById(@PathVariable Long id) {
        menuService.delete(id);
        return ok();
    }
}
