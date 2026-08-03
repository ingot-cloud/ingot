package com.ingot.cloud.security.api.rpc;

import com.ingot.cloud.security.api.model.vo.policy.LoginFailureProtectionPolicyVO;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * 登录失败保护策略 Feign 接口。
 *
 * @author jy
 * @since 1.0.0
 */
@FeignClient(contextId = "RemoteLoginFailurePolicyService", value = ServiceNameConstants.SECURITY_SERVICE)
public interface RemoteLoginFailurePolicyService {

    /**
     * 获取全量登录失败保护策略（最多四维）。
     */
    @GetMapping("/inner/security/access/login-failure-policies")
    R<List<LoginFailureProtectionPolicyVO>> listPolicies();
}
