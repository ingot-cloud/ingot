package com.ingot.cloud.pms.web.inner;

import com.ingot.cloud.pms.service.biz.TenantDetailsService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import com.ingot.framework.commons.model.security.TenantDetailsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : TenantDetailsAPI.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/27.</p>
 * <p>Time         : 4:35 PM.</p>
 */
@Slf4j
@Permit(mode = PermitMode.INNER)
@RestController
@RequestMapping(value = "/user/tenant/details")
@RequiredArgsConstructor
public class TenantDetailsAPI implements RShortcuts {
    private final TenantDetailsService tenantDetailsService;

    @PostMapping("/{username}")
    public R<TenantDetailsResponse> getUserTenantDetails(@PathVariable String username) {
        return ok(tenantDetailsService.getUserTenantDetails(username));
    }
}
