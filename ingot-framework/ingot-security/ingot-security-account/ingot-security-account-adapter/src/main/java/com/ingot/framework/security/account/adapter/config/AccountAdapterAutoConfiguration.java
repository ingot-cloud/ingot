package com.ingot.framework.security.account.adapter.config;

import com.ingot.framework.security.account.adapter.mapper.AccountLockStateMapper;
import com.ingot.framework.security.account.adapter.mapper.MapperModule;
import com.ingot.framework.security.account.adapter.port.DefaultLockStatePortAdapter;
import com.ingot.framework.security.account.adapter.service.ServiceModule;
import com.ingot.framework.security.account.adapter.task.TaskModule;
import com.ingot.framework.security.account.domain.config.AccountDomainAutoConfiguration;
import com.ingot.framework.security.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.security.recording.config.SecurityEventProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * account-adapter 模块自动配置
 * <p>
 * 提供基于 MyBatis-Plus 的 LockStatePort 实现；SecurityEventPort 由
 * {@link AccountSecurityEventPortAutoConfiguration} 在 recording 就绪后注册。
 * 通过 {@link AutoConfigureBefore} 确保在 {@link AccountDomainAutoConfiguration} 之前处理，
 * 使 core 模块的 Port NoOp 回退能正确让位给 adapter / recording 装配链。
 * </p>
 *
 * @author jymot
 * @since 2026-02-13
 */
@AutoConfiguration
@AutoConfigureBefore({
        AccountDomainAutoConfiguration.class,
        AccountSecurityEventPortAutoConfiguration.class
})
@EnableConfigurationProperties(SecurityEventProperties.class)
@MapperScan(basePackageClasses = MapperModule.class)
@ComponentScan(basePackageClasses = {TaskModule.class, ServiceModule.class})
public class AccountAdapterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LockStatePort.class)
    public LockStatePort lockStatePort(AccountLockStateMapper lockStateMapper) {
        return new DefaultLockStatePortAdapter(lockStateMapper);
    }
}
