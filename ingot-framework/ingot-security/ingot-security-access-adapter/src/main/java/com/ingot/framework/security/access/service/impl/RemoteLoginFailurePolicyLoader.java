package com.ingot.framework.security.access.service.impl;

import com.ingot.cloud.security.api.model.vo.policy.LoginFailureProtectionPolicyVO;
import com.ingot.cloud.security.api.rpc.RemoteLoginFailurePolicyService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.access.internal.LoginFailurePolicyRemoteUnavailableException;
import com.ingot.framework.security.access.model.LoginFailurePolicy;
import com.ingot.framework.security.access.service.LoginFailurePolicyLoader;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 远程登录失败策略加载器（Feign delegate）。
 *
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class RemoteLoginFailurePolicyLoader implements LoginFailurePolicyLoader {

    private final RemoteLoginFailurePolicyService remoteService;

    @Override
    public List<LoginFailurePolicy> loadAll() {
        R<List<LoginFailureProtectionPolicyVO>> response;
        try {
            response = remoteService.listPolicies();
        } catch (Exception e) {
            throw new LoginFailurePolicyRemoteUnavailableException("Remote listPolicies error", e);
        }
        if (response == null || !response.isSuccess()) {
            throw new LoginFailurePolicyRemoteUnavailableException(
                    "Remote listPolicies failed, response=" + response);
        }
        List<LoginFailureProtectionPolicyVO> data = response.getData();
        if (data == null || data.isEmpty()) {
            return List.of();
        }
        return data.stream().map(this::toPolicy).toList();
    }

    private LoginFailurePolicy toPolicy(LoginFailureProtectionPolicyVO vo) {
        return new LoginFailurePolicy(
                vo.getDimension(),
                Boolean.TRUE.equals(vo.getEnabled()),
                vo.getMaxAttempts() != null ? vo.getMaxAttempts() : 0,
                vo.getWindowMinutes() != null ? vo.getWindowMinutes() : 1,
                vo.getBlockTtlSec() != null ? vo.getBlockTtlSec() : 60,
                vo.getBlockKeyType()
        );
    }
}
