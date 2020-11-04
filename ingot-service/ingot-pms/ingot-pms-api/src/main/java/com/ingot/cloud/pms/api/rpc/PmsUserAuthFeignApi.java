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
 * <p>Description  : UserCenterFeignApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/14.</p>
 * <p>Time         : 上午10:04.</p>
 */
@FeignClient(contextId = "pmsUserAuthFeignApi", value = ServiceNameConstants.PMS_SERVICE)
public interface PmsUserAuthFeignApi {

    @PostMapping(value = "/user/detail")
    IngotResponse<UserAuthDetailDto> getUserAuthDetail(@RequestParam("username") String username,
                                                       @RequestParam("client_id") String clientId,
                                                       @RequestHeader(TenantConstants.TENANT_HEADER_KEY) String tenantCode);
}
