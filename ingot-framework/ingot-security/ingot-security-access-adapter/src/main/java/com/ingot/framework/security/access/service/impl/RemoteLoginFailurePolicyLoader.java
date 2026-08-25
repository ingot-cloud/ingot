package com.ingot.framework.security.access.service.impl;

import java.util.List;

import com.ingot.cloud.security.api.model.vo.policy.LoginFailureProtectionPolicyVO;
import com.ingot.cloud.security.api.rpc.RemoteLoginFailurePolicyService;
import com.ingot.framework.cache.spi.CacheValueLoader;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.access.internal.LoginFailurePolicyRemoteUnavailableException;
import com.ingot.framework.security.access.model.LoginFailurePolicy;
import lombok.RequiredArgsConstructor;

/**
 * <p>分层缓存链最内层的加载器，经 Feign 向安全中心拉取登录失败四维策略。</p>
 *
 * <p>任何调用异常或非 success 响应都包装为 {@link LoginFailurePolicyRemoteUnavailableException}
 * 上抛，交由上层降级阶梯处理；响应成功但列表为空属于合法空，原样返回空列表。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see LoginFailurePolicyRemoteUnavailableException
 */
@RequiredArgsConstructor
public class RemoteLoginFailurePolicyLoader implements CacheValueLoader<String, List<LoginFailurePolicy>> {

    private final RemoteLoginFailurePolicyService remoteService;

    @Override
    public List<LoginFailurePolicy> load(String key) {
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
