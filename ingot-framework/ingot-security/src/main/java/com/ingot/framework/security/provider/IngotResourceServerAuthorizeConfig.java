package com.ingot.framework.security.provider;

import com.ingot.framework.security.annotation.IgnoreUserAuthentication;
import com.ingot.framework.security.provider.authorize.ActuatorAuthorizeConfigProvider;
import com.ingot.framework.security.provider.authorize.AuthorizePermitConfigProvider;
import com.ingot.framework.security.provider.authorize.SecurityFilterConfigProvider;
import com.ingot.framework.security.service.ResourcePermitService;
import com.ingot.framework.security.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * <p>Description  : IngotResourceServerAuthorizeConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/30.</p>
 * <p>Time         : 下午12:16.</p>
 */
@Slf4j
public class IngotResourceServerAuthorizeConfig {

    @Bean
    public ActuatorAuthorizeConfigProvider actuatorProvider(WebEndpointProperties properties) {
        return new ActuatorAuthorizeConfigProvider(properties);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public AuthorizePermitConfigProvider permitProvider(@Value("${spring.application.name}") String applicationName,
                                                        ResourcePermitService resourcePermitService) {
        return new AuthorizePermitConfigProvider(applicationName, resourcePermitService);
    }

    @Bean
    @ConditionalOnMissingBean(annotation = IgnoreUserAuthentication.class)
    public SecurityFilterConfigProvider filterProvider(ResourcePermitService resourcePermitService,
                                                       TokenService tokenService) {
        return new SecurityFilterConfigProvider(resourcePermitService, tokenService);
    }

}
