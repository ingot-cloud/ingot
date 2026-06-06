package com.ingot.framework.gateway.rule.client.challenge;

import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengeSnapshot;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengeTrigger;
import org.springframework.http.HttpMethod;

/**
 * 挑战策略查询 SPI。
 *
 * <p>网关限流触发（{@link ChallengeTrigger#ON_RATE_LIMIT}）或始终挑战
 *（{@link ChallengeTrigger#ALWAYS}）时，通过本 SPI 查找匹配路径的挑战策略，
 * 决定验证码类型、PassToken 有效期等。</p>
 *
 * <h3>实现与装配</h3>
 * <ul>
 *     <li>{@code policy.mode=local} — {@link com.ingot.framework.gateway.rule.client.challenge.internal.LocalChallengePolicyService}</li>
 *     <li>{@code policy.mode=remote} — {@link com.ingot.framework.gateway.rule.client.challenge.internal.RemoteChallengePolicyService}</li>
 * </ul>
 *
 * <h3>配置开关</h3>
 * <p>需 {@code ingot.security.challenge.enabled=true} 才会装配实现类。
 * yaml 示例见 {@link com.ingot.framework.gateway.rule.client.challenge.config.ChallengeProperties}。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
public interface ChallengePolicyService {

    /**
     * 按请求路径 + HTTP 方法 + 触发类型查找匹配的策略。
     * <p>多条策略命中时按 {@link ChallengePolicy#getPriority()} 升序取第一条。</p>
     *
     * @param requestPath 请求路径（不含 query string）
     * @param method      HTTP 方法，可为 null（视为 ANY）
     * @param trigger     触发类型（ALWAYS / ON_RATE_LIMIT 等）
     * @return 匹配的策略；找不到返回 null
     */
    ChallengePolicy match(String requestPath, HttpMethod method, ChallengeTrigger trigger);

    /**
     * 获取当前挑战策略快照（原始策略列表 + 版本号）。
     */
    ChallengeSnapshot getSnapshot();

    /**
     * 失效本地 L1 编译缓存，下次查询将重新从 yaml 或远端加载。
     * <p>由 Coordinator 在收到 {@code CHALLENGE_POLICY} / {@code ALL} 失效事件时调用。</p>
     */
    void evictAll();

    /**
     * 按 PassToken scope 查找启用中的策略（用于签发 / 校验 PassToken 时关联策略元数据）。
     *
     * @param scope PassToken 作用域，对应策略的 {@link ChallengePolicy#getScope()}
     * @return 首个匹配 scope 的启用策略；未找到返回 null
     */
    default ChallengePolicy findByScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return null;
        }
        ChallengeSnapshot snapshot = getSnapshot();
        if (snapshot == null || snapshot.getPolicies() == null) {
            return null;
        }
        return snapshot.getPolicies().stream()
                .filter(p -> p.isEnabled() && scope.equals(p.getScope()))
                .findFirst()
                .orElse(null);
    }
}
