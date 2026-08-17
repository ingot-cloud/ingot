package com.ingot.framework.security.recording.transport.feign.config;

import com.ingot.cloud.security.api.rpc.RemoteSecurityEventService;
import com.ingot.framework.security.recording.config.SecurityEventRecordingAutoConfiguration;
import com.ingot.framework.security.recording.spool.FileSpoolAutoConfiguration;
import com.ingot.framework.security.recording.spi.SecurityEventPublisher;
import com.ingot.framework.security.recording.transport.feign.SecurityEventReportPublisher;
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
 * <p>验证 {@link SecurityEventReportPublisher} 在 Publisher 就绪后注册（Gateway center 场景）。</p>
 */
class SecurityEventReportPublisherAutoConfigurationTest {

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
                    SecurityEventRecordingAutoConfiguration.class,
                    FeignSecurityEventTransportAutoConfiguration.class,
                    SecurityEventReportPublisherAutoConfiguration.class))
            .withUserConfiguration(RemoteSecurityEventServiceTestConfiguration.class)
            .withPropertyValues(
                    "ingot.security.event.enabled=true",
                    "ingot.security.event.target=center",
                    "ingot.security.event.sourceModule=GATEWAY");

    @Test
    @DisplayName("enabled=true + target=center 时注册 SecurityEventReportPublisher")
    void registersReportPublisherAfterRecording() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SecurityEventPublisher.class);
            assertThat(context).hasSingleBean(SecurityEventReportPublisher.class);
        });
    }

    @Test
    @DisplayName("未显式 enabled=true 时不注册 SecurityEventReportPublisher")
    void skipsWhenRecordingNotEnabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        SecurityEventReportPublisherAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(SecurityEventReportPublisher.class));
    }
}
