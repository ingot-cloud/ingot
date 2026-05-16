package com.ingot.cloud.security.service.credential;

import com.ingot.cloud.security.mapper.CredentialPolicyConfigMapper;
import com.ingot.framework.security.credential.config.CredentialSecurityAutoConfiguration;
import com.ingot.framework.security.credential.service.CredentialPolicyConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ingot-security-provider 进程内的凭证策略配置 delegate 装配。
 * <p>
 * 仅注册 L0 delegate（{@link LocalCredentialPolicyConfigService}）作为
 * {@code credentialPolicyConfigDelegate} bean。L1 Caffeine、L2 Redis、对外的
 * {@link CredentialPolicyConfigService} {@code @Primary} bean 与
 * {@code CredentialCacheCoordinator} 均由 {@link CredentialSecurityAutoConfiguration} 统一组合。
 * </p>
 *
 * @author jy
 * @since 2026/5/16
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class LocalCredentialPolicyConfigConfig {

    @Bean(name = CredentialSecurityAutoConfiguration.CREDENTIAL_POLICY_CONFIG_DELEGATE)
    public CredentialPolicyConfigService credentialPolicyConfigDelegate(CredentialPolicyConfigMapper policyConfigMapper) {
        log.info("[Credential] register local delegate (LocalCredentialPolicyConfigService)");
        return new LocalCredentialPolicyConfigService(policyConfigMapper);
    }
}
