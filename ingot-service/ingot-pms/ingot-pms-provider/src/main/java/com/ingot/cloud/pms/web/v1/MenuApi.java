package com.ingot.cloud.pms.web.v1;

import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.service.domain.SysMenuService;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.model.support.R;
import lombok.RequiredArgsConstructor;
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
@RequestMapping(value = "/v1/menu")
@RequiredArgsConstructor
public class MenuApi implements RShortcuts {
    private final SysMenuService sysMenuService;

    @GetMapping("/tree")
    public R<?> tree() {
        return ok(sysMenuService.tree());
    }

    @PostMapping
    public R<?> create(@Validated(Group.Create.class) @RequestBody SysMenu params) {
        sysMenuService.createMenu(params);
        return ok();
    }

    @PutMapping
    public R<?> update(@Validated(Group.Update.class) @RequestBody SysMenu params) {
        sysMenuService.updateMenu(params);
        return ok();
    }

    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Integer id) {
        sysMenuService.removeMenuById(id);
        return ok();
    }
}
