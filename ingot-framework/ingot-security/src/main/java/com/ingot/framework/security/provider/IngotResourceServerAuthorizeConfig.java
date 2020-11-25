package com.ingot.framework.security.provider;

import com.ingot.framework.security.annotation.IgnoreUserAuthentication;
import com.ingot.framework.security.core.authorize.AuthorizeConfigManager;
import com.ingot.framework.security.core.authorize.AuthorizeConfigProvider;
import com.ingot.framework.security.core.authorize.manager.IngotAuthorizeConfigManager;
import com.ingot.framework.security.core.authorize.provider.ActuatorAuthorizeConfigProvider;
import com.ingot.framework.security.core.authorize.provider.AuthorizePermitConfigProvider;
import com.ingot.framework.security.core.authorize.provider.SecurityFilterConfigProvider;
import com.ingot.framework.security.provider.filter.UserAuthenticationFilter;
import com.ingot.framework.security.service.AuthenticationService;
import com.ingot.framework.security.service.ResourcePermitService;
import com.ingot.framework.security.service.UserAccessTokenRedisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * <p>Description  : IngotResourceServerAuthorizeConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/30.</p>
 * <p>Time         : 下午12:16.</p>
 */
public class IngotResourceServerAuthorizeConfig {

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
    public SecurityFilterConfigProvider filterProvider(ResourcePermitService resourcePermitService,
                                                       @Lazy UserAuthenticationFilter userAuthenticationFilter){
        return new SecurityFilterConfigProvider(resourcePermitService, userAuthenticationFilter);
    }

    @Bean
    @ConditionalOnBean(UserAccessTokenRedisService.class)
    @ConditionalOnMissingBean(annotation = IgnoreUserAuthentication.class)
    public UserAuthenticationFilter userAuthenticationFilter(@Lazy UserAccessTokenRedisService userAccessTokenRedisService,
                                                             AuthenticationService authenticationService,
                                                             ResourcePermitService resourcePermitService){
        return new UserAuthenticationFilter(userAccessTokenRedisService, resourcePermitService, authenticationService);
    }

}
