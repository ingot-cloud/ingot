package com.ingot.framework.data.mybatis.scope.config;

import java.time.Duration;

import javax.sql.DataSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.pms.api.model.dto.authorization.AuthorizationSnapshotDTO;
import com.ingot.cloud.pms.api.rpc.RemotePmsAuthorizationService;
import com.ingot.framework.cache.config.LayeredCacheBuilder;
import com.ingot.framework.cache.config.LayeredCacheSettings;
import com.ingot.framework.cache.registry.LayeredCacheRegistry;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.cache.spi.RemoteUnavailableException;
import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationSnapshotAccess;
import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationSnapshotCacheCoordinator;
import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationSnapshotConstants;
import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationSnapshotHttpConfigurer;
import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationSnapshotLoader;
import com.ingot.framework.data.mybatis.scope.authorization.RemoteAuthorizationSnapshotLoader;
import com.ingot.framework.data.mybatis.scope.error.AuthorizationExceptionHandler;
import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.config.EventBusAutoConfiguration;
import feign.Feign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * <p>数据范围与授权快照自动配置：装配分层缓存（无 LKG）、请求过滤器、AOP 与写校验。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter({DataSourceAutoConfiguration.class, EventBusAutoConfiguration.class})
@EnableConfigurationProperties(DataScopeProperties.class)
public class InDataScopeConfig {

    private static final TypeReference<AuthorizationSnapshotDTO> SNAPSHOT_TYPE = new TypeReference<>() {
    };

    /**
     * 其它服务默认走 PMS 内部 RPC 加载快照；PMS 进程内由本地 {@code @Primary} 覆盖。
     *
     * @param remotePmsAuthorizationService Feign 客户端
     * @return 远端加载器
     */
    @Bean
    @ConditionalOnClass(Feign.class)
    @ConditionalOnBean(RemotePmsAuthorizationService.class)
    @ConditionalOnMissingBean(AuthorizationSnapshotLoader.class)
    public AuthorizationSnapshotLoader remoteAuthorizationSnapshotLoader(
            RemotePmsAuthorizationService remotePmsAuthorizationService) {
        log.info("[AuthorizationSnapshot] register remote loader");
        return new RemoteAuthorizationSnapshotLoader(remotePmsAuthorizationService);
    }

    /**
     * 装配 L1/L2 授权快照缓存，关闭 Resilient/LKG。
     *
     * @param loader               快照加载器
     * @param properties           模块配置
     * @param redisProvider        Redis
     * @param objectMapperProvider Jackson
     * @param registryProvider     注册表
     * @return 分层缓存
     */
    @Bean
    @ConditionalOnBean(AuthorizationSnapshotLoader.class)
    @ConditionalOnMissingBean(name = "authorizationSnapshotCache")
    public LayeredCache<String, AuthorizationSnapshotDTO> authorizationSnapshotCache(
            AuthorizationSnapshotLoader loader,
            DataScopeProperties properties,
            ObjectProvider<StringRedisTemplate> redisProvider,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            ObjectProvider<LayeredCacheRegistry> registryProvider) {
        Duration l1Ttl = capTtl(properties.getCacheTtl());
        Duration l2Ttl = capTtl(properties.getRedisTtl());
        LayeredCacheSettings settings = LayeredCacheSettings.builder()
                .l1Enabled(properties.isCacheEnabled())
                .l1Ttl(l1Ttl)
                .l1MaximumSize(properties.getCacheMaximumSize())
                .l2Enabled(properties.isRedisEnabled())
                .l2Ttl(l2Ttl)
                .resilienceEnabled(false)
                .localFloorEnabled(false)
                .build();
        String prefix = StrUtilBlankToDefault(properties.getRedisKeyPrefix(),
                AuthorizationSnapshotConstants.REDIS_KEY_PREFIX);
        log.info("[AuthorizationSnapshot] layered cache assembled l1={} l2={}",
                properties.isCacheEnabled(), properties.isRedisEnabled());
        return LayeredCacheBuilder.<String, AuthorizationSnapshotDTO>named(AuthorizationSnapshotConstants.CACHE_NAME)
                .loader(key -> loadSnapshot(loader, key))
                .settings(settings)
                .cacheable(v -> v != null && !v.isEmptyAuthorization())
                .l2(redisProvider.getIfAvailable(), objectMapperProvider.getIfAvailable(), SNAPSHOT_TYPE,
                        key -> prefix + key, prefix + "*")
                .registry(registryProvider.getIfAvailable())
                .build();
    }

