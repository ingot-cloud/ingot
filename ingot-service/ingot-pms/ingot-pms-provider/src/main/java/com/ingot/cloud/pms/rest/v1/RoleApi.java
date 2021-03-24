package com.ingot.cloud.pms.rest.v1;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.service.SysRoleService;
import com.ingot.framework.core.validation.Group;
import com.ingot.framework.core.wrapper.BaseController;
import com.ingot.framework.core.wrapper.IngotResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : RoleApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/22.</p>
 * <p>Time         : 4:41 下午.</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/role")
@AllArgsConstructor
public class RoleApi extends BaseController {
    private final SysRoleService sysRoleService;

    @GetMapping("/page")
    public IngotResponse<?> page(Page<SysRole> page, SysRole condition) {
        return ok(sysRoleService.conditionPage(page, condition));
    }

    @PostMapping
    public IngotResponse<?> create(@Validated(Group.Create.class) @RequestBody SysRole params) {
        sysRoleService.createRole(params);
        return ok();
    }

    @PutMapping
    public IngotResponse<?> update(@Validated(Group.Update.class) @RequestBody SysRole params) {
        sysRoleService.updateRoleById(params);
        return ok();
    }

    @DeleteMapping("/{id}")
    public IngotResponse<?> removeById(@PathVariable Long id) {
        sysRoleService.removeRoleById(id);
        return ok();
    }
}
