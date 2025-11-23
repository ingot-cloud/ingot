package com.ingot.cloud.pms.web.v1.admin;

import com.ingot.cloud.pms.service.biz.BizMenuService;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.domain.SysMenuService;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : MenuApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/30.</p>
 * <p>Time         : 10:12 下午.</p>
 */
@RestController
@Tag(description = "menu", name = "系统菜单管理模块")
@RequestMapping(value = "/v1/admin/menu")
@RequiredArgsConstructor
public class AdminMenuAPI implements RShortcuts {
    private final SysMenuService sysMenuService;
    private final BizMenuService bizMenuService;
    private final BizUserService bizUserService;

//    @GetMapping("/userMenu")
//    public R<?> userMenu() {
//        return ok(bizUserService.getUserMenus(SecurityAuthContext.getUser()));
//    }
//
//    @HasAnyAuthority({"basic:menu:w", "basic:menu:r"})
//    @GetMapping("/tree")
//    public R<?> tree(MenuFilterDTO filter) {
//        return ok(sysMenuService.treeList(filter));
//    }
//
//    @HasAnyAuthority({"basic:menu:w"})
//    @PostMapping
//    public R<?> create(@Validated(Group.Create.class) @RequestBody SysMenu params) {
//        bizMenuService.createMenu(params);
//        return ok();
//    }
//
//    @HasAnyAuthority({"basic:menu:w"})
//    @PutMapping
//    public R<?> update(@Validated(Group.Update.class) @RequestBody SysMenu params) {
//        bizMenuService.updateMenu(params);
//        return ok();
//    }
//
//    @HasAnyAuthority({"basic:menu:w"})
//    @DeleteMapping("/{id}")
//    public R<?> removeById(@PathVariable Long id) {
//        bizMenuService.removeMenuById(id);
//        return ok();
//    }
}
