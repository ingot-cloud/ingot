package com.ingot.cloud.pms.web.v1.admin;

import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.domain.SysMenuService;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : MenuApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/30.</p>
 * <p>Time         : 10:12 下午.</p>
 */
@RestController
@RequestMapping(value = "/v1/admin/menu")
@RequiredArgsConstructor
public class AdminMenuAPI implements RShortcuts {
    private final SysMenuService sysMenuService;
    private final BizUserService bizUserService;

    @GetMapping("/userMenu")
    public R<?> userMenu() {
        return ok(bizUserService.getUserMenus(SecurityAuthContext.getUser()));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.menu.write', 'basic.menu.read')")
    @GetMapping("/tree")
    public R<?> tree() {
        return ok(sysMenuService.treeList());
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.menu.write')")
    @PostMapping
    public R<?> create(@Validated(Group.Create.class) @RequestBody SysMenu params) {
        sysMenuService.createMenu(params);
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.menu.write')")
    @PutMapping
    public R<?> update(@Validated(Group.Update.class) @RequestBody SysMenu params) {
        sysMenuService.updateMenu(params);
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.menu.write')")
    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Long id) {
        sysMenuService.removeMenuById(id);
        return ok();
    }
}
