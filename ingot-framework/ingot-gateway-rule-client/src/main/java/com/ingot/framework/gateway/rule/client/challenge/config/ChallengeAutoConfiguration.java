package com.ingot.framework.gateway.rule.client.challenge.config;

import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.framework.gateway.rule.client.challenge.ChallengePolicyService;
import com.ingot.framework.gateway.rule.client.challenge.internal.LocalChallengePolicyService;
import com.ingot.framework.gateway.rule.client.challenge.internal.RemoteChallengePolicyService;
import com.ingot.framework.gateway.rule.client.internal.RemoteSnapshotFetcher;
import com.ingot.framework.gateway.rule.client.internal.SecurityPolicyCacheCoordinator;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 挑战域 SDK 自动配置。
 *
 * <p>装配条件：{@code ingot.security.challenge.enabled=true}。</p>
 *
 * <ul>
 *     <li>{@code policy.mode=local}（默认）— 装配 {@link LocalChallengePolicyService}，
 *         策略来自 {@link ChallengeProperties.Policy#getPolicies()} / yaml。</li>
 *     <li>{@code policy.mode=remote} — 装配 {@link RemoteChallengePolicyService}，
 *         通过 {@link RemoteSnapshotFetcher} 调 ingot-service-security 的
 *         {@code /inner/security/policy/snapshot} 拉取 {@code challengePolicies}。</li>
 * </ul>
 *
 * <p>向 {@link SecurityPolicyCacheCoordinator} 注册 {@code CHALLENGE_POLICY} 域 evictor，
 * Platform 改策略后各节点 {@code evictAll()} 清空 L1 编译索引。</p>
 *
 * <h3>典型配置 — local 模式</h3>
 *
 * <pre>{@code
 * ingot:
 *   security:
 *     challenge:
 *       enabled: true
 *       policy:
 *         mode: local
 *         groups:
 *           - code: login-flow
 *             pattern-list:
 *               - path: /auth/token
 *                 method: POST
 *         policies:
 *           - code: login-always
 *             group-code: login-flow
 *             trigger: ALWAYS
 *             challenge-type: SLIDER
 *             scope: login
 *             enabled: true
 * }</pre>
 *
 * <h3>典型配置 — remote 模式</h3>
 *
 * <pre>{@code
 * ingot:
 *   security:
 *     policy:
 *       client:
 *         enabled: true
 *         invalidation-enabled: true
 *     challenge:
 *       enabled: true
 *       policy:
 *         mode: remote
 * }</pre>
 *
 * <p>yaml 完整示例见 {@link ChallengeProperties}。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(ChallengeProperties.class)
@ConditionalOnProperty(prefix = "ingot.security.challenge",
        name = "enabled", havingValue = "true")
public class ChallengeAutoConfiguration {

    /**
     * local 模式挑战策略服务。
     * <p>激活条件：{@code policy.mode=local}（默认）且容器中尚无 {@link ChallengePolicyService} Bean。</p>
     */
    @Bean
    @ConditionalOnMissingBean(ChallengePolicyService.class)
    @ConditionalOnProperty(prefix = "ingot.security.challenge.policy",
            name = "mode", havingValue = "local", matchIfMissing = true)
    public ChallengePolicyService localChallengePolicyService(ChallengeProperties properties) {
        log.info("[Challenge] using local challenge policy service");
        return new LocalChallengePolicyService(properties);
    }

    /**
     * remote 模式挑战策略服务。
     * <p>激活条件：{@code policy.mode=remote} 且存在 {@link RemoteSnapshotFetcher} Bean。</p>
     */
    @Bean
    @ConditionalOnMissingBean(ChallengePolicyService.class)
    @ConditionalOnProperty(prefix = "ingot.security.challenge.policy",
            name = "mode", havingValue = "remote")
    public ChallengePolicyService remoteChallengePolicyService(RemoteSnapshotFetcher fetcher) {
        log.info("[Challenge] using remote challenge policy service");
        return new RemoteChallengePolicyService(fetcher);
    }

    /**
     * 挑战域缓存失效注册器。
     */
    @Bean
    static ChallengeCoordinatorRegistrar challengeCoordinatorRegistrar() {
        return new ChallengeCoordinatorRegistrar();
    }

    /**
     * 在 {@link jakarta.annotation.PostConstruct} 阶段把
     * {@link ChallengePolicyService#evictAll()} 挂到 Coordinator 的 {@code CHALLENGE_POLICY} 域。
     */
    static class ChallengeCoordinatorRegistrar {

        @Autowired(required = false)
        private SecurityPolicyCacheCoordinator coordinator;

        @Autowired(required = false)
        private ChallengePolicyService challengePolicyService;

        @PostConstruct
        public void register() {
            if (coordinator == null || challengePolicyService == null) return;
            coordinator.register(SecurityPolicyDomain.CHALLENGE_POLICY,
                    challengePolicyService::evictAll);
        }
    }
}
