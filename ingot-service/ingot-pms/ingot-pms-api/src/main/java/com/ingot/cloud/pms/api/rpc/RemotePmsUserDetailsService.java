package com.ingot.cloud.pms.api.rpc;

import java.util.List;

import com.ingot.cloud.pms.api.model.dto.user.InnerUserDTO;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
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
public interface RemotePmsUserDetailsService {

    @PostMapping(value = "/inner/user/details")
    R<UserDetailsResponse> getUserAuthDetails(@RequestBody UserDetailsRequest params);

    @GetMapping("/inner/user/{id}")
    R<InnerUserDTO> getUserInfo(@PathVariable Long id);

    @GetMapping("/inner/user/list")
    R<List<InnerUserDTO>> getAllUserInfo(@RequestBody List<Long> ids);
}
