package com.ingot.framework.gateway.rule.client.violation.config;

import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.framework.commons.model.security.PolicySourceMode;
import com.ingot.framework.gateway.rule.client.internal.LocalPolicyEnvironmentRefreshListener;
import com.ingot.framework.gateway.rule.client.internal.RemoteSnapshotFetcher;
import com.ingot.framework.gateway.rule.client.internal.SecurityPolicyCacheCoordinator;
import com.ingot.framework.gateway.rule.client.violation.ViolationEscalationService;
import com.ingot.framework.gateway.rule.client.violation.internal.LocalViolationEscalationService;
import com.ingot.framework.gateway.rule.client.violation.internal.RemoteViolationEscalationService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 违规升级域 SDK 自动配置。
 *
 * <p>装配条件：{@code ingot.security.violation-escalation.enabled=true}。</p>
 *
 * @author jy
 * @since 2026/6/5
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(ViolationEscalationProperties.class)
@ConditionalOnProperty(prefix = "ingot.security.violation-escalation",
        name = "enabled", havingValue = "true")
public class ViolationEscalationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ViolationEscalationService.class)
    @ConditionalOnProperty(prefix = "ingot.security.violation-escalation.policy",
            name = "mode", havingValue = PolicySourceMode.VALUE_LOCAL, matchIfMissing = true)
    public ViolationEscalationService localViolationEscalationService(ViolationEscalationProperties properties) {
        log.info("[ViolationEscalation] using local service");
        return new LocalViolationEscalationService(properties);
    }

    @Bean
    @ConditionalOnMissingBean(ViolationEscalationService.class)
    @ConditionalOnProperty(prefix = "ingot.security.violation-escalation.policy",
            name = "mode", havingValue = PolicySourceMode.VALUE_REMOTE)
    public ViolationEscalationService remoteViolationEscalationService(RemoteSnapshotFetcher fetcher) {
        log.info("[ViolationEscalation] using remote service");
        return new RemoteViolationEscalationService(fetcher);
    }

    @Bean
    static ViolationEscalationCoordinatorRegistrar violationEscalationCoordinatorRegistrar() {
        return new ViolationEscalationCoordinatorRegistrar();
    }

    @RequiredArgsConstructor
    static class ViolationEscalationCoordinatorRegistrar {

        @Autowired(required = false)
        private SecurityPolicyCacheCoordinator coordinator;

        @Autowired(required = false)
        private ViolationEscalationService violationEscalationService;

        @Autowired(required = false)
        private LocalPolicyEnvironmentRefreshListener refreshListener;

        @Autowired(required = false)
        private ViolationEscalationProperties properties;

        @PostConstruct
        public void register() {
            if (coordinator != null && violationEscalationService != null) {
                coordinator.register(SecurityPolicyDomain.VIOLATION_ESCALATION,
                        violationEscalationService::evictAll);
            }
            if (refreshListener != null && violationEscalationService != null && properties != null
                    && properties.getPolicy().getMode() == PolicySourceMode.LOCAL) {
                refreshListener.register("ingot.security.violation-escalation.",
                        violationEscalationService::evictAll);
            }
        }
    }
}
