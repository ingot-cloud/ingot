package com.ingot.framework.security.config;

import com.ingot.framework.security.core.authorize.AuthorizeConfigManager;
import com.ingot.framework.security.core.authorize.manager.IngotAuthorizeConfigManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * <p>Description  : AuthorizeConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/30.</p>
 * <p>Time         : 下午12:16.</p>
 */
public class AuthorizeConfig {

    @Bean
    @ConditionalOnMissingBean(AuthorizeConfigManager.class)
    public AuthorizeConfigManager authorizeConfigManager(){
        return new IngotAuthorizeConfigManager();
    }
}
