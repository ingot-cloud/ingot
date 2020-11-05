package com.ingot.cloud.pms.api.rpc;

import com.ingot.framework.core.constants.ServiceNameConstants;
import com.ingot.framework.core.model.dto.user.UserAuthDetails;
import com.ingot.framework.core.model.dto.user.UserDetailsDto;
import com.ingot.framework.core.wrapper.IngotResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * <p>Description  : UserCenterFeignApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/14.</p>
 * <p>Time         : 上午10:04.</p>
 */
@FeignClient(contextId = "pmsUserAuthFeignApi", value = ServiceNameConstants.PMS_SERVICE)
public interface PmsUserAuthFeignApi {

    @PostMapping(value = "/user/detail")
    IngotResponse<UserAuthDetails> getUserAuthDetail(@RequestBody UserDetailsDto params);
}
