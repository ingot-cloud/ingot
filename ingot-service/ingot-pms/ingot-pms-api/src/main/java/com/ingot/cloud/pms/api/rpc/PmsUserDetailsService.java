package com.ingot.cloud.pms.api.rpc;

import com.ingot.framework.core.constants.ServiceNameConstants;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.security.TenantDetailsResponse;
import com.ingot.framework.core.model.security.UserDetailsRequest;
import com.ingot.framework.core.model.security.UserDetailsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * <p>Description  : UserCenterFeignApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/14.</p>
 * <p>Time         : 上午10:04.</p>
 */
@FeignClient(contextId = "pmsUserDetailsService", value = ServiceNameConstants.PMS_SERVICE)
public interface PmsUserDetailsService {

    @PostMapping(value = "/user/details")
    R<UserDetailsResponse> getUserAuthDetails(@RequestBody UserDetailsRequest params);

    @PostMapping(value = "/user/tenant/details/{username}")
    R<TenantDetailsResponse> getUserTenantDetails(@PathVariable("username") String username);
}
