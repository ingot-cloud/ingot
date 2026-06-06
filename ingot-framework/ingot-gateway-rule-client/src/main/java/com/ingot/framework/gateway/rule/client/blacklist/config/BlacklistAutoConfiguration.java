package com.ingot.framework.gateway.rule.client.blacklist.config;

import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.framework.gateway.rule.client.blacklist.BlacklistService;
import com.ingot.framework.gateway.rule.client.blacklist.internal.LocalBlacklistService;
import com.ingot.framework.gateway.rule.client.blacklist.internal.RemoteBlacklistService;
import com.ingot.framework.gateway.rule.client.internal.RemoteSnapshotFetcher;
import com.ingot.framework.gateway.rule.client.internal.SecurityPolicyCacheCoordinator;
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
 * 黑白名单域 SDK 自动配置。
 *
 * <p>装配条件：{@code ingot.security.blacklist.enabled=true}。</p>
 *
 * <ul>
 *     <li>{@code policy.mode=local} — {@link LocalBlacklistService}，名单来自 yaml
 *         {@link BlacklistProperties.Policy#getItems()}</li>
 *     <li>{@code policy.mode=remote} — {@link RemoteBlacklistService}，Feign 拉
 *         {@code /inner/security/policy/snapshot} 中的 {@code ipList}</li>
 * </ul>
 *
 * <p>向 {@link SecurityPolicyCacheCoordinator} 注册 {@code IP_LIST} 域 evictor，
 * Platform 改名单后各节点 {@code evictAll()} 清空 L1 编译索引。</p>
 *
 * <p>yaml 配置示例见 {@link BlacklistProperties}。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(BlacklistProperties.class)
@ConditionalOnProperty(prefix = "ingot.security.blacklist",
        name = "enabled", havingValue = "true")
public class BlacklistAutoConfiguration {

    /**
     * local 模式黑白名单服务。
     * <p>激活条件：{@code policy.mode=local}（默认）且容器中尚无 {@link BlacklistService} Bean。</p>
     */
    @Bean
    @ConditionalOnMissingBean(BlacklistService.class)
    @ConditionalOnProperty(prefix = "ingot.security.blacklist.policy",
            name = "mode", havingValue = "local", matchIfMissing = true)
    public BlacklistService localBlacklistService(BlacklistProperties properties) {
        log.info("[Blacklist] using local blacklist service");
        return new LocalBlacklistService(properties);
    }

    /**
     * remote 模式黑白名单服务。
     * <p>激活条件：{@code policy.mode=remote} 且存在 {@link RemoteSnapshotFetcher} Bean。</p>
     */
    @Bean
    @ConditionalOnMissingBean(BlacklistService.class)
    @ConditionalOnProperty(prefix = "ingot.security.blacklist.policy",
            name = "mode", havingValue = "remote")
    public BlacklistService remoteBlacklistService(RemoteSnapshotFetcher fetcher) {
        log.info("[Blacklist] using remote blacklist service");
        return new RemoteBlacklistService(fetcher);
    }

    /**
     * 黑白名单域缓存失效注册器。
     */
    @Bean
    static BlacklistCoordinatorRegistrar blacklistCoordinatorRegistrar() {
        return new BlacklistCoordinatorRegistrar();
    }

    /**
     * 在 {@link jakarta.annotation.PostConstruct} 阶段把
     * {@link BlacklistService#evictAll()} 挂到 Coordinator 的 {@code IP_LIST} 域。
     */
    @RequiredArgsConstructor
    static class BlacklistCoordinatorRegistrar {

        @Autowired(required = false)
        private SecurityPolicyCacheCoordinator coordinator;

        @Autowired(required = false)
        private BlacklistService blacklistService;

        @PostConstruct
        public void register() {
            if (coordinator == null || blacklistService == null) {
                return;
            }
            coordinator.register(SecurityPolicyDomain.IP_LIST, blacklistService::evictAll);
        }
    }
}
