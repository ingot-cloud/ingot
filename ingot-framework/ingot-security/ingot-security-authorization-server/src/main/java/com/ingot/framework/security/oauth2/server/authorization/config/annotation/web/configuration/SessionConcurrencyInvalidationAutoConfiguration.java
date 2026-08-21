package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration;

import java.util.List;

import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.cloud.security.api.model.vo.policy.SessionConcurrencyPolicyVO;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.commons.model.security.PolicySourceMode;
import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.config.EventBusAutoConfiguration;
import com.ingot.framework.security.oauth2.server.authorization.session.concurrency.SessionConcurrencyCacheCoordinator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * <p>并发会话策略失效广播的订阅装配，使安全中心改策略后各 Auth 实例秒级生效。</p>
 *
 * <p>刻意做成自动配置而非放进 {@link SessionConcurrencyConfiguration}：
 * {@link InvalidationBus} 由 {@link EventBusAutoConfiguration} 提供，而用户配置类先于自动配置处理，
 * 在那里写 {@code @ConditionalOnBean(InvalidationBus.class)} 会永久判定为假，
 * 表现为「改策略后 Auth 不热更新」这类只能靠 TTL 掩盖的静默故障。</p>
 *
 * <p>事件总线关闭（{@code ingot.event-bus.type=none}）时本配置整体跳过，策略变更靠 L1 / L2 TTL 收敛，
 * 登录链路不受影响。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see SessionConcurrencyCacheCoordinator
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(EventBusAutoConfiguration.class)
@ConditionalOnBean(InvalidationBus.class)
@ConditionalOnProperty(prefix = "ingot.security.session", name = "mode", havingValue = PolicySourceMode.VALUE_REMOTE)
public class SessionConcurrencyInvalidationAutoConfiguration {

    @Bean
    @ConditionalOnBean(name = SessionConcurrencyConfiguration.CACHE_BEAN_NAME)
    @ConditionalOnMissingBean(SessionConcurrencyCacheCoordinator.class)
    public SessionConcurrencyCacheCoordinator sessionConcurrencyCacheCoordinator(
            InvalidationBus bus,
            LayeredCache<String, List<SessionConcurrencyPolicyVO>> sessionConcurrencyPolicyCache) {
        SessionConcurrencyCacheCoordinator coordinator = new SessionConcurrencyCacheCoordinator(bus);
        coordinator.register(SecurityPolicyDomain.SESSION_CONCURRENCY,
                sessionConcurrencyPolicyCache::evictAll);
        log.info("[SessionConcurrency] 已订阅并发策略失效广播");
        return coordinator;
    }
}
