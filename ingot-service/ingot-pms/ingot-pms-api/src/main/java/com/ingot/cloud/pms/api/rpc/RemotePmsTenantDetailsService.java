package com.ingot.cloud.pms.api.rpc;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.security.TenantDetailsResponse;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>Description  : PmsTenantDetailsService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/3.</p>
 * <p>Time         : 16:38.</p>
 */
@FeignClient(contextId = "pmsTenantDetailsService", value = ServiceNameConstants.PMS_SERVICE)
public interface RemotePmsTenantDetailsService {

    @PostMapping(value = "/inner/tenant/details/{username}")
    R<TenantDetailsResponse> getUserTenantDetails(@PathVariable("username") String username);

    @PostMapping("/inner/tenant/detailsList")
    R<TenantDetailsResponse> getTenantByIds(@RequestBody List<Long> ids);

    @GetMapping("/inner/tenant/{id}")
    R<SysTenant> getTenantById(@PathVariable Long id);
}
