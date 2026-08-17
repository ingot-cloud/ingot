package com.ingot.framework.security.account.adapter.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.ingot.framework.security.account.adapter.port.CompositeSecurityEventPort;
import com.ingot.framework.security.account.adapter.support.AccountSecurityEventRecordMapper;
import com.ingot.framework.security.account.domain.config.AccountSecurityEventPortNoOpAutoConfiguration;
import com.ingot.framework.security.account.domain.port.outbound.SecurityEventPort;
import com.ingot.framework.security.account.domain.port.outbound.noop.NoOpSecurityEventPort;
import com.ingot.framework.security.recording.config.SecurityEventProperties;
import com.ingot.framework.security.recording.config.SecurityEventRecordingAutoConfiguration;
import com.ingot.framework.security.recording.spool.FileSpoolAutoConfiguration;
import com.ingot.framework.security.recording.store.mysql.config.SecurityEventStoreMysqlAutoConfiguration;
import com.ingot.framework.security.recording.store.mysql.config.SecurityEventStoreMysqlMapperAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>验证 recording 就绪后注册 {@link CompositeSecurityEventPort}，而非 {@link NoOpSecurityEventPort}。</p>
 */
class AccountSecurityEventPortAutoConfigurationTest {

    private static final AutoConfigurations RECORDING_STACK = AutoConfigurations.of(
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            SecurityEventStoreMysqlMapperAutoConfiguration.class,
            SecurityEventStoreMysqlAutoConfiguration.class,
            FileSpoolAutoConfiguration.class,
            SecurityEventRecordingAutoConfiguration.class,
            AccountSecurityEventPortAutoConfiguration.class,
            AccountSecurityEventPortNoOpAutoConfiguration.class);

    private final ApplicationContextRunner enabledContextRunner = new ApplicationContextRunner()
            .withConfiguration(RECORDING_STACK)
            .withUserConfiguration(SecurityEventPropertiesTestConfiguration.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:account-event-port;MODE=MySQL;DB_CLOSE_DELAY=-1",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "mybatis-plus.mapper-locations=classpath*:/mapper/*Mapper.xml",
                    "ingot.security.event.enabled=true",
                    "ingot.security.event.target=local",
                    "ingot.security.event.primary-store=mysql",
                    "ingot.security.event.sourceModule=PMS");

    @Test
    @DisplayName("enabled=true 时使用 CompositeSecurityEventPort")
    void registersCompositeSecurityEventPortAfterRecording() {
        enabledContextRunner.run(context -> {
            assertThat(context).hasSingleBean(AccountSecurityEventRecordMapper.class);
            assertThat(context).hasSingleBean(SecurityEventPort.class);
            assertThat(context.getBean(SecurityEventPort.class)).isInstanceOf(CompositeSecurityEventPort.class);
        });
    }

    @Test
    @DisplayName("未启用 recording 时使用 NoOpSecurityEventPort")
    void fallsBackToNoOpWhenRecordingDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        JacksonAutoConfiguration.class,
                        AccountSecurityEventPortAutoConfiguration.class,
                        AccountSecurityEventPortNoOpAutoConfiguration.class))
                .withUserConfiguration(SecurityEventPropertiesTestConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(SecurityEventPort.class);
                    assertThat(context.getBean(SecurityEventPort.class)).isInstanceOf(NoOpSecurityEventPort.class);
                });
    }

    @Configuration
    @EnableConfigurationProperties(SecurityEventProperties.class)
    static class SecurityEventPropertiesTestConfiguration {
    }
}
