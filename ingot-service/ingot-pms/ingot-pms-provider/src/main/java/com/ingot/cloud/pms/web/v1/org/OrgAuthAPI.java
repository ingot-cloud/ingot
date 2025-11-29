package com.ingot.cloud.pms.web.v1.org;

import com.ingot.cloud.pms.service.biz.BizOrgService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import com.ingot.framework.tenant.TenantContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : OrgAuthAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/12/1.</p>
 * <p>Time         : 14:55.</p>
 */
@Slf4j
@Tag(description = "OrgAuth", name = "组织权限模块")
@RestController
@RequestMapping(value = "/v1/org/auth")
@RequiredArgsConstructor
public class OrgAuthAPI implements RShortcuts {
    private final BizOrgService bizOrgService;

    @Operation(summary = "获取组织权限", description = "获取组织权限")
    @AdminOrHasAnyAuthority("contacts:auth:query")
    @GetMapping("/tree")
    public R<?> getOrgAuthTree() {
        return ok(bizOrgService.getTenantPermissionTree(TenantContextHolder.get()));
    }
}
