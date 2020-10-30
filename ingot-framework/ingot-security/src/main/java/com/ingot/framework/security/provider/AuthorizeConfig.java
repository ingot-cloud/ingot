package com.ingot.framework.security.provider;

import com.ingot.framework.security.config.IngotSecurityFilterConfig;
import com.ingot.framework.security.core.authorize.AuthorizeConfigManager;
import com.ingot.framework.security.core.authorize.AuthorizeConfigProvider;
import com.ingot.framework.security.core.authorize.manager.IngotAuthorizeConfigManager;
import com.ingot.framework.security.core.authorize.provider.ActuatorAuthorizeConfigProvider;
import com.ingot.framework.security.core.authorize.provider.AuthorizePermitConfigProvider;
import com.ingot.framework.security.core.authorize.provider.IngotFilterConfigProvider;
import com.ingot.framework.security.service.ResourcePermitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * <p>Description  : AuthorizeConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/30.</p>
 * <p>Time         : 下午12:16.</p>
 */
public class AuthorizeConfig {

    @Bean
    @ConditionalOnMissingBean(AuthorizeConfigManager.class)
    public AuthorizeConfigManager authorizeConfigManager(List<AuthorizeConfigProvider> providers) {
        return new IngotAuthorizeConfigManager(providers);
    }

    @Bean
    public ActuatorAuthorizeConfigProvider actuatorProvider(WebEndpointProperties properties){
        return new ActuatorAuthorizeConfigProvider(properties);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public AuthorizePermitConfigProvider permitProvider(@Value("${spring.application.name}") String applicationName,
                                                        ResourcePermitService resourcePermitService) {
        return new AuthorizePermitConfigProvider(applicationName, resourcePermitService);
    }

    @Bean
    public IngotFilterConfigProvider filterProvider(IngotSecurityFilterConfig ingotSecurityFilterConfig){
        return new IngotFilterConfigProvider(ingotSecurityFilterConfig);
    }

}
