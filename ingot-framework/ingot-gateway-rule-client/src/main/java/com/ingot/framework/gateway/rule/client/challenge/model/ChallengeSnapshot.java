package com.ingot.framework.gateway.rule.client.challenge.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 挑战策略快照：某一时刻完整的策略列表 + 版本号。
 *
 * <p>由 {@link com.ingot.framework.gateway.rule.client.challenge.ChallengePolicyService}
 * 持有并编译为 {@link com.ingot.framework.gateway.rule.client.challenge.internal.CompiledChallengePolicy}；
 * local 模式从 yaml 组装，remote 模式经 Inner Feign 拉取后转换。</p>
 *
 * <p>跨节点规则变更时，{@link com.ingot.framework.gateway.rule.client.internal.SecurityPolicyCacheCoordinator}
 * 根据 {@link #version} 触发 evict + reload，保证各网关节点策略一致。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeSnapshot implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前生效的挑战策略列表。
     * 仅包含 {@link ChallengePolicy#isEnabled()} 为 {@code true} 的条目会进入编译缓存；
     * 空列表表示无挑战策略（网关不发起 412 挑战）。
     */
    private List<ChallengePolicy> policies;

    /**
     * 单调递增版本号，用于一致性校验与跨节点失效广播。
     * local 模式通常为配置加载时的本地计数；remote 模式与 Platform 快照版本对齐。
     * {@code 0} 表示初始空快照。
     */
    private long version;

    public static ChallengeSnapshot empty() {
        return new ChallengeSnapshot(Collections.emptyList(), 0L);
    }
}
