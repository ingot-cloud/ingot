package com.ingot.cloud.pms.web.v1;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.Oauth2RegisteredClient;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.service.domain.Oauth2RegisteredClientService;
import com.ingot.cloud.pms.service.domain.SysAuthorityService;
import com.ingot.cloud.pms.service.domain.SysDeptService;
import com.ingot.cloud.pms.service.domain.SysRoleAuthorityService;
import com.ingot.cloud.pms.service.domain.SysRoleDeptService;
import com.ingot.cloud.pms.service.domain.SysRoleOauthClientService;
import com.ingot.cloud.pms.service.domain.SysRoleService;
import com.ingot.framework.core.model.common.RelationDTO;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class RoleAPI implements RShortcuts {
    private final SysRoleService sysRoleService;
    private final SysAuthorityService sysAuthorityService;
    private final SysRoleAuthorityService sysRoleAuthorityService;
    private final SysDeptService sysDeptService;
    private final SysRoleDeptService sysRoleDeptService;
    private final SysRoleOauthClientService sysRoleOauthClientService;
    private final Oauth2RegisteredClientService oauth2RegisteredClientService;

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.write', 'basic.role.read')")
    @GetMapping("/options")
    public R<?> options() {
        return ok(sysRoleService.options());
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.write', 'basic.role.read')")
    @GetMapping("/page")
    public R<?> page(Page<SysRole> page, SysRole condition) {
        return ok(sysRoleService.conditionPage(page, condition));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.write')")
    @PostMapping
    public R<?> create(@Validated(Group.Create.class) @RequestBody SysRole params) {
        sysRoleService.createRole(params);
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.write')")
    @PutMapping
    public R<?> update(@Validated(Group.Update.class) @RequestBody SysRole params) {
        sysRoleService.updateRoleById(params);
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.write')")
    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Long id) {
        sysRoleService.removeRoleById(id);
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.write')")
    @PutMapping("/bindAuthority")
    public R<?> bindAuthority(@RequestBody @Validated RelationDTO<Long, Long> params) {
        sysRoleAuthorityService.roleBindAuthorities(params);
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.write')")
    @GetMapping("/bindAuthority/{id}")
    public R<?> getBindAuthorities(@PathVariable Long id,
                                   @RequestParam("isBind") boolean isBind,
                                   SysAuthority condition) {
        if (isBind) {
            return ok(sysRoleAuthorityService.getRoleAuthorities(id, condition));
        }
        return ok(sysAuthorityService.treeList(condition));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.write')")
    @PutMapping("/bindDept")
    public R<?> bindDept(@RequestBody @Validated RelationDTO<Long, Long> params) {
        sysRoleDeptService.roleBindDepts(params);
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.write')")
    @GetMapping("/bindDept/{id}")
    public R<?> getBindDepts(@PathVariable Long id,
                             @RequestParam("isBind") boolean isBind,
                             SysDept condition) {
        if (isBind) {
            return ok(sysRoleDeptService.getRoleDepts(id, condition));
        }
        return ok(sysDeptService.treeList(condition));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.write')")
    @PutMapping("/bindClient")
    public R<?> bindClient(@RequestBody @Validated RelationDTO<Long, String> params) {
        sysRoleOauthClientService.roleBindClients(params);
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.write')")
    @GetMapping("/bindClient/{id}")
    public R<?> getBindClients(@PathVariable Long id,
                               @RequestParam("isBind") boolean isBind,
                               Oauth2RegisteredClient condition) {
        if (isBind) {
            return ok(sysRoleOauthClientService.getRoleClients(id, condition));
        }
        return ok(oauth2RegisteredClientService.list(condition));
    }
}