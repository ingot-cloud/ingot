package com.ingot.framework.security.core.authorize.provider;

import com.ingot.framework.security.core.authorize.AuthorizeConfigProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

/**
 * <p>Description  : AuthorizeActuatorConfigProvider.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/21.</p>
 * <p>Time         : 11:37 AM.</p>
 */
@Slf4j
@Component
@AllArgsConstructor
public class ActuatorAuthorizeConfigProvider implements AuthorizeConfigProvider {
    private final WebEndpointProperties webEndpointProperties;

    @Override public boolean config(HttpSecurity http) throws Exception {
        log.info(">>> AuthorizeActuatorConfigProvider - {} 端点 permitAll", webEndpointProperties.getBasePath());
        http.authorizeRequests().antMatchers(webEndpointProperties.getBasePath() + "/**")
                .permitAll(); // permit /actuator 端点
        return false;
    }
}
