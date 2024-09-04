package com.ingot.cloud.pms.web.v1.admin;

import com.ingot.cloud.pms.service.biz.BizDeptService;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : AdminDeptAPI.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/10/20.</p>
 * <p>Time         : 11:01 AM.</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/admin/dept")
@RequiredArgsConstructor
public class AdminDeptAPI implements RShortcuts {
    private final BizDeptService bizDeptService;

    @AdminOrHasAnyAuthority({"contacts:member:r", "contacts:dept:w", "contacts:dept:r"})
    @GetMapping("/tree/{orgId}")
    public R<?> tree(@PathVariable Long orgId) {
        return TenantEnv.applyAs(orgId, () -> ok(bizDeptService.orgList()));
    }
}
