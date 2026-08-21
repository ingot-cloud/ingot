package com.ingot.framework.security.account.adapter.config;

import com.ingot.cloud.auth.api.EnableAPIConfiguration;
import com.ingot.cloud.auth.api.rpc.RemoteAuthSessionService;
import com.ingot.framework.security.account.adapter.port.FeignSessionRevocationAdapter;
import com.ingot.framework.security.account.domain.port.outbound.SessionRevocationPort;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>注册经 Auth Inner RPC 撤销会话的 {@link SessionRevocationPort}。</p>
 *
 * <p>须晚于 {@link EnableAPIConfiguration} 评估，否则 {@code @ConditionalOnBean} 会在
 * Feign 客户端定义注册前误判为缺失；本配置不生效时由 account-core 的 NoOp 兜底，
 * 账号状态变更仍然可用，只是不联动下线。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote 可通过 {@code ingot.security.session.linkage-revoke-enabled=false} 关闭联动，
 * 仅用于 Auth 不可达的临时降级，默认开启。
 */
@AutoConfiguration
@AutoConfigureAfter(EnableAPIConfiguration.class)
@ConditionalOnBean(RemoteAuthSessionService.class)
@ConditionalOnProperty(name = SessionRevocationPortAutoConfiguration.ENABLED_KEY, matchIfMissing = true)
public class SessionRevocationPortAutoConfiguration {

    static final String ENABLED_KEY = "ingot.security.session.linkage-revoke-enabled";

    private static final String METRIC_SUCCESS = "ingot.security.session.revoke.linkage.success";
    private static final String METRIC_FAILURE = "ingot.security.session.revoke.linkage.failure";

    @Bean
    @ConditionalOnMissingBean(SessionRevocationPort.class)
    public FeignSessionRevocationAdapter feignSessionRevocationAdapter(
            RemoteAuthSessionService remoteAuthSessionService) {
        return new FeignSessionRevocationAdapter(remoteAuthSessionService);
    }

    /**
     * <p>把会话撤销联动的成功/失败计数桥接为 Micrometer 指标。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MeterBinder.class)
    @ConditionalOnBean(FeignSessionRevocationAdapter.class)
    public static class MetricsConfiguration {

        @Bean
        public MeterBinder accountSessionRevokeMeterBinder(FeignSessionRevocationAdapter adapter) {
            return registry -> {
                registry.gauge(METRIC_SUCCESS, adapter, FeignSessionRevocationAdapter::getRevokedSessions);
                registry.gauge(METRIC_FAILURE, adapter, FeignSessionRevocationAdapter::getFailures);
            };
        }
    }
}
