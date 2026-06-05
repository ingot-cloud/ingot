package com.ingot.framework.gateway.rule.client.internal;

import com.ingot.cloud.security.api.model.vo.policy.SecurityPolicySnapshotVO;
import com.ingot.cloud.security.api.rpc.RemoteSecurityPolicyService;
import com.ingot.framework.commons.model.support.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 共享的远端快照拉取器：所有 remote 模式的 ConfigService 通过本类调用 Feign 一次拉全量，
 * 避免每个域分别 RPC。
 *
 * <p>实际缓存由调用方（{@link com.ingot.framework.gateway.rule.client.internal.LocalCompiledCache}）
 * 持有；本类只负责 Feign 调用与异常隔离，失败时返回 {@code null}，由调用方决定降级策略。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteSnapshotFetcher {

    private final RemoteSecurityPolicyService remoteService;

    public SecurityPolicySnapshotVO fetch() {
        try {
            R<SecurityPolicySnapshotVO> response = remoteService.snapshot();
            if (response == null || !response.isSuccess()) {
                log.warn("[SecurityPolicy] remote snapshot failed: {}", response);
                return null;
            }
            return response.getData();
        } catch (Exception e) {
            log.warn("[SecurityPolicy] remote snapshot error", e);
            return null;
        }
    }
}
