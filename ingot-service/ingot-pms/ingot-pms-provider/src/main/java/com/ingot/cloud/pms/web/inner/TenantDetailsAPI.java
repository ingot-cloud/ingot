package com.ingot.cloud.pms.web.inner;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.service.biz.TenantDetailsService;
import com.ingot.framework.commons.model.security.TenantDetailsResponse;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : TenantDetailsAPI.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/27.</p>
 * <p>Time         : 4:35 PM.</p>
 */
@Slf4j
@Permit(mode = PermitMode.INNER)
@RestController
@RequestMapping(value = "/inner/tenant")
@RequiredArgsConstructor
public class TenantDetailsAPI implements RShortcuts {
    private final TenantDetailsService tenantDetailsService;

    @PostMapping("/details/{username}")
    public R<TenantDetailsResponse> getUserTenantDetails(@PathVariable String username) {
        return ok(tenantDetailsService.getUserTenantDetails(username));
    }

    @PostMapping("/detailsList")
    public R<TenantDetailsResponse> getTenantByIds(@RequestBody List<Long> ids) {
        return ok(tenantDetailsService.getTenantByIds(ids));
    }

    @GetMapping("/{id}")
    public R<SysTenant> getTenantById(@PathVariable Long id) {
        return ok(tenantDetailsService.getTenantById(id));
    }
}
