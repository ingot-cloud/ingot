package com.ingot.cloud.gateway.security.config;

import com.ingot.cloud.gateway.config.BlacklistEventReporterConfiguration;
import com.ingot.cloud.gateway.security.BlacklistEventReporter;
import com.ingot.cloud.security.api.rpc.RemoteSecurityEventService;
import com.ingot.framework.security.recording.config.SecurityEventRecordingAutoConfiguration;
import com.ingot.framework.security.recording.spool.FileSpoolAutoConfiguration;
import com.ingot.framework.security.recording.transport.feign.SecurityEventReportPublisher;
import com.ingot.framework.security.recording.transport.feign.config.FeignSecurityEventTransportAutoConfiguration;
import com.ingot.framework.security.recording.transport.feign.config.SecurityEventReportPublisherAutoConfiguration;
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
 * <p>验证 {@link BlacklistEventReporter} 在 ReportPublisher 就绪后注册（Gateway center 场景）。</p>
 */
class BlacklistEventReporterConfigurationTest {

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
                    SecurityEventReportPublisherAutoConfiguration.class,
                    BlacklistEventReporterConfiguration.class))
            .withUserConfiguration(RemoteSecurityEventServiceTestConfiguration.class)
            .withPropertyValues(
                    "ingot.security.event.enabled=true",
                    "ingot.security.event.target=center",
                    "ingot.security.event.sourceModule=GATEWAY",
                    "ingot.security.event.categories.access=true");

    @Test
    @DisplayName("enabled=true + target=center 时注册 BlacklistEventReporter")
    void registersReporterAfterReportPublisher() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SecurityEventReportPublisher.class);
            assertThat(context).hasSingleBean(BlacklistEventReporter.class);
        });
    }

    @Test
    @DisplayName("recording 未启用时不注册 BlacklistEventReporter")
    void skipsWhenRecordingNotEnabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(BlacklistEventReporterConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(BlacklistEventReporter.class));
    }
}
