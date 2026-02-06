package com.ingot.framework.security.credential.config;

import com.ingot.cloud.security.api.rpc.RemoteCredentialService;
import com.ingot.framework.security.credential.service.CredentialPolicyLoader;
import com.ingot.framework.security.credential.service.CredentialSecurityService;
import com.ingot.framework.security.credential.service.PasswordExpirationService;
import com.ingot.framework.security.credential.service.PasswordHistoryService;
import com.ingot.framework.security.credential.service.impl.*;
import com.ingot.framework.security.credential.validator.DefaultPasswordValidator;
import com.ingot.framework.security.credential.validator.PasswordValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 凭证安全自动配置
 *
 * @author jymot
 * @since 2026-01-21
 */
@AutoConfiguration
@EnableConfigurationProperties(CredentialSecurityProperties.class)
public class CredentialSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CredentialPolicyLoader.class)
    @ConditionalOnProperty(name = "ingot.credential.policy.mode", havingValue = "local", matchIfMissing = true)
    public CredentialPolicyLoader localCredentialPolicyLoader(CredentialSecurityProperties properties,
                                                              PasswordEncoder passwordEncoder) {
        return new LocalCredentialPolicyLoader(properties, passwordEncoder);
    }

    @Bean
    @ConditionalOnMissingBean(CredentialPolicyLoader.class)
    @ConditionalOnProperty(name = "ingot.credential.policy.mode", havingValue = "remote")
    public CredentialPolicyLoader credentialPolicyLoader(RemoteCredentialService remoteCredentialService,
                                                         PasswordEncoder passwordEncoder) {
        return new RemoteCredentialPolicyLoader(remoteCredentialService, passwordEncoder);
    }

    @Bean
    @ConditionalOnMissingBean(PasswordValidator.class)
    public PasswordValidator passwordValidator(CredentialPolicyLoader policyLoader) {
        return new DefaultPasswordValidator(policyLoader);
    }

    @Bean
    @ConditionalOnMissingBean(PasswordHistoryService.class)
    public PasswordHistoryService passwordHistoryService() {
        return new NoOpPasswordHistoryService();
    }

    @Bean
    @ConditionalOnMissingBean(PasswordExpirationService.class)
    public PasswordExpirationService passwordExpirationService() {
        return new NoOpPasswordExpirationService();
    }

    @Bean
    @ConditionalOnMissingBean(CredentialSecurityService.class)
    public CredentialSecurityService credentialSecurityService(
            PasswordValidator passwordValidator,
            PasswordHistoryService passwordHistoryService,
            PasswordExpirationService passwordExpirationService,
            CredentialSecurityProperties properties,
            CredentialPolicyLoader credentialPolicyLoader) {
        return new DefaultCredentialSecurityService(
                passwordValidator,
                passwordHistoryService,
                passwordExpirationService,
                properties,
                credentialPolicyLoader
        );
    }
}
