package com.ingot.framework.authorization;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.iam.api.rpc.RemoteIamAuthorizationService;
import com.ingot.framework.cache.config.LayeredCacheBuilder;
import com.ingot.framework.cache.config.LayeredCacheSettings;
import com.ingot.framework.cache.coordinator.LayeredCacheCoordinator;
import com.ingot.framework.cache.registry.LayeredCacheRegistry;
import com.ingot.framework.cache.spi.RemoteUnavailableException;
import com.ingot.framework.commons.model.iam.extension.AuthorizationDecision;
import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationInvalidationEvent;
import com.ingot.framework.eventbus.InvalidationBus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * <p>业务服务同源授权的自动配置，IAM 由本地 Bean 覆盖。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AuthorizationSdkProperties.class)
public class AuthorizationSdkConfiguration {

    /**
     * 映射SDK自己的稳定拒绝与故障，不接管普通业务异常。
     * @return SDK异常处理器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public SdkAuthorizationErrorHandler sdkAuthorizationErrorHandler() {
        return new SdkAuthorizationErrorHandler();
    }

    /**
     * 授权缓存最长热窗口。
     */
    public static final Duration MAX_TTL = Duration.ofSeconds(30);

    private static final String PREFIX = "in:iam:fields-v3:";

    private static final String CACHE_NAME = "iam-authorization-fields-v3";

    private static final String ALL = "all";

    /**
     * 只注册本服务的实际资源。
     * @param providers 业务适配器
     * @return 注册表
     */
    @Bean
    @ConditionalOnMissingBean
    public ResourceRegistry resourceRegistry(List<ResourceObjectProvider> providers) {
        return new ResourceRegistry(providers);
    }

    /**
     * 装配无 LKG 的远程授权客户端。
     * @param remote RPC
     * @param resources 注册表
     * @param mapper JSON
     * @param redis Redis
     * @param registries 缓存监控
     * @param properties 消费模块设置
     * @return 客户端
     */
    @Bean
    @ConditionalOnMissingBean(AuthorizationClient.class)
    @ConditionalOnBean(RemoteIamAuthorizationService.class)
    public RemoteAuthorizationClient remoteAuthorizationClient(RemoteIamAuthorizationService remote,
            ResourceRegistry resources, ObjectMapper mapper, ObjectProvider<StringRedisTemplate> redis,
            ObjectProvider<LayeredCacheRegistry> registries, AuthorizationSdkProperties properties) {
        Duration requested = properties.getTtl();
        Duration ttl = requested == null || requested.isNegative() || requested.isZero()
                || requested.compareTo(MAX_TTL) > 0 ? MAX_TTL : requested;
        var settings = LayeredCacheSettings.builder()
            .l1Enabled(properties.isL1Enabled())
            .l1Ttl(ttl)
            .l1MaximumSize(properties.getMaximumSize())
            .l2Enabled(properties.isL2Enabled())
            .l2Ttl(ttl)
            .resilienceEnabled(false)
            .localFloorEnabled(false)
            .build();
        var cache = LayeredCacheBuilder.<String, AuthorizationDecision>named(CACHE_NAME)
            .settings(settings)
            .loader(key -> {
                try {
                    var query = mapper.readValue(key, RemoteAuthorizationClient.CacheKey.class);
                    if (!query.actor().equals(RemoteAuthorizationClient.current())) {
                        throw new RemoteUnavailableException("授权缓存身份不一致");
                    }
                    return RemoteAuthorizationClient.load(remote, query.request(), query.actor(), query.preview());
                }
                catch (IOException exception) {
                    throw new RemoteUnavailableException("授权缓存键无效", exception);
                }
            })
            .cacheable(value -> value != null && value.actions().values().stream().anyMatch(action -> action.allowed()))
            .l2(redis.getIfAvailable(), mapper, new TypeReference<AuthorizationDecision>() {
            }, key -> PREFIX + key, PREFIX + "*")
            .registry(registries.getIfAvailable())
            .build();
        return new RemoteAuthorizationClient(remote, resources, cache, mapper);
    }

    /**
     * 通用操作准入。
     * @param client 授权实现
     * @return 门禁
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AuthorizationClient.class)
    public AuthorizationAccess authorizationAccess(AuthorizationClient client) {
        return new AuthorizationAccess(client);
    }

    /**
     * 精确注解入口。
     * @param access 门禁
     * @return 切面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AuthorizationAccess.class)
    public IamActionAspect iamActionAspect(AuthorizationAccess access) {
        return new IamActionAspect(access);
    }

    /**
     * 订阅既有授权失效事件。
     * @param bus 总线
     * @param client 本节点客户端
     * @return 协调器
     */
    @Bean
    @ConditionalOnBean({ InvalidationBus.class, RemoteAuthorizationClient.class })
    public LayeredCacheCoordinator<AuthorizationInvalidationEvent, String> iamV2CacheCoordinator(InvalidationBus bus,
            RemoteAuthorizationClient client) {
        var coordinator = new LayeredCacheCoordinator<>(bus, AuthorizationInvalidationEvent.class, event -> ALL, ALL);
        coordinator.register(ALL, client::evictAll);
        return coordinator;
    }

}
