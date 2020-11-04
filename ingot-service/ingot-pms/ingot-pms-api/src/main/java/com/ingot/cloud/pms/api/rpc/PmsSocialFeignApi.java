package com.ingot.cloud.pms.api.rpc;

import com.ingot.cloud.pms.api.model.dto.user.UserAuthDetailDto;
import com.ingot.framework.core.constants.ServiceNameConstants;
import com.ingot.framework.core.constants.TenantConstants;
import com.ingot.framework.core.wrapper.IngotResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>Description  : PmsSocialFeignApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-27.</p>
 * <p>Time         : 09:42.</p>
 */
@FeignClient(contextId = "pmsSocialFeignApi", value = ServiceNameConstants.PMS_SERVICE)
public interface PmsSocialFeignApi {

    @PostMapping(value = "/social/getUserAuthDetail")
    IngotResponse<UserAuthDetailDto> getUserAuthDetail(@RequestParam("type") String type,
                                                       @RequestParam("code") String code,
                                                       @RequestParam("client_id") String clientId,
                                                       @RequestHeader(TenantConstants.TENANT_HEADER_KEY) String tenantCode);
}
