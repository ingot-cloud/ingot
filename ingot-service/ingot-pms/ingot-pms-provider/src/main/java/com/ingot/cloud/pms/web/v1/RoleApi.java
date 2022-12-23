package com.ingot.cloud.pms.web.v1;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.Oauth2RegisteredClient;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.service.domain.SysRoleAuthorityService;
import com.ingot.cloud.pms.service.domain.SysRoleDeptService;
import com.ingot.cloud.pms.service.domain.SysRoleMenuService;
import com.ingot.cloud.pms.service.domain.SysRoleOauthClientService;
import com.ingot.cloud.pms.service.domain.SysRoleService;
import com.ingot.cloud.pms.service.domain.SysRoleUserService;
import com.ingot.framework.core.model.dto.common.RelationDTO;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : RoleApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/22.</p>
 * <p>Time         : 4:41 下午.</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/role")
@RequiredArgsConstructor
public class RoleApi implements RShortcuts {
    private final SysRoleService sysRoleService;
    private final SysRoleAuthorityService sysRoleAuthorityService;
    private final SysRoleDeptService sysRoleDeptService;
    private final SysRoleMenuService sysRoleMenuService;
    private final SysRoleOauthClientService sysRoleOauthClientService;
    private final SysRoleUserService sysRoleUserService;

    @GetMapping("/options")
    public R<?> options() {
        return ok(sysRoleService.options());
    }

    @GetMapping("/page")
    public R<?> page(Page<SysRole> page, SysRole condition) {
        return ok(sysRoleService.conditionPage(page, condition));
    }

    @PostMapping
    public R<?> create(@Validated(Group.Create.class) @RequestBody SysRole params) {
        sysRoleService.createRole(params);
        return ok();
    }

    @PutMapping
    public R<?> update(@Validated(Group.Update.class) @RequestBody SysRole params) {
        sysRoleService.updateRoleById(params);
        return ok();
    }

    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Long id) {
        sysRoleService.removeRoleById(id);
        return ok();
    }

    @PutMapping("/bindAuthority")
    public R<?> bindAuthority(@RequestBody @Validated RelationDTO<Long, Long> params) {
        sysRoleAuthorityService.roleBindAuthorities(params);
        return ok();
    }

    @GetMapping("/bindAuthority/{id}")
    public R<?> getBindAuthorities(@PathVariable Long id,
                                   @RequestParam("isBind") boolean isBind,
                                   SysAuthority condition) {
        return ok(sysRoleAuthorityService.getRoleAuthorities(id, isBind, condition));
    }

    @PutMapping("/bindDept")
    public R<?> bindDept(@RequestBody @Validated RelationDTO<Long, Long> params) {
        sysRoleDeptService.roleBindDepts(params);
        return ok();
    }

    @GetMapping("/bindDept/{id}")
    public R<?> getBindDepts(@PathVariable Long id,
                             @RequestParam("isBind") boolean isBind,
                             SysDept condition) {
        return ok(sysRoleDeptService.getRoleDepts(id, isBind, condition));
    }

    @PutMapping("/bindMenu")
    public R<?> bindMenu(@RequestBody @Validated RelationDTO<Long, Long> params) {
        sysRoleMenuService.roleBindMenus(params);
        return ok();
    }

    @GetMapping("/bindMenu/{id}")
    public R<?> getBindMenus(@PathVariable Long id,
                             @RequestParam("isBind") boolean isBind,
                             SysMenu condition) {
        return ok(sysRoleMenuService.getRoleMenus(id, isBind, condition));
    }

    @PutMapping("/bindClient")
    public R<?> bindClient(@RequestBody @Validated RelationDTO<Long, String> params) {
        sysRoleOauthClientService.roleBindClients(params);
        return ok();
    }

    @GetMapping("/bindClient/{id}")
    public R<?> getBindClients(@PathVariable Long id,
                               Page<?> page,
                               @RequestParam("isBind") boolean isBind,
                               Oauth2RegisteredClient condition) {
        return ok(sysRoleOauthClientService.getRoleClients(id, page, isBind, condition));
    }
}
