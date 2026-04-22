package com.ingot.framework.account.web.support;

import com.ingot.framework.account.domain.config.AccountDomainProperties;
import com.ingot.framework.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.security.credential.service.CredentialSecurityService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Web/登录认证场景共享工具的自动配置。
 * <p>{@link CredentialSecurityService} / {@link LockStatePort} / {@link AccountDomainProperties}
 * 全部以 {@link ObjectProvider} 方式按需注入，任意一项缺失时 {@link AuthContextSupport}
 * 会自动降级，允许仅启用账号域基础能力的服务（如 Member baseline）直接接入。</p>
 *
 * @author jymot
 * @since 2026-02-14
 */
@Configuration(proxyBeanMethods = false)
public class AccountWebSupportAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuthContextSupport authContextSupport(
            ObjectProvider<CredentialSecurityService> credentialSecurityService,
            ObjectProvider<LockStatePort> lockStatePort,
            ObjectProvider<AccountDomainProperties> accountProperties) {
        return new AuthContextSupport(
                credentialSecurityService.getIfAvailable(),
                lockStatePort.getIfAvailable(),
                accountProperties.getIfAvailable());
    }
}
