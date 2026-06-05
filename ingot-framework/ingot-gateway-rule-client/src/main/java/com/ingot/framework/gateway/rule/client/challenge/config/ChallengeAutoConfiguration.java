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
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(ChallengeProperties.class)
@ConditionalOnProperty(prefix = "ingot.security.challenge",
        name = "enabled", havingValue = "true")
public class ChallengeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChallengePolicyService.class)
    @ConditionalOnProperty(prefix = "ingot.security.challenge.policy",
            name = "mode", havingValue = "local", matchIfMissing = true)
    public ChallengePolicyService localChallengePolicyService(ChallengeProperties properties) {
        log.info("[Challenge] using local challenge policy service");
        return new LocalChallengePolicyService(properties);
    }

    @Bean
    @ConditionalOnMissingBean(ChallengePolicyService.class)
    @ConditionalOnProperty(prefix = "ingot.security.challenge.policy",
            name = "mode", havingValue = "remote")
    public ChallengePolicyService remoteChallengePolicyService(RemoteSnapshotFetcher fetcher) {
        log.info("[Challenge] using remote challenge policy service");
        return new RemoteChallengePolicyService(fetcher);
    }

    @Bean
    static ChallengeCoordinatorRegistrar challengeCoordinatorRegistrar() {
        return new ChallengeCoordinatorRegistrar();
    }

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
