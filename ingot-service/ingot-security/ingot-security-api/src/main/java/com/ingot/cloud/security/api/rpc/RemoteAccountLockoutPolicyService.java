package com.ingot.cloud.security.api.rpc;

import java.util.List;

import com.ingot.cloud.security.api.model.vo.policy.AccountLockoutPolicyVO;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * <p>账号锁定策略 Inner Feign，供 IAM / Member 在 {@code mode=remote} 时拉取全量策略。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@FeignClient(contextId = "RemoteAccountLockoutPolicyService", value = ServiceNameConstants.SECURITY_SERVICE)
public interface RemoteAccountLockoutPolicyService {

    /**
     * 拉取全部用户类型的账号锁定策略（固定两行）。
     *
     * @return 包装后的策略列表；调用失败由 Feign 抛出，不在此吞掉
     */
    @GetMapping("/inner/security/account/lockout-policies")
    R<List<AccountLockoutPolicyVO>> listPolicies();
}
