package com.ingot.framework.security.recording.store.mysql.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.ingot.framework.security.recording.config.SecurityEventProperties;
import com.ingot.framework.security.recording.config.SecurityEventRecordingAutoConfiguration;
import com.ingot.framework.security.recording.spool.FileSpoolAutoConfiguration;
import com.ingot.framework.security.recording.spi.SecurityEventRetentionHandler;
import com.ingot.framework.security.recording.spi.SecurityEventStore;
import com.ingot.framework.security.recording.store.mysql.MySqlSecurityEventRetentionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.lang.reflect.Method;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>验证 MySQL Store 自动配置能在 recording 之前注册 {@link SecurityEventStore}。</p>
 */
class SecurityEventStoreMysqlAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    DataSourceTransactionManagerAutoConfiguration.class,
                    TransactionAutoConfiguration.class,
                    JacksonAutoConfiguration.class,
                    MybatisPlusAutoConfiguration.class,
                    SecurityEventStoreMysqlMapperAutoConfiguration.class,
                    SecurityEventStoreMysqlAutoConfiguration.class,
                    FileSpoolAutoConfiguration.class,
                    SecurityEventRecordingAutoConfiguration.class))
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:security-event-store;MODE=MySQL;DB_CLOSE_DELAY=-1",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "mybatis-plus.mapper-locations=classpath*:/mapper/*Mapper.xml",
                    "ingot.security.event.enabled=true",
                    "ingot.security.event.target=local",
                    "ingot.security.event.primary-store=mysql",
                    "ingot.security.event.delivery.spool.enabled=false");

    @Test
    @DisplayName("target=local 时注册 SecurityEventStore 与 dispatcher")
    void registersSecurityEventStoreBeforeDispatcher() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SecurityEventStore.class);
            assertThat(context.getBean(SecurityEventStore.class).storeId()).isEqualTo("mysql");
            assertThat(context).hasSingleBean(SecurityEventProperties.class);
            assertThat(context).hasBean("securityEventRecordingDispatcher");
        });
    }

    @Test
    @DisplayName("同时存在 memory/durable RecordQueue 时 retention 让步探测不抛多 Bean 异常")
    void retentionYieldDoesNotFailOnMultipleRecordQueues(@TempDir Path spoolDir) {
        contextRunner
                .withPropertyValues(
                        "ingot.security.event.delivery.spool.enabled=true",
                        "ingot.security.event.delivery.spool.directory=" + spoolDir,
                        "ingot.security.event.retention.enabled=true",
                        "ingot.security.event.retention.days=30")
                .run(context -> {
                    assertThat(context).hasBean("memoryRecordQueue");
                    assertThat(context).hasBean("durableRecordQueue");
                    SecurityEventRetentionHandler handler = context.getBean(SecurityEventRetentionHandler.class);
                    assertThat(handler).isInstanceOf(MySqlSecurityEventRetentionHandler.class);
                    try {
                        Method shouldYield = MySqlSecurityEventRetentionHandler.class
                                .getDeclaredMethod("shouldYield");
                        shouldYield.setAccessible(true);
                        assertThat(shouldYield.invoke(handler)).isEqualTo(false);
                    } catch (ReflectiveOperationException e) {
                        throw new AssertionError("shouldYield must resolve unique queue beans", e);
                    }
                });
    }
}
