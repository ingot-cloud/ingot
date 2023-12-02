package com.ingot.cloud.pms.web.v1.org;

import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.domain.SysRoleAuthorityService;
import com.ingot.framework.core.model.common.RelationDTO;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : OrgAuthAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/12/1.</p>
 * <p>Time         : 14:55.</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/org/auth")
@RequiredArgsConstructor
public class OrgAuthAPI implements RShortcuts {
    private final BizRoleService bizRoleService;
    private final SysRoleAuthorityService sysRoleAuthorityService;

    @PreAuthorize("@ingot.adminOrHasAnyAuthority('contacts.auth')")
    @GetMapping("/list")
    public R<?> getOrgAuth() {
        return ok(bizRoleService.getOrgAuthority(TenantContextHolder.get()));
    }

    @PreAuthorize("@ingot.adminOrHasAnyAuthority('contacts.auth')")
    @PutMapping("/bindAuthority")
    public R<?> bindAuthority(@RequestBody @Validated RelationDTO<Long, Long> params) {
        sysRoleAuthorityService.roleBindAuthorities(params);
        return ok();
    }

    @PreAuthorize("@ingot.adminOrHasAnyAuthority('contacts.auth')")
    @GetMapping("/bindAuthority/{id}")
    public R<?> getBindAuthorities(@PathVariable Long id,
                                   SysAuthority condition) {
        return ok(bizRoleService.getOrgRoleAuthorities(id, condition));
    }
}
