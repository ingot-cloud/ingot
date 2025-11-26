package com.ingot.cloud.pms.web.v1.platform.system;

import java.util.List;

import com.ingot.cloud.pms.api.model.vo.role.RoleTreeNodeVO;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import com.ingot.framework.tenant.TenantEnv;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : SystemRoleAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/26.</p>
 * <p>Time         : 10:41.</p>
 */
@Slf4j
@Tag(description = "SystemRole", name = "系统角色管理模块")
@RestController
@RequestMapping(value = "/v1/platform/system/role")
@RequiredArgsConstructor
public class SystemRoleAPI implements RShortcuts {
    private final BizRoleService bizRoleService;

    @AdminOrHasAnyAuthority({"platform:system:role:query"})
    @Operation(summary = "角色树", description = "角色树")
    @GetMapping("/tree/{orgId}")
    public R<List<RoleTreeNodeVO>> options(@PathVariable Long orgId) {
        return ok(TenantEnv.applyAs(orgId, () -> bizRoleService.conditionTree(null)));
    }

}
