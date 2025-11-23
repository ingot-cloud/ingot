package com.ingot.cloud.pms.web.v1.admin;

import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysRoleGroup;
import com.ingot.cloud.pms.api.model.dto.authority.AuthorityFilterDTO;
import com.ingot.cloud.pms.api.model.dto.role.RoleFilterDTO;
import com.ingot.cloud.pms.service.biz.BizOrgRoleServiceOld;
import com.ingot.cloud.pms.service.domain.SysAuthorityService;
import com.ingot.cloud.pms.service.domain.SysRoleAuthorityService;
import com.ingot.cloud.pms.service.domain.SysRoleService;
import com.ingot.framework.commons.model.common.AssignDTO;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.security.access.HasAnyAuthority;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.tenant.TenantEnv;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
@Tag(description = "role", name = "系统角色管理模块")
@RequestMapping(value = "/v1/admin/role")
@RequiredArgsConstructor
public class AdminRoleAPI implements RShortcuts {
    private final SysRoleService sysRoleService;
    private final SysAuthorityService sysAuthorityService;
    private final SysRoleAuthorityService sysRoleAuthorityService;
    private final BizOrgRoleServiceOld bizOrgRoleServiceOld;

    @HasAnyAuthority({"basic:role:w", "basic:role:r"})
    @GetMapping("/options")
    public R<?> options() {
        return ok(sysRoleService.options(SecurityAuthContext.isAdmin()));
    }

    @HasAnyAuthority({"basic:role:w", "basic:role:r"})
    @GetMapping("/options/{orgId}")
    public R<?> orgOptions(@PathVariable Long orgId) {
        return TenantEnv.applyAs(orgId, () -> ok(sysRoleService.options(SecurityAuthContext.isAdmin())));
    }

    @HasAnyAuthority({"basic:role:w", "basic:role:r"})
    @GetMapping("/list")
    public R<?> list(SysRole condition) {
        return ok(sysRoleService.conditionList(condition, SecurityAuthContext.isAdmin()));
    }

    @HasAnyAuthority({"basic:role:w", "basic:role:r"})
    @GetMapping("/group/list")
    public R<?> groupList(RoleFilterDTO filter) {
        return ok(sysRoleService.groupRoleList(SecurityAuthContext.isAdmin(), filter));
    }

    @HasAnyAuthority({"basic:role:w"})
    @PostMapping
    public R<?> create(@Validated(Group.Create.class) @RequestBody SysRole params) {
        bizOrgRoleServiceOld.createRoleEffectOrg(params, SecurityAuthContext.isAdmin());
        return ok();
    }

    @HasAnyAuthority({"basic:role:w"})
    @PutMapping
    public R<?> update(@Validated(Group.Update.class) @RequestBody SysRole params) {
        bizOrgRoleServiceOld.updateRoleEffectOrg(params, SecurityAuthContext.isAdmin());
        return ok();
    }

    @HasAnyAuthority({"basic:role:w"})
    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Long id) {
        bizOrgRoleServiceOld.removeRoleEffectOrg(id, SecurityAuthContext.isAdmin());
        return ok();
    }

    /**
     * 角色绑定权限，绑定关系会清除，重新绑定，只处理#params.getBindIds()中的值
     */
    @HasAnyAuthority({"basic:role:w"})
    @PutMapping("/bindAuthority")
    public R<?> bindAuthority(@RequestBody @Validated AssignDTO<Long, Long> params) {
        bizOrgRoleServiceOld.roleBindAuthoritiesEffectOrg(params);
        return ok();
    }

    /**
     * 组织角色绑定默认权限
     */
    @HasAnyAuthority({"basic:role:w"})
    @PutMapping("/orgRoleBindDefaultAuthority")
    public R<?> orgRoleBindDefaultAuthorities(@RequestBody AssignDTO<Long, Long> params) {
        bizOrgRoleServiceOld.orgRoleBindDefaultAuthorities(params);
        return ok();
    }

    @HasAnyAuthority({"basic:role:w"})
    @GetMapping("/bindAuthority/{id}")
    public R<?> getBindAuthorities(@PathVariable Long id,
                                   @RequestParam("isBind") boolean isBind,
                                   AuthorityFilterDTO condition) {
        if (isBind) {
            return ok(sysRoleAuthorityService.getRoleAuthorities(id, condition));
        }
        return ok(sysAuthorityService.treeList(condition));
    }
}
