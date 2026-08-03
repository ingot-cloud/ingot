package com.ingot.framework.gateway.rule.client.internal;

import com.ingot.cloud.security.api.model.vo.policy.SecurityPolicySnapshotVO;
import com.ingot.framework.cache.spi.CacheFloorSupplier;
import com.ingot.framework.gateway.rule.client.blacklist.config.BlacklistProperties;
import com.ingot.framework.gateway.rule.client.challenge.config.ChallengeProperties;
import com.ingot.framework.gateway.rule.client.ratelimit.config.RateLimitProperties;
import com.ingot.framework.gateway.rule.client.violation.config.ViolationEscalationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * <p>Nacos 本地地板：按域聚合各自的 {@code *Properties}，组装为全量快照 VO。</p>
 *
 * <p>四个域均以 {@link ObjectProvider} 延迟解析，因此本类<b>不感知</b>域开关：某域
 * {@code ingot.security.<domain>.enabled=false} 时其 Properties Bean 不存在，
 * 对应地板片段自然为空，地板内容与域开关始终一致。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see PolicySnapshotFloorAssembler
 * @apiNote 每次调用都重新解析并组装，从而反映 Nacos 动态刷新后的最新属性值。
 */
@RequiredArgsConstructor
public class LocalPolicyFloorSupplier implements CacheFloorSupplier<String, SecurityPolicySnapshotVO> {

    private final ObjectProvider<RateLimitProperties> rateLimitProvider;
    private final ObjectProvider<BlacklistProperties> blacklistProvider;
    private final ObjectProvider<ChallengeProperties> challengeProvider;
    private final ObjectProvider<ViolationEscalationProperties> violationEscalationProvider;

    @Override
    public SecurityPolicySnapshotVO get(String key) {
        return get();
    }

    /**
     * 组装当前地板快照。
     *
     * @return 非空快照
     */
    public SecurityPolicySnapshotVO get() {
        return PolicySnapshotFloorAssembler.fromLocal(
                rateLimitProvider.getIfAvailable(),
                blacklistProvider.getIfAvailable(),
                challengeProvider.getIfAvailable(),
                violationEscalationProvider.getIfAvailable());
    }
}
