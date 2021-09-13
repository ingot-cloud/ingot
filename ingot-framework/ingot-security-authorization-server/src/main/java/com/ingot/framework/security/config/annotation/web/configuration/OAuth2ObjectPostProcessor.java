package com.ingot.framework.security.config.annotation.web.configuration;

import com.ingot.framework.security.oauth2.server.authorization.web.authentication.IngotAuthenticationFailureHandler;
import com.ingot.framework.security.oauth2.server.authorization.web.authentication.OAuth2PasswordAuthenticationConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.oauth2.server.authorization.web.OAuth2ClientAuthenticationFilter;
import org.springframework.security.oauth2.server.authorization.web.OAuth2TokenEndpointFilter;
import org.springframework.security.oauth2.server.authorization.web.authentication.DelegatingAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2ClientCredentialsAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2RefreshTokenAuthenticationConverter;

import java.util.Arrays;

/**
 * <p>Description  : OAuth2ObjectPostProcessor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/8.</p>
 * <p>Time         : 5:06 下午.</p>
 */
@Slf4j
public class OAuth2ObjectPostProcessor implements ObjectPostProcessor<Object> {

    @Override
    public <O> O postProcess(O object) {
        // 替换 OAuth2TokenEndpointFilter 中的 AuthenticationConverter
        if (object instanceof OAuth2TokenEndpointFilter) {
            ((OAuth2TokenEndpointFilter) object).setAuthenticationConverter(
                    new DelegatingAuthenticationConverter(
                            Arrays.asList(
                                    new OAuth2AuthorizationCodeAuthenticationConverter(),
                                    new OAuth2RefreshTokenAuthenticationConverter(),
                                    new OAuth2ClientCredentialsAuthenticationConverter(),
                                    new OAuth2PasswordAuthenticationConverter())
                    )
            );

            ((OAuth2TokenEndpointFilter) object).setAuthenticationFailureHandler(
                    new IngotAuthenticationFailureHandler());
        }
        else if (object instanceof OAuth2ClientAuthenticationFilter) {
            ((OAuth2ClientAuthenticationFilter) object).setAuthenticationFailureHandler(
                    new IngotAuthenticationFailureHandler());
        }
        return object;
    }
}
