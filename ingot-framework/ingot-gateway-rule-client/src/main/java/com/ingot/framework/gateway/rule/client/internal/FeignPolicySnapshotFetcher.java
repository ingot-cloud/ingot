package com.ingot.framework.gateway.rule.client.internal;

import com.ingot.cloud.security.api.model.vo.policy.SecurityPolicySnapshotVO;
import com.ingot.cloud.security.api.rpc.RemoteSecurityPolicyService;
import com.ingot.framework.cache.spi.CacheValueLoader;
import com.ingot.framework.commons.model.support.R;
import lombok.RequiredArgsConstructor;

/**
 * <p>分层缓存链最内层的加载器，通过 Feign 向安全中心拉取全量策略快照。</p>
 *
 * <p>任何调用异常或非 success 响应都包装为 {@link PolicyRemoteUnavailableException} 上抛，
 * 交由上层降级阶梯处理；响应成功但 {@code data} 为空属于合法空，原样返回，
 * 表示「当前确实没有配置」而非故障。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see PolicyRemoteUnavailableException
 */
@RequiredArgsConstructor
public class FeignPolicySnapshotFetcher implements CacheValueLoader<String, SecurityPolicySnapshotVO> {

    private final RemoteSecurityPolicyService remoteService;

    @Override
    public SecurityPolicySnapshotVO load(String key) {
        return fetch();
    }

    /**
     * 直接拉取快照，不经过缓存。
     *
     * @return 远端快照；合法空时 {@code data} 可能为 {@code null}
     * @throws PolicyRemoteUnavailableException 远端不可用
     */
    public SecurityPolicySnapshotVO fetch() {
        try {
            R<SecurityPolicySnapshotVO> response = remoteService.snapshot();
            if (response == null || !response.isSuccess()) {
                throw new PolicyRemoteUnavailableException(
                        "[SecurityPolicy] remote snapshot failed: " + response);
            }
            return response.getData();
        } catch (PolicyRemoteUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new PolicyRemoteUnavailableException("[SecurityPolicy] remote snapshot error", e);
        }
    }
}
