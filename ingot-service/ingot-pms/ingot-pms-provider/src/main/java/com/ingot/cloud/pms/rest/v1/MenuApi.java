package com.ingot.cloud.pms.rest.v1;

import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.service.domain.SysMenuService;
import com.ingot.framework.core.validation.Group;
import com.ingot.framework.core.wrapper.BaseController;
import com.ingot.framework.core.wrapper.IngotResponse;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : MenuApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/30.</p>
 * <p>Time         : 10:12 下午.</p>
 */
@RestController
@RequestMapping(value = "/v1/menu")
@AllArgsConstructor
public class MenuApi extends BaseController {
    private final SysMenuService sysMenuService;

    @GetMapping("/tree")
    public IngotResponse<?> tree() {
        return ok(sysMenuService.tree());
    }

    @PostMapping
    public IngotResponse<?> create(@Validated(Group.Create.class) @RequestBody SysMenu params) {
        sysMenuService.createMenu(params);
        return ok();
    }

    @PutMapping
    public IngotResponse<?> update(@Validated(Group.Update.class) @RequestBody SysMenu params) {
        sysMenuService.updateMenu(params);
        return ok();
    }

    @DeleteMapping("/{id}")
    public IngotResponse<?> removeById(@PathVariable Long id) {
        sysMenuService.removeMenuById(id);
        return ok();
    }
}
