package com.ingot.framework.security.config;

import lombok.AllArgsConstructor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import java.util.List;

/**
 * <p>Description  : DefaultSecurityConfigManager.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/30.</p>
 * <p>Time         : 下午12:14.</p>
 */
@AllArgsConstructor
public class DefaultSecurityConfigManager implements SecurityConfigManager {
    private final List<SecurityConfigProvider> securityConfigProviders;

    @Override
    public void config(HttpSecurity http) throws Exception {
        boolean existAnyRequestConfig = false;
        String existAnyRequestConfigName = null;

        for (SecurityConfigProvider securityConfigProvider : securityConfigProviders) {
            boolean currentIsAnyRequestConfig = securityConfigProvider.config(http);
            if (existAnyRequestConfig && currentIsAnyRequestConfig) {
                throw new RuntimeException(">>> IngotAuthorizeConfigManager: " + existAnyRequestConfigName + ", " + securityConfigProvider.getClass().getSimpleName());
            } else if (currentIsAnyRequestConfig) {
                existAnyRequestConfig = true;
                existAnyRequestConfigName = securityConfigProvider.getClass().getSimpleName();
            }
        }

        if (!existAnyRequestConfig) {
            http.authorizeRequests().anyRequest().authenticated();
        }
    }
}
