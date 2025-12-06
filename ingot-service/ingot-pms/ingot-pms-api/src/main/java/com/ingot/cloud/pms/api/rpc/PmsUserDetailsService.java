package com.ingot.cloud.pms.api.rpc;

import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
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

    @PostMapping(value = "/inner/user/details")
    R<UserDetailsResponse> getUserAuthDetails(@RequestBody UserDetailsRequest params);
}
