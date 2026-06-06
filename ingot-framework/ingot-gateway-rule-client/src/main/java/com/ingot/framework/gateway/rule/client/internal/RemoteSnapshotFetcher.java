package com.ingot.framework.gateway.rule.client.internal;

import com.ingot.cloud.security.api.model.vo.policy.SecurityPolicySnapshotVO;
import com.ingot.cloud.security.api.rpc.RemoteSecurityPolicyService;
import com.ingot.framework.commons.model.support.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 共享的远端安全策略快照拉取器。
 *
 * <p>所有 {@code policy.mode=remote} 域（限流 / 黑白名单 / 挑战）通过本类调用 Feign
 * 一次拉取全量快照（{@code GET /inner/security/policy/snapshot}），避免每个域分别 RPC。</p>
 *
 * <h3>装配条件</h3>
 * <ul>
 *     <li>{@code ingot.security.policy.client.enabled=true}</li>
 *     <li>classpath 存在 {@link RemoteSecurityPolicyService} 且已注册为 Spring Bean</li>
 *     <li>至少一个域配置了 {@code policy.mode=remote}</li>
 * </ul>
 *
 * <p>实际 L1 缓存由调用方（{@link LocalCompiledCache}）持有；本类只负责 Feign 调用与异常隔离，
 * 失败时返回 {@code null}，由各领域 Service 降级为空快照 / 空索引（不抛异常）。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteSnapshotFetcher {

    private final RemoteSecurityPolicyService remoteService;

    /**
     * 拉取全量安全策略快照。
     *
     * @return 成功时返回快照 VO；Feign 失败、响应非 success 或异常时返回 null
     */
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
