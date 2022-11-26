package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration;

import com.ingot.framework.security.oauth2.server.authorization.web.authentication.AccessTokenAuthenticationFailureHandler;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.oauth2.server.authorization.web.OAuth2ClientAuthenticationFilter;

/**
 * <p>Description  : OAuth2ClientAuthenticationFilterPostProcessor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/15.</p>
 * <p>Time         : 10:22 上午.</p>
 */
public class OAuth2ClientAuthenticationFilterPostProcessor implements ObjectPostProcessor<OAuth2ClientAuthenticationFilter> {
    @Override
    public <O extends OAuth2ClientAuthenticationFilter> O postProcess(O filter) {
        filter.setAuthenticationFailureHandler(new AccessTokenAuthenticationFailureHandler());
        return filter;
    }
}
