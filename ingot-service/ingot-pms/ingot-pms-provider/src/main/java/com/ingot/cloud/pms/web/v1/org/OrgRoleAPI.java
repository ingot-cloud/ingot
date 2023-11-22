package com.ingot.cloud.pms.web.v1.org;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysRoleGroup;
import com.ingot.cloud.pms.api.model.dto.role.RoleFilterDTO;
import com.ingot.cloud.pms.api.model.dto.role.RoleGroupSortDTO;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.domain.SysAuthorityService;
import com.ingot.cloud.pms.service.domain.SysRoleAuthorityService;
import com.ingot.cloud.pms.service.domain.SysRoleService;
import com.ingot.framework.core.model.common.RelationDTO;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : OrgRoleAPI.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/22.</p>
 * <p>Time         : 2:40 PM.</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/org/role")
@RequiredArgsConstructor
public class OrgRoleAPI implements RShortcuts {
    private final SysRoleService sysRoleService;
    private final SysAuthorityService sysAuthorityService;
    private final SysRoleAuthorityService sysRoleAuthorityService;
    private final BizRoleService bizRoleService;
    
    @PreAuthorize("@ingot.adminOrHasAnyAuthority('contacts.role.r', 'contacts.role.w')")
    @GetMapping("/options")
    public R<?> options() {
        return ok(sysRoleService.options(false));
    }

    @PreAuthorize("@ingot.adminOrHasAnyAuthority('contacts.role.r', 'contacts.role.w')")
    @GetMapping("/list")
    public R<?> list(SysRole condition) {
        return ok(sysRoleService.conditionList(condition, false));
    }

    @PreAuthorize("@ingot.adminOrHasAnyAuthority('contacts.role.r', 'contacts.role.w')")
    @GetMapping("/page")
    public R<?> page(Page<SysRole> page, SysRole condition) {
        return ok(sysRoleService.conditionPage(page, condition, false));
    }

    @PreAuthorize("@ingot.adminOrHasAnyAuthority('contacts.role.r', 'contacts.role.w')")
    @GetMapping("/group/list")
    public R<?> groupList(RoleFilterDTO filter) {
        return ok(sysRoleService.groupRoleList(false, filter));
    }

    @PreAuthorize("@ingot.adminOrHasAnyAuthority('contacts.role.w')")
    @PostMapping
    public R<?> create(@Validated(Group.Create.class) @RequestBody SysRole params) {
        sysRoleService.createRole(params, false);
        return ok();
    }

    @PreAuthorize("@ingot.adminOrHasAnyAuthority('contacts.role.w')")
    @PutMapping
    public R<?> update(@Validated(Group.Update.class) @RequestBody SysRole params) {
        sysRoleService.updateRoleById(params, false);
        return ok();
    }

    @PreAuthorize("@ingot.adminOrHasAnyAuthority('contacts.role.w')")
    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Long id) {
        sysRoleService.removeRoleById(id, false);
        return ok();
    }

    @PreAuthorize("@ingot.adminOrHasAnyAuthority('contacts.role.w')")
    @PostMapping("/group")
    public R<?> createGroup(@RequestBody SysRoleGroup params) {
        sysRoleService.createGroup(params, false);
        return ok();
    }

    @PreAuthorize("@ingot.adminOrHasAnyAuthority('contacts.role.w')")
    @PutMapping("/group")
    public R<?> updateGroup(@RequestBody SysRoleGroup params) {
        sysRoleService.updateGroup(params, false);
        return ok();
    }

    @PreAuthorize("@ingot.adminOrHasAnyAuthority('contacts.role.w')")
    @DeleteMapping("/group/{id}")
    public R<?> removeGroupById(@PathVariable Long id) {
        sysRoleService.deleteGroup(id, false);
        return ok();
    }

    @PreAuthorize("@ingot.adminOrHasAnyAuthority('contacts.role.w')")
    @PutMapping("/group/sort")
    public R<?> groupSort(@RequestBody RoleGroupSortDTO params) {
        sysRoleService.sortGroup(params.getIds());
        return ok();
    }

    @PreAuthorize("@ingot.adminOrHasAnyAuthority('contacts.role.w')")
    @PutMapping("/bindAuthority")
    public R<?> bindAuthority(@RequestBody @Validated RelationDTO<Long, Long> params) {
        sysRoleAuthorityService.roleBindAuthorities(params);
        return ok();
    }

    @PreAuthorize("@ingot.adminOrHasAnyAuthority('contacts.role.w')")
    @GetMapping("/bindAuthority/{id}")
    public R<?> getBindAuthorities(@PathVariable Long id,
                                   @RequestParam("isBind") boolean isBind,
                                   SysAuthority condition) {
        if (isBind) {
            return ok(sysRoleAuthorityService.getRoleAuthorities(id, condition));
        }
        return ok(sysAuthorityService.treeList(condition));
    }

    @PreAuthorize("@ingot.adminOrHasAnyAuthority('contacts.role.w')")
    @PutMapping("/bindUser")
    public R<?> bindUser(@RequestBody @Validated RelationDTO<Long, Long> params) {
        bizRoleService.orgRoleBindUsers(params);
        return ok();
    }
}
