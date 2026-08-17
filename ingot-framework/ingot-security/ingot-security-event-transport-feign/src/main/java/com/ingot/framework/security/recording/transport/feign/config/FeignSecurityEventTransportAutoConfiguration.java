package com.ingot.framework.security.recording.transport.feign.config;

import com.ingot.cloud.security.api.EnableAPIConfiguration;
import com.ingot.cloud.security.api.rpc.RemoteSecurityEventService;
import com.ingot.framework.security.recording.config.SecurityEventRecordingAutoConfiguration;
import com.ingot.framework.security.recording.spi.SecurityEventTransport;
import com.ingot.framework.security.recording.transport.feign.FeignSecurityEventTransport;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * <p>Feign 安全事件 Transport 自动配置。</p>
 *
 * <p>须在 {@link EnableAPIConfiguration}（注册 {@link RemoteSecurityEventService} Feign 客户端）
 * 之后、{@link SecurityEventRecordingAutoConfiguration} 之前执行，避免 {@code target=center}
 * 时 dispatcher 启动校验找不到 Transport。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration
@AutoConfigureAfter(EnableAPIConfiguration.class)
@AutoConfigureBefore(SecurityEventRecordingAutoConfiguration.class)
@ConditionalOnClass(RemoteSecurityEventService.class)
public class FeignSecurityEventTransportAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SecurityEventTransport.class)
    public SecurityEventTransport feignSecurityEventTransport(RemoteSecurityEventService remoteService) {
        return new FeignSecurityEventTransport(remoteService);
    }
}
