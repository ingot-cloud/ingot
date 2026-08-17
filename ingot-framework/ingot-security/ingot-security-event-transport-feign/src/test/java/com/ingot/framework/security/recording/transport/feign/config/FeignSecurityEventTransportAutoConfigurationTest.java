package com.ingot.framework.security.recording.transport.feign.config;

import com.ingot.cloud.security.api.rpc.RemoteSecurityEventService;
import com.ingot.framework.security.recording.config.SecurityEventRecordingAutoConfiguration;
import com.ingot.framework.security.recording.runtime.SecurityEventRecordingDispatcher;
import com.ingot.framework.security.recording.spool.FileSpoolAutoConfiguration;
import com.ingot.framework.security.recording.spi.SecurityEventTransport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * <p>验证 center 场景下 Transport 在 Recording dispatcher 之前就绪。</p>
 */
class FeignSecurityEventTransportAutoConfigurationTest {

    @Configuration
    static class RemoteSecurityEventServiceTestConfiguration {
        @Bean
        RemoteSecurityEventService remoteSecurityEventService() {
            return mock(RemoteSecurityEventService.class);
        }
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    FileSpoolAutoConfiguration.class,
                    FeignSecurityEventTransportAutoConfiguration.class,
                    SecurityEventRecordingAutoConfiguration.class))
            .withUserConfiguration(RemoteSecurityEventServiceTestConfiguration.class)
            .withPropertyValues(
                    "ingot.security.event.enabled=true",
                    "ingot.security.event.target=center",
                    "ingot.security.event.sourceModule=GATEWAY");

    @Test
    @DisplayName("target=center 时注册 Feign Transport 且 dispatcher 可启动")
    void registersTransportBeforeRecordingDispatcher() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SecurityEventTransport.class);
            assertThat(context).hasSingleBean(SecurityEventRecordingDispatcher.class);
        });
    }
}
