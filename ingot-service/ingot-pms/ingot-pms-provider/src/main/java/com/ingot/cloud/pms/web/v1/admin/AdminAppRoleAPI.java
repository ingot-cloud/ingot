package com.ingot.cloud.pms.web.v1.admin;

import com.ingot.cloud.pms.api.model.domain.AppRole;
import com.ingot.cloud.pms.api.model.domain.AppRoleGroup;
import com.ingot.cloud.pms.api.model.dto.role.RoleFilterDTO;
import com.ingot.cloud.pms.api.model.dto.role.RoleGroupSortDTO;
import com.ingot.cloud.pms.service.biz.BizAppRoleService;
import com.ingot.cloud.pms.service.domain.AppRoleService;
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
 * <p>Description  : AdminAppRoleAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/23.</p>
 * <p>Time         : 12:35.</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/admin/appRole")
@RequiredArgsConstructor
public class AdminAppRoleAPI implements RShortcuts {
    private final AppRoleService appRoleService;
    private final BizAppRoleService bizAppRoleService;

    @PreAuthorize("@ingot.hasAnyAuthority('app.role.w', 'app.role.r')")
    @GetMapping("/options/{orgId}")
    public R<?> orgOptions(@PathVariable Long orgId) {
        return TenantEnv.applyAs(orgId, () -> ok(appRoleService.options(SecurityAuthContext.isAdmin())));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('app.role.w', 'app.role.r')")
    @GetMapping("/list")
    public R<?> list(AppRole condition) {
        return ok(appRoleService.conditionList(condition, SecurityAuthContext.isAdmin()));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('app.role.w', 'app.role.r')")
    @GetMapping("/group/list")
    public R<?> groupList(RoleFilterDTO filter) {
        return ok(appRoleService.groupRoleList(SecurityAuthContext.isAdmin(), filter));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('app.role.w')")
    @PostMapping
    public R<?> create(@Validated(Group.Create.class) @RequestBody AppRole params) {
        bizAppRoleService.createRoleEffectOrg(params, SecurityAuthContext.isAdmin());
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('app.role.w')")
    @PutMapping
    public R<?> update(@Validated(Group.Update.class) @RequestBody AppRole params) {
        bizAppRoleService.updateRoleEffectOrg(params, SecurityAuthContext.isAdmin());
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('app.role.w')")
    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Long id) {
        bizAppRoleService.removeRoleEffectOrg(id, SecurityAuthContext.isAdmin());
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.w')")
    @PostMapping("/group")
    public R<?> createGroup(@RequestBody AppRoleGroup params) {
        bizAppRoleService.createRoleGroupEffectOrg(params, SecurityAuthContext.isAdmin());
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.w')")
    @PutMapping("/group")
    public R<?> updateGroup(@RequestBody AppRoleGroup params) {
        bizAppRoleService.updateRoleGroupEffectOrg(params, SecurityAuthContext.isAdmin());
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.w')")
    @DeleteMapping("/group/{id}")
    public R<?> removeGroupById(@PathVariable Long id) {
        bizAppRoleService.removeRoleGroupEffectOrg(id, SecurityAuthContext.isAdmin());
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.role.w')")
    @PutMapping("/group/sort")
    public R<?> groupSort(@RequestBody RoleGroupSortDTO params) {
        appRoleService.sortGroup(params.getIds());
        return ok();
    }
}
