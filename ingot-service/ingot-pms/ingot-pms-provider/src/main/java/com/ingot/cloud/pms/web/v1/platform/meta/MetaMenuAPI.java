package com.ingot.cloud.pms.web.v1.platform.meta;

import com.ingot.cloud.pms.api.model.domain.MetaMenu;
import com.ingot.cloud.pms.service.biz.BizMetaMenuService;
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
 * <p>Description  : MetaMenuAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/13.</p>
 * <p>Time         : 17:16.</p>
 */
@RestController
@Tag(description = "MetaMenu", name = "元数据菜单管理模块")
@RequestMapping(value = "/v1/platform/meta/menu")
@RequiredArgsConstructor
public class MetaMenuAPI implements RShortcuts {
    private final BizMetaMenuService menuService;

    @AdminOrHasAnyAuthority({"meta:menu:query"})
    @GetMapping("/tree")
    @Operation(summary = "菜单树", description = "菜单树列表")
    public R<?> tree(MetaMenu filter) {
        return ok(menuService.treeList(filter));
    }

    @AdminOrHasAnyAuthority({"platform:meta:menu:create"})
    @PostMapping
    @Operation(summary = "创建菜单", description = "创建菜单")
    public R<?> create(@Validated(Group.Create.class) @RequestBody MetaMenu params) {
        menuService.create(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"platform:meta:menu:update"})
    @PutMapping
    @Operation(summary = "更新菜单", description = "更新菜单")
    public R<?> update(@Validated(Group.Update.class) @RequestBody MetaMenu params) {
        menuService.update(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"platform:meta:menu:delete"})
    @DeleteMapping("/{id}")
    @Operation(summary = "删除菜单", description = "删除菜单")
    public R<?> removeById(@PathVariable Long id) {
        menuService.delete(id);
        return ok();
    }
}
