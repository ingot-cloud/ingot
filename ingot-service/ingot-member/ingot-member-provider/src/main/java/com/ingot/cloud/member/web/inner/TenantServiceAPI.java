package com.ingot.cloud.member.web.inner;

import com.ingot.cloud.member.service.biz.BizTenantService;
import com.ingot.framework.commons.model.common.TenantBaseDTO;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : TenantServiceAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 * <p>Time         : 08:43.</p>
 */
@Slf4j
@Permit(mode = PermitMode.INNER)
@RestController
@RequestMapping(value = "/inner/tenant")
@RequiredArgsConstructor
public class TenantServiceAPI implements RShortcuts {
    private final BizTenantService bizTenantService;

    @DeleteMapping("/{id}")
    public R<Void> deleteTenant(@PathVariable Long id) {
        bizTenantService.deleteTenant(id);
        return ok();
    }

    @PutMapping("/base")
    public R<Void> updateTenantBase(@RequestBody TenantBaseDTO params) {
        bizTenantService.updateBase(params);
        return ok();
    }
}
