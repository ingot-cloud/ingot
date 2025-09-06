package com.ingot.cloud.pms.web.v1.org;

import com.ingot.cloud.pms.api.model.dto.authority.AuthorityFilterDTO;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.framework.commons.model.common.RelationDTO;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import com.ingot.framework.tenant.TenantContextHolder;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : OrgAuthAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/12/1.</p>
 * <p>Time         : 14:55.</p>
 */
@Slf4j
@Tag(description = "orgAuth", name = "组织权限模块")
@RestController
@RequestMapping(value = "/v1/org/auth")
@RequiredArgsConstructor
public class OrgAuthAPI implements RShortcuts {
    private final BizRoleService bizRoleService;

    @AdminOrHasAnyAuthority("contacts:auth")
    @GetMapping("/list")
    public R<?> getOrgAuth() {
        return ok(bizRoleService.getOrgAuthority(TenantContextHolder.get()));
    }

    @AdminOrHasAnyAuthority("contacts:auth")
    @PutMapping("/bindAuthority")
    public R<?> bindAuthority(@RequestBody @Validated RelationDTO<Long, Long> params) {
        bizRoleService.orgRoleBindAuthorities(params);
        return ok();
    }

    @AdminOrHasAnyAuthority("contacts:auth")
    @GetMapping("/bindAuthority/{id}")
    public R<?> getBindAuthorities(@PathVariable Long id,
                                   AuthorityFilterDTO condition) {
        return ok(bizRoleService.getOrgRoleAuthorities(id, condition));
    }
}
