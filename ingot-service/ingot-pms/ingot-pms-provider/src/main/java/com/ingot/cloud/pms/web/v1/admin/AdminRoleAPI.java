package com.ingot.cloud.pms.web.v1.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysRoleGroup;
import com.ingot.cloud.pms.api.model.dto.role.RoleGroupSortDTO;
import com.ingot.cloud.pms.service.domain.SysAuthorityService;
import com.ingot.cloud.pms.service.domain.SysRoleAuthorityService;
import com.ingot.cloud.pms.service.domain.SysRoleService;
import com.ingot.cloud.pms.service.domain.SysRoleUserService;
import com.ingot.framework.core.model.common.RelationDTO;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping(value = "/v1/admin/role")
@RequiredArgsConstructor
public class AdminRoleAPI implements RShortcuts {
    private final SysRoleService sysRoleService;
    private final SysAuthorityService sysAuthorityService;
    private final SysRoleAuthorityService sysRoleAuthorityService;
    private final SysRoleUserService sysRoleUserService;

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.w', 'basic.role.r')")
    @GetMapping("/options")
    public R<?> options() {
        return ok(sysRoleService.options(SecurityAuthContext.isAdmin()));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.w', 'basic.role.r')")
    @GetMapping("/options/{orgId}")
    public R<?> orgOptions(@PathVariable Long orgId) {
        return TenantEnv.applyAs(orgId, () -> ok(sysRoleService.options(SecurityAuthContext.isAdmin())));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.w', 'basic.role.r')")
    @GetMapping("/list")
    public R<?> list(SysRole condition) {
        return ok(sysRoleService.conditionList(condition, SecurityAuthContext.isAdmin()));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.w', 'basic.role.r')")
    @GetMapping("/page")
    public R<?> page(Page<SysRole> page, SysRole condition) {
        return ok(sysRoleService.conditionPage(page, condition, SecurityAuthContext.isAdmin()));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.w', 'basic.role.r')")
    @GetMapping("/group/list")
    public R<?> groupList() {
        return ok(sysRoleService.groupRoleList(SecurityAuthContext.isAdmin()));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.w')")
    @PostMapping
    public R<?> create(@Validated(Group.Create.class) @RequestBody SysRole params) {
        sysRoleService.createRole(params, SecurityAuthContext.isAdmin());
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.w')")
    @PutMapping
    public R<?> update(@Validated(Group.Update.class) @RequestBody SysRole params) {
        sysRoleService.updateRoleById(params, SecurityAuthContext.isAdmin());
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.w')")
    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Long id) {
        sysRoleService.removeRoleById(id, SecurityAuthContext.isAdmin());
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.w')")
    @PostMapping("/group")
    public R<?> createGroup(@RequestBody SysRoleGroup params) {
        sysRoleService.createGroup(params, SecurityAuthContext.isAdmin());
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.w')")
    @PutMapping("/group")
    public R<?> updateGroup(@RequestBody SysRoleGroup params) {
        sysRoleService.updateGroup(params, SecurityAuthContext.isAdmin());
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.w')")
    @DeleteMapping("/group/{id}")
    public R<?> removeGroupById(@PathVariable Long id) {
        sysRoleService.deleteGroup(id, SecurityAuthContext.isAdmin());
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.w')")
    @PutMapping("/group/sort")
    public R<?> groupSort(@RequestBody RoleGroupSortDTO params) {
        sysRoleService.sortGroup(params.getIds());
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.w')")
    @PutMapping("/bindAuthority")
    public R<?> bindAuthority(@RequestBody @Validated RelationDTO<Long, Long> params) {
        sysRoleAuthorityService.roleBindAuthorities(params);
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.w')")
    @GetMapping("/bindAuthority/{id}")
    public R<?> getBindAuthorities(@PathVariable Long id,
                                   @RequestParam("isBind") boolean isBind,
                                   SysAuthority condition) {
        if (isBind) {
            return ok(sysRoleAuthorityService.getRoleAuthorities(id, condition));
        }
        return ok(sysAuthorityService.treeList(condition));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.w')")
    @PutMapping("/bindUser")
    public R<?> bindUser(@RequestBody @Validated RelationDTO<Long, Long> params) {
        sysRoleUserService.roleBindUsers(params);
        return ok();
    }
}
