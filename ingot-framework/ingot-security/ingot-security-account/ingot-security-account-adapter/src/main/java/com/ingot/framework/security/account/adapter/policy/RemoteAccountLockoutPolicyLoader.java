package com.ingot.framework.security.account.adapter.policy;

import java.util.List;

import com.ingot.cloud.security.api.model.vo.policy.AccountLockoutPolicyVO;
import com.ingot.cloud.security.api.rpc.RemoteAccountLockoutPolicyService;
import com.ingot.framework.cache.spi.CacheValueLoader;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.account.domain.model.LockoutPolicy;
import lombok.RequiredArgsConstructor;

/**
 * <p>分层缓存最内层加载器，经 Feign 向安全中心拉取账号锁定策略全量快照。</p>
 *
 * <p>调用异常、非 success 响应或成功但列表为空都包装为
 * {@link AccountLockoutPolicyRemoteUnavailableException}，交由降级阶梯处理。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class RemoteAccountLockoutPolicyLoader implements CacheValueLoader<String, List<LockoutPolicy>> {

    private final RemoteAccountLockoutPolicyService remoteService;

    @Override
    public List<LockoutPolicy> load(String key) {
        R<List<AccountLockoutPolicyVO>> response;
        try {
            response = remoteService.listPolicies();
        } catch (Exception e) {
            throw new AccountLockoutPolicyRemoteUnavailableException("Remote listPolicies error", e);
        }
        if (response == null || !response.isSuccess()) {
            throw new AccountLockoutPolicyRemoteUnavailableException(
                    "Remote listPolicies failed, response=" + response);
        }
        List<AccountLockoutPolicyVO> data = response.getData();
        if (data == null || data.isEmpty()) {
            throw new AccountLockoutPolicyRemoteUnavailableException(
                    "Remote listPolicies returned empty list");
        }
        return data.stream().map(this::toPolicy).toList();
    }

    private LockoutPolicy toPolicy(AccountLockoutPolicyVO vo) {
        return new LockoutPolicy(
                vo.getUserType(),
                Boolean.TRUE.equals(vo.getEnabled()),
                vo.getMaxAttempts() != null ? vo.getMaxAttempts() : 0,
                vo.getLockDurationMinutes() != null ? vo.getLockDurationMinutes() : 0,
                vo.getAttemptWindowMinutes() != null ? vo.getAttemptWindowMinutes() : 1,
                vo.getHintAfterAttempts() != null ? vo.getHintAfterAttempts() : 0
        );
    }
}
