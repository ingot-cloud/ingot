package com.ingot.framework.security.recording.store.mysql.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.recording.config.SecurityEventProperties;
import com.ingot.framework.security.recording.config.SecurityEventRecordingAutoConfiguration;
import com.ingot.framework.security.recording.spi.SecurityEventQueryRepository;
import com.ingot.framework.security.recording.spi.SecurityEventRetentionHandler;
import com.ingot.framework.security.recording.spi.SecurityEventStore;
import com.ingot.framework.security.recording.store.mysql.MySqlSecurityEventQueryRepository;
import com.ingot.framework.security.recording.store.mysql.MySqlSecurityEventRetentionHandler;
import com.ingot.framework.security.recording.store.mysql.MySqlSecurityEventStore;
import com.ingot.framework.security.recording.store.mysql.internal.SecurityEventWriteSemaphore;
import com.ingot.framework.security.recording.store.mysql.mapper.SecurityEventStoreMapper;
import com.ingot.framework.security.recording.store.mysql.task.SecurityEventRetentionScheduledTask;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * <p>MySQL 安全事件 Store 自动配置。</p>
 *
 * <p>Mapper 由 {@link SecurityEventStoreMysqlMapperAutoConfiguration} 注册；Store 通过方法参数注入
 * {@link SecurityEventStoreMapper}（与 account-adapter 相同，不使用 {@code @ConditionalOnBean} 判 Mapper）。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration
@AutoConfigureAfter(SecurityEventStoreMysqlMapperAutoConfiguration.class)
@AutoConfigureBefore(SecurityEventRecordingAutoConfiguration.class)
@ConditionalOnClass(DataSource.class)
@EnableConfigurationProperties(SecurityEventProperties.class)
public class SecurityEventStoreMysqlAutoConfiguration {

    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean
    public SecurityEventWriteSemaphore securityEventWriteSemaphore(SecurityEventProperties properties) {
        SecurityEventProperties.Mysql mysql = properties.getMysql();
        return new SecurityEventWriteSemaphore(mysql.getMaxConcurrentWrites(), mysql.getTransactionTimeoutSeconds());
    }

    @Bean
    @ConditionalOnMissingBean
    public TransactionTemplate securityEventTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    @ConditionalOnMissingBean(SecurityEventStore.class)
    public SecurityEventStore mySqlSecurityEventStore(
            SecurityEventStoreMapper mapper,
            SecurityEventWriteSemaphore writeSemaphore,
            TransactionTemplate securityEventTransactionTemplate,
            org.springframework.beans.factory.ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new MySqlSecurityEventStore(
                mapper,
                writeSemaphore,
                securityEventTransactionTemplate,
                objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean(SecurityEventQueryRepository.class)
    public SecurityEventQueryRepository mySqlSecurityEventQueryRepository(
            SecurityEventStoreMapper mapper,
            org.springframework.beans.factory.ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new MySqlSecurityEventQueryRepository(mapper, objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean(SecurityEventRetentionHandler.class)
    public SecurityEventRetentionHandler mySqlSecurityEventRetentionHandler(
            SecurityEventStoreMapper mapper,
            SecurityEventWriteSemaphore writeSemaphore,
            TransactionTemplate securityEventTransactionTemplate,
            SecurityEventProperties properties,
            org.springframework.beans.factory.ObjectProvider<com.ingot.framework.security.recording.runtime.MemoryRecordQueue> memoryQueueProvider,
            org.springframework.beans.factory.ObjectProvider<com.ingot.framework.security.recording.spool.FileSpoolRecordQueue> durableQueueProvider) {
        return new MySqlSecurityEventRetentionHandler(
                mapper,
                writeSemaphore,
                securityEventTransactionTemplate,
                properties,
                memoryQueueProvider,
                durableQueueProvider);
    }

    @Bean
    @ConditionalOnBean(SecurityEventRetentionHandler.class)
    @ConditionalOnClass(name = "com.ingot.framework.tss.common.annotation.ScheduledTask")
    @ConditionalOnMissingBean
    public SecurityEventRetentionScheduledTask securityEventRetentionScheduledTask(
            SecurityEventRetentionHandler retentionHandler) {
        return new SecurityEventRetentionScheduledTask(retentionHandler);
    }
}
