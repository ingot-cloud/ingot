package com.ingot.framework.security.config.annotation.web.configuration;

import com.ingot.framework.security.oauth2.server.authorization.SessionStoreAvailability;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>把 {@link SessionStoreAvailability} 的降级计数桥接为 Micrometer 指标，供告警规则消费。</p>
 *
 * <p>宽限期放行是「用可观测性换可用性」的取舍：放行本身不打日志级告警，靠这些指标发现 Redis 抖动
 * 与持续降级。仅在类路径存在 Micrometer 时生效，未接入 actuator 的服务不受影响。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see SessionStoreAvailability
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MeterRegistry.class)
public class SessionStoreMetricsConfiguration {

    /**
     * 会话存储访问失败累计次数。
     */
    private static final String UNAVAILABLE = "ingot.security.session.store.unavailable";

    /**
     * 宽限期内降级放行的累计请求数。持续增长说明 Redis 未恢复。
     */
    private static final String DEGRADED_PASS = "ingot.security.session.store.degraded_pass";

    /**
     * 当前是否处于降级状态：1 降级，0 正常。
     */
    private static final String DEGRADED = "ingot.security.session.store.degraded";

    @Bean
    public MeterBinder sessionStoreAvailabilityMeterBinder(SessionStoreAvailability availability) {
        return registry -> {
            registry.gauge(UNAVAILABLE, availability, SessionStoreAvailability::getUnavailableCount);
            registry.gauge(DEGRADED_PASS, availability, SessionStoreAvailability::getDegradedPassCount);
            registry.gauge(DEGRADED, availability, sentinel -> sentinel.isDegraded() ? 1D : 0D);
        };
    }
}
