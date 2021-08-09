package com.ingot.cloud.pms.rest.v1;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.core.model.dto.common.RelationDto;
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
    private final SysRoleAuthorityService sysRoleAuthorityService;
    private final SysRoleDeptService sysRoleDeptService;
    private final SysRoleMenuService sysRoleMenuService;
    private final SysRoleOauthClientService sysRoleOauthClientService;
    private final SysRoleUserService sysRoleUserService;

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

    @PutMapping("/bindAuthority")
    public IngotResponse<?> bindAuthority(@RequestBody @Validated RelationDto<Long, Long> params) {
        sysRoleAuthorityService.roleBindAuthorities(params);
        return ok();
    }

    @GetMapping("/bindAuthority/{id}")
    public IngotResponse<?> getBindAuthorities(@PathVariable Long id,
                                               Page<?> page,
                                               @RequestParam("isBind") boolean isBind) {
        return ok(sysRoleAuthorityService.getRoleAuthorities(id, page, isBind));
    }

    @PutMapping("/bindDept")
    public IngotResponse<?> bindDept(@RequestBody @Validated RelationDto<Long, Long> params) {
        sysRoleDeptService.roleBindDepts(params);
        return ok();
    }

    @GetMapping("/bindDept/{id}")
    public IngotResponse<?> getBindDepts(@PathVariable Long id,
                                         Page<?> page,
                                         @RequestParam("isBind") boolean isBind) {
        return ok(sysRoleDeptService.getRoleDepts(id, page, isBind));
    }

    @PutMapping("/bindMenu")
    public IngotResponse<?> bindMenu(@RequestBody @Validated RelationDto<Long, Long> params) {
        sysRoleMenuService.roleBindMenus(params);
        return ok();
    }

    @GetMapping("/bindMenu/{id}")
    public IngotResponse<?> getBindMenus(@PathVariable Long id,
                                         Page<?> page,
                                         @RequestParam("isBind") boolean isBind) {
        return ok(sysRoleMenuService.getRoleMenus(id, page, isBind));
    }

    @PutMapping("/bindClient")
    public IngotResponse<?> bindClient(@RequestBody @Validated RelationDto<Long, Long> params) {
        sysRoleOauthClientService.roleBindClients(params);
        return ok();
    }

    @GetMapping("/bindClient/{id}")
    public IngotResponse<?> getBindClients(@PathVariable Long id,
                                           Page<?> page,
                                           @RequestParam("isBind") boolean isBind) {
        return ok(sysRoleOauthClientService.getRoleClients(id, page, isBind));
    }

    @PutMapping("/bindUser")
    public IngotResponse<?> bindUser(@RequestBody @Validated RelationDto<Long, Long> params) {
        sysRoleUserService.roleBindUsers(params);
        return ok();
    }

    @GetMapping("/bindUser/{id}")
    public IngotResponse<?> getBindUsers(@PathVariable Long id,
                                         Page<?> page,
                                         @RequestParam("isBind") boolean isBind,
                                         SysUser condition) {
        return ok(sysRoleUserService.getRoleUsers(id, page, isBind, condition));
    }
}
