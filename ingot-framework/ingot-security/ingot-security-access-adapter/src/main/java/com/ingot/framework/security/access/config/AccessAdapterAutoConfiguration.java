package com.ingot.framework.security.access.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.security.api.model.dto.SecurityEventReportDTO;
import com.ingot.cloud.security.api.rpc.RemoteLoginFailurePolicyService;
import com.ingot.framework.commons.constants.RedisKeyConstants;
import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.config.EventBusAutoConfiguration;
import com.ingot.framework.security.access.actuate.LoginFailurePolicyEndpoint;
import com.ingot.framework.security.access.internal.LocalLoginFailureFloorSupplier;
import com.ingot.framework.security.access.internal.LoginFailureLkgStore;
import com.ingot.framework.security.access.internal.LoginFailurePolicyCacheCoordinator;
import com.ingot.framework.security.access.internal.LoginFailurePolicySourceHolder;
import com.ingot.framework.security.access.listener.LoginFailureAccessListener;
import com.ingot.framework.security.access.redis.RedisLoginFailureCounter;
import com.ingot.framework.security.access.redis.RedisTempBlockWriter;
import com.ingot.framework.security.access.service.LoginFailureCounter;
import com.ingot.framework.security.access.service.LoginFailurePolicyLoader;
import com.ingot.framework.security.access.service.LoginFailureProtectionService;
import com.ingot.framework.security.access.service.TempBlockWriter;
import com.ingot.framework.security.access.service.impl.LocalLoginFailurePolicyLoader;
import com.ingot.framework.security.access.service.impl.RemoteLoginFailurePolicyLoader;
import com.ingot.framework.security.access.service.impl.ResilientLoginFailurePolicyLoader;
import com.ingot.framework.security.recording.transport.feign.SecurityEventReportPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 访问防护适配器自动配置。
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(EventBusAutoConfiguration.class)
@EnableConfigurationProperties(AccessProtectionProperties.class)
public class AccessAdapterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LoginFailurePolicySourceHolder.class)
    public LoginFailurePolicySourceHolder loginFailurePolicySourceHolder() {
        return new LoginFailurePolicySourceHolder();
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    @ConditionalOnBean(LoginFailurePolicySourceHolder.class)
    @ConditionalOnMissingBean
    public LoginFailurePolicyEndpoint loginFailurePolicyEndpoint(LoginFailurePolicySourceHolder sourceHolder) {
        return new LoginFailurePolicyEndpoint(sourceHolder);
    }

    @Bean
    @ConditionalOnMissingBean(LocalLoginFailureFloorSupplier.class)
    public LocalLoginFailureFloorSupplier localLoginFailureFloorSupplier(AccessProtectionProperties properties) {
        return new LocalLoginFailureFloorSupplier(properties);
    }

    @Bean
    @ConditionalOnMissingBean(LoginFailureLkgStore.class)
    public LoginFailureLkgStore loginFailureLkgStore(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                                     ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new LoginFailureLkgStore(
                redisTemplateProvider.getIfAvailable(),
                objectMapperProvider.getIfAvailable(ObjectMapper::new),
                RedisKeyConstants.LoginFailure.POLICY_LKG);
    }

    @Bean
    @ConditionalOnMissingBean(LoginFailurePolicyLoader.class)
    @ConditionalOnProperty(name = "ingot.security.access.mode", havingValue = "local", matchIfMissing = true)
    public LoginFailurePolicyLoader localLoginFailurePolicyLoader(AccessProtectionProperties properties) {
        return new LocalLoginFailurePolicyLoader(properties);
    }

    @Bean
    @ConditionalOnClass(RemoteLoginFailurePolicyService.class)
    @ConditionalOnMissingBean(LoginFailurePolicyLoader.class)
    @ConditionalOnProperty(name = "ingot.security.access.mode", havingValue = "remote")
    public LoginFailurePolicyLoader resilientLoginFailurePolicyLoader(
            RemoteLoginFailurePolicyService remoteService,
            LoginFailureLkgStore lkgStore,
            LocalLoginFailureFloorSupplier floorSupplier,
            LoginFailurePolicySourceHolder sourceHolder,
            AccessProtectionProperties properties) {
        RemoteLoginFailurePolicyLoader delegate = new RemoteLoginFailurePolicyLoader(remoteService);
        boolean localFloorEnabled = properties.getPolicy().getFallback().isLocalFloorEnabled();
        log.info("[LoginFailure] register resilient loader (remote -> LKG -> local-floor), localFloorEnabled={}",
                localFloorEnabled);
        return new ResilientLoginFailurePolicyLoader(
                delegate, lkgStore, floorSupplier, localFloorEnabled, sourceHolder);
    }

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(LoginFailureCounter.class)
    public LoginFailureCounter redisLoginFailureCounter(StringRedisTemplate redisTemplate) {
        return new RedisLoginFailureCounter(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(LoginFailureCounter.class)
    public LoginFailureCounter noopLoginFailureCounter() {
        return new LoginFailureCounter() {
            @Override
            public long increment(String counterKey, java.time.Duration window) {
                return 0L;
            }

            @Override
            public void reset(String counterKey) {
                // no-op
            }
        };
    }

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(TempBlockWriter.class)
    public TempBlockWriter redisTempBlockWriter(StringRedisTemplate redisTemplate) {
        return new RedisTempBlockWriter(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(TempBlockWriter.class)
    public TempBlockWriter noopTempBlockWriter() {
        return (keyType, keyValue, ttl) -> {
            // no-op
        };
    }

    @Bean
    @ConditionalOnMissingBean(LoginFailureProtectionService.class)
    public LoginFailureProtectionService loginFailureProtectionService(
            LoginFailurePolicyLoader policyLoader,
            LoginFailureCounter counter,
            TempBlockWriter tempBlockWriter,
            LoginFailureEventSink eventSink) {
        return new LoginFailureProtectionService(
                policyLoader,
                counter,
                tempBlockWriter,
                eventSink::offer);
    }

    @Bean
    @ConditionalOnBean(LoginFailureProtectionService.class)
    @ConditionalOnMissingBean(LoginFailureAccessListener.class)
    public LoginFailureAccessListener loginFailureAccessListener(
            LoginFailureProtectionService loginFailureProtectionService) {
        log.info("[LoginFailure] register LoginFailureAccessListener");
        return new LoginFailureAccessListener(loginFailureProtectionService);
    }

    @Bean
    @ConditionalOnBean(InvalidationBus.class)
    @ConditionalOnMissingBean(LoginFailurePolicyCacheCoordinator.class)
    public LoginFailurePolicyCacheCoordinator loginFailurePolicyCacheCoordinator(
            InvalidationBus bus,
            LoginFailurePolicyLoader policyLoader) {
        return new LoginFailurePolicyCacheCoordinator(bus, policyLoader);
    }

    @Bean
    @ConditionalOnBean(SecurityEventReportPublisher.class)
    @ConditionalOnMissingBean(LoginFailureEventSink.class)
    public LoginFailureEventSink recordingLoginFailureEventSink(SecurityEventReportPublisher reportPublisher) {
        return reportPublisher::publish;
    }

    @Bean
    @ConditionalOnMissingBean(LoginFailureEventSink.class)
    public LoginFailureEventSink noopLoginFailureEventSink() {
        return dto -> {
            // recording 未装配时不阻塞登录防护
        };
    }

    @FunctionalInterface
    interface LoginFailureEventSink {
        void offer(SecurityEventReportDTO dto);
    }
}