    /**
     * 快照读取入口。
     *
     * @param cache 分层缓存
     * @return 访问器
     */
    @Bean
    @ConditionalOnBean(name = "authorizationSnapshotCache")
    @ConditionalOnMissingBean
    public AuthorizationSnapshotAccess authorizationSnapshotAccess(
            LayeredCache<String, AuthorizationSnapshotDTO> cache) {
        return new AuthorizationSnapshotAccess(cache);
    }

    /**
     * 将快照过滤器挂入 Spring Security 链。
     *
     * @param snapshotAccess 快照访问器
     * @return HTTP 配置器
     */
    @Bean
    @ConditionalOnBean(AuthorizationSnapshotAccess.class)
    @ConditionalOnMissingBean
    public AuthorizationSnapshotHttpConfigurer authorizationSnapshotHttpConfigurer(
            AuthorizationSnapshotAccess snapshotAccess) {
        return new AuthorizationSnapshotHttpConfigurer(snapshotAccess);
    }

    /**
     * 跨节点失效协调器。
     *
     * @param bus            总线
     * @param snapshotAccess 访问器
     * @return 协调器
     */
    @Bean
    @ConditionalOnBean({InvalidationBus.class, AuthorizationSnapshotAccess.class})
    @ConditionalOnMissingBean
    public AuthorizationSnapshotCacheCoordinator authorizationSnapshotCacheCoordinator(
            InvalidationBus bus,
            AuthorizationSnapshotAccess snapshotAccess) {
        return new AuthorizationSnapshotCacheCoordinator(bus, snapshotAccess);
    }

    /**
     * 运行时授权异常 HTTP 映射。
     *
     * @return 处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthorizationExceptionHandler authorizationExceptionHandler() {
        return new AuthorizationExceptionHandler();
    }

    /**
     * 表映射解析。
     *
     * @param properties 配置
     * @return 解析器
     */
    @Bean
    @ConditionalOnBean(DataSource.class)
    public DataScopeTableResolver dataScopeTableResolver(DataScopeProperties properties) {
        return new DataScopeTableResolver(properties);
    }

    /**
     * 数据范围 AOP。
     *
     * @return 切面
     */
    @Bean
    @ConditionalOnBean(DataSource.class)
    public DataScopeAOP dataScopeAOP() {
        return new DataScopeAOP();
    }

    private static AuthorizationSnapshotDTO loadSnapshot(AuthorizationSnapshotLoader loader, String key) {
        int split = key.indexOf(':');
        if (split <= 0 || split == key.length() - 1) {
            throw new RemoteUnavailableException("invalid authorization snapshot key");
        }
        long tenantId = Long.parseLong(key.substring(0, split));
        long userId = Long.parseLong(key.substring(split + 1));
        try {
            AuthorizationSnapshotDTO snapshot = loader.load(tenantId, userId);
            if (snapshot == null) {
                throw new RemoteUnavailableException("authorization snapshot loader returned null");
            }
            return snapshot;
        } catch (RemoteUnavailableException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new RemoteUnavailableException("authorization snapshot loader failed", ex);
        }
    }

    private static Duration capTtl(Duration ttl) {
        Duration max = Duration.ofSeconds(AuthorizationSnapshotConstants.MAX_TTL_SECONDS);
        if (ttl == null || ttl.isZero() || ttl.isNegative() || ttl.compareTo(max) > 0) {
            return max;
        }
        return ttl;
    }

    private static String StrUtilBlankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
