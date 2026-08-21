package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.security.api.model.vo.policy.SessionConcurrencyPolicyVO;
import com.ingot.cloud.security.api.rpc.RemoteSessionConcurrencyPolicyService;
import com.ingot.framework.cache.config.LayeredCacheBuilder;
import com.ingot.framework.cache.config.LayeredCacheSettings;
import com.ingot.framework.cache.registry.LayeredCacheRegistry;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.commons.model.security.PolicySourceMode;
import com.ingot.framework.security.core.InSecurityProperties;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import com.ingot.framework.security.oauth2.server.authorization.session.SessionRevocationService;
import com.ingot.framework.security.oauth2.server.authorization.session.concurrency.ClientOnlySessionConcurrencyPolicyResolver;
import com.ingot.framework.security.oauth2.server.authorization.session.concurrency.FeignSessionConcurrencyPolicyLoader;
import com.ingot.framework.security.oauth2.server.authorization.session.concurrency.LocalSessionConcurrencyPolicyResolver;
import com.ingot.framework.security.oauth2.server.authorization.session.concurrency.RemoteSessionConcurrencyPolicyResolver;
import com.ingot.framework.security.oauth2.server.authorization.session.concurrency.SessionConcurrencyEnforcer;
import com.ingot.framework.security.oauth2.server.authorization.session.concurrency.SessionConcurrencyPolicyFloorSupplier;
import com.ingot.framework.security.oauth2.server.authorization.session.concurrency.SessionConcurrencyPolicyResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * <p>并发会话策略的装配入口，按部署形态选择策略来源并组装远端策略的分层缓存。</p>
 *
 * <p>三种形态互斥，由配置决定，均以 {@link SessionConcurrencyPolicyResolver} 对执行面暴露：</p>
 * <ul>
 *     <li>{@code concurrency.enabled=false} — 只认 Client 的 {@code UNIQUE / STANDARD}，
 *         紧急回退用，不读配置也不发远端调用；</li>
 *     <li>{@code mode=local}（默认） — 读 Nacos 下发的本地参数，适用于未部署安全中心的环境；</li>
 *     <li>{@code mode=remote} — 走 {@code remote → LKG → Nacos 地板} 分层缓存，
 *         并订阅失效广播实现秒级生效。</li>
 * </ul>
 *
 * <p>形态判定放在嵌套配置类的类级条件上，而不是同类内多个 {@code @ConditionalOnMissingBean} 方法：
 * 后者依赖 Bean 方法声明顺序，在非自动配置类中不可靠。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see SessionConcurrencyEnforcer
 * @apiNote {@code mode=remote} 要求容器内存在 {@link RemoteSessionConcurrencyPolicyService}；
 *          缺失时启动即失败，而不是静默退回无限并发。
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class SessionConcurrencyConfiguration {

    /**
     * 远端策略缓存的 Bean 名称，供失效广播装配按名称做条件判定。
     */
    public static final String CACHE_BEAN_NAME = "sessionConcurrencyPolicyCache";

    /**
     * 并发约束执行面：登录落库前把在线会话数压到策略允许范围内。
     */
    @Bean
    @ConditionalOnMissingBean(SessionConcurrencyEnforcer.class)
    public SessionConcurrencyEnforcer sessionConcurrencyEnforcer(
            OnlineTokenService onlineTokenService,
            SessionRevocationService sessionRevocationService,
            SessionConcurrencyPolicyResolver policyResolver) {
        log.info("[SessionConcurrency] Creating SessionConcurrencyEnforcer, resolver={}",
                policyResolver.getClass().getSimpleName());
        return new SessionConcurrencyEnforcer(onlineTokenService, sessionRevocationService, policyResolver);
    }

    /**
     * <p>并发策略总开关关闭时的装配：退回 Client 语义。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "ingot.security.session.concurrency", name = "enabled", havingValue = "false")
    static class ClientOnlyConfiguration {

        @Bean
        @ConditionalOnMissingBean(SessionConcurrencyPolicyResolver.class)
        public SessionConcurrencyPolicyResolver clientOnlySessionConcurrencyPolicyResolver() {
            log.info("[SessionConcurrency] 并发策略已关闭，仅保留 Client UNIQUE/STANDARD 语义");
            return new ClientOnlySessionConcurrencyPolicyResolver();
        }
    }

    /**
     * <p>并发策略启用时的装配：按 {@code ingot.security.session.mode} 选择本地或远端来源。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "ingot.security.session.concurrency", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    static class PolicyDrivenConfiguration {

        private static final String CACHE_NAME = "session-concurrency-policy";
        private static final TypeReference<List<SessionConcurrencyPolicyVO>> POLICY_LIST_TYPE =
                new TypeReference<>() {
                };

        @Bean
        @ConditionalOnMissingBean(SessionConcurrencyPolicyResolver.class)
        @ConditionalOnProperty(prefix = "ingot.security.session", name = "mode",
                havingValue = PolicySourceMode.VALUE_LOCAL, matchIfMissing = true)
        public SessionConcurrencyPolicyResolver localSessionConcurrencyPolicyResolver(
                InSecurityProperties properties) {
            log.info("[SessionConcurrency] 并发策略来源=local，读 ingot.security.session.concurrency.*");
            return new LocalSessionConcurrencyPolicyResolver(properties);
        }

        /**
         * 远端策略的分层缓存：{@code L1 Caffeine → L2 Redis → remote → LKG → Nacos 地板}。
         */
        @Bean(CACHE_BEAN_NAME)
        @ConditionalOnMissingBean(name = CACHE_BEAN_NAME)
        @ConditionalOnProperty(prefix = "ingot.security.session", name = "mode", havingValue = PolicySourceMode.VALUE_REMOTE)
        public LayeredCache<String, List<SessionConcurrencyPolicyVO>> sessionConcurrencyPolicyCache(
                RemoteSessionConcurrencyPolicyService remoteService,
                InSecurityProperties properties,
                ObjectProvider<StringRedisTemplate> redisProvider,
                ObjectProvider<ObjectMapper> objectMapperProvider,
                ObjectProvider<LayeredCacheRegistry> registryProvider) {
            InSecurityProperties.Policy policy = properties.getSession().getPolicy();
            InSecurityProperties.Cache cacheProps = policy.getCache();
            LayeredCacheSettings settings = LayeredCacheSettings.builder()
                    .l1Enabled(cacheProps.isL1Enabled())
                    .l1Ttl(cacheProps.getL1Ttl())
                    .l1MaximumSize(cacheProps.getL1MaximumSize())
                    .l2Enabled(cacheProps.isL2Enabled())
                    .l2Ttl(cacheProps.getL2Ttl())
                    .resilienceEnabled(policy.isResilienceEnabled())
                    .localFloorEnabled(policy.getFallback().isLocalFloorEnabled())
                    .build();

            StringRedisTemplate redisTemplate = redisProvider.getIfAvailable();
            ObjectMapper objectMapper = objectMapperProvider.getIfAvailable();

            return LayeredCacheBuilder.<String, List<SessionConcurrencyPolicyVO>>named(CACHE_NAME)
                    .loader(new FeignSessionConcurrencyPolicyLoader(remoteService))
                    .settings(settings)
                    .cacheable(value -> value != null && !value.isEmpty())
                    .emptyValue(List::of)
                    .resilientSingleKey(redisTemplate, objectMapper, POLICY_LIST_TYPE,
                            cacheProps.getLkgRedisKey(),
                            new SessionConcurrencyPolicyFloorSupplier(properties))
                    .l2SingleKey(redisTemplate, objectMapper, POLICY_LIST_TYPE, cacheProps.getL2RedisKey())
                    .registry(registryProvider.getIfAvailable())
                    .build();
        }

        @Bean
        @ConditionalOnMissingBean(SessionConcurrencyPolicyResolver.class)
        @ConditionalOnProperty(prefix = "ingot.security.session", name = "mode", havingValue = PolicySourceMode.VALUE_REMOTE)
        public SessionConcurrencyPolicyResolver remoteSessionConcurrencyPolicyResolver(
                LayeredCache<String, List<SessionConcurrencyPolicyVO>> sessionConcurrencyPolicyCache) {
            log.info("[SessionConcurrency] 并发策略来源=remote，降级链 remote -> LKG -> Nacos 地板");
            return new RemoteSessionConcurrencyPolicyResolver(sessionConcurrencyPolicyCache);
        }
    }
}
