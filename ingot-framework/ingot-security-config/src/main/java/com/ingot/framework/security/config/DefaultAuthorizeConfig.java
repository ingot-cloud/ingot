package com.ingot.framework.security.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * <p>Description  : DefaultAuthorizeConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/25.</p>
 * <p>Time         : 3:48 下午.</p>
 */
public class DefaultAuthorizeConfig {

    @Bean
    @ConditionalOnMissingBean(AuthorizeConfigManager.class)
    public AuthorizeConfigManager authorizeConfigManager(List<AuthorizeConfigProvider> providers) {
        return new DefaultAuthorizeConfigManager(providers);
    }
}
