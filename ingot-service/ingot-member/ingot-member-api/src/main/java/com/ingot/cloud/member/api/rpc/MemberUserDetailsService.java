package com.ingot.cloud.member.api.rpc;

import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.security.TenantDetailsResponse;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>Description  : MemberUserDetailsService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/1.</p>
 * <p>Time         : 14:22.</p>
 */
@FeignClient(contextId = "memberUserDetailsService", value = ServiceNameConstants.MEMBER_SERVICE)
public interface MemberUserDetailsService {

    @PostMapping(value = "/user/details")
    R<UserDetailsResponse> getUserAuthDetails(@RequestBody UserDetailsRequest params);

    @PostMapping(value = "/user/tenant/details/{username}")
    R<TenantDetailsResponse> getUserTenantDetails(@PathVariable("username") String username);
}
