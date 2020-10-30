package com.ingot.framework.security.core.authorize.manager;

import com.ingot.framework.security.core.authorize.AuthorizeConfigManager;
import com.ingot.framework.security.core.authorize.AuthorizeConfigProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import java.util.List;

/**
 * <p>Description  : IngotAuthorizeConfigManager.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/30.</p>
 * <p>Time         : 下午12:14.</p>
 */
@Slf4j
@AllArgsConstructor
public class IngotAuthorizeConfigManager implements AuthorizeConfigManager {
    private final List<AuthorizeConfigProvider> authorizeConfigProviders;

    @Override public void config(HttpSecurity http) throws Exception{
        log.info(">>> AuthorizeConfigProvider={}", authorizeConfigProviders);
        boolean existAnyRequestConfig = false;
        String existAnyRequestConfigName = null;

        for (AuthorizeConfigProvider authorizeConfigProvider : authorizeConfigProviders) {
            boolean currentIsAnyRequestConfig = authorizeConfigProvider.config(http);
            if (existAnyRequestConfig && currentIsAnyRequestConfig) {
                throw new RuntimeException(">>> IngotAuthorizeConfigManager: " + existAnyRequestConfigName + ", " + authorizeConfigProvider.getClass().getSimpleName());
            } else if (currentIsAnyRequestConfig) {
                existAnyRequestConfig = true;
                existAnyRequestConfigName = authorizeConfigProvider.getClass().getSimpleName();
            }
        }

        if (!existAnyRequestConfig) {
            http.authorizeRequests().anyRequest().authenticated();
        }
    }
}
