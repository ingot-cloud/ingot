package com.ingot.framework.account.adapter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.security.api.config.SecurityEventProperties;
import com.ingot.cloud.security.api.rpc.RemoteSecurityEventService;
import com.ingot.framework.account.adapter.mapper.AccountLockStateMapper;
import com.ingot.framework.account.adapter.mapper.AccountSecurityEventMapper;
import com.ingot.framework.account.adapter.mapper.MapperModule;
import com.ingot.framework.account.adapter.port.CompositeSecurityEventPort;
import com.ingot.framework.account.adapter.port.DefaultLockStatePortAdapter;
import com.ingot.framework.account.adapter.port.DefaultSecurityEventPortAdapter;
import com.ingot.framework.account.adapter.port.RemoteSecurityEventPortAdapter;
import com.ingot.framework.account.adapter.service.ServiceModule;
import com.ingot.framework.account.adapter.support.AccountSecurityEventReportMapper;
import com.ingot.framework.account.adapter.task.TaskModule;
import com.ingot.framework.account.domain.config.AccountDomainAutoConfiguration;
import com.ingot.framework.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.account.domain.port.outbound.SecurityEventPort;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * account-adapter 模块自动配置
 * <p>
 * 提供基于 MyBatis-Plus 的 LockStatePort 和 SecurityEventPort 具体实现。
 * 通过 {@link AutoConfigureBefore} 确保在 {@link AccountDomainAutoConfiguration} 之前处理，
 * 使 core 模块的 NoOp 回退 Bean 检测到此处已注册的具体实现并自动跳过。
 * </p>
 *
 * @author jymot
 * @since 2026-02-13
 */
@Configuration
@AutoConfigureBefore(AccountDomainAutoConfiguration.class)
@EnableConfigurationProperties(SecurityEventProperties.class)
@MapperScan(basePackageClasses = MapperModule.class)
@ComponentScan(basePackageClasses = {TaskModule.class, ServiceModule.class})
public class AccountAdapterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LockStatePort.class)
    public LockStatePort lockStatePort(AccountLockStateMapper lockStateMapper) {
        return new DefaultLockStatePortAdapter(lockStateMapper);
    }

    @Bean
    @ConditionalOnClass(RemoteSecurityEventService.class)
    @ConditionalOnMissingBean(RemoteSecurityEventPortAdapter.class)
    public RemoteSecurityEventPortAdapter remoteSecurityEventPortAdapter(
            org.springframework.beans.factory.ObjectProvider<RemoteSecurityEventService> remoteProvider,
            SecurityEventProperties properties,
            AccountSecurityEventReportMapper reportMapper) {
        return new RemoteSecurityEventPortAdapter(remoteProvider, properties, reportMapper);
    }

    @Bean
    @ConditionalOnMissingBean(AccountSecurityEventReportMapper.class)
    public AccountSecurityEventReportMapper accountSecurityEventReportMapper(
            SecurityEventProperties properties,
            ObjectMapper objectMapper) {
        return new AccountSecurityEventReportMapper(properties, objectMapper);
    }

    /**
     * 注册唯一 {@link SecurityEventPort} 入口。
     * <p>{@link DefaultSecurityEventPortAdapter} 不可单独声明为 {@code @Bean}：
     * 其实现了 {@link SecurityEventPort}，会导致本方法上的
     * {@link ConditionalOnMissingBean} 误判，{@link CompositeSecurityEventPort} 永远无法装配。</p>
     */
    @Bean
    @ConditionalOnMissingBean(SecurityEventPort.class)
    public SecurityEventPort securityEventPort(
            AccountSecurityEventMapper securityEventMapper,
            SecurityEventProperties properties,
            ObjectProvider<RemoteSecurityEventPortAdapter> remoteProvider) {
        DefaultSecurityEventPortAdapter localPort = new DefaultSecurityEventPortAdapter(securityEventMapper);
        return new CompositeSecurityEventPort(localPort, remoteProvider.getIfAvailable(), properties);
    }
}
