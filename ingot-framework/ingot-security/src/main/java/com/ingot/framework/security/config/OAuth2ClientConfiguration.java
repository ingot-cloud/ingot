package com.ingot.framework.security.config;

import com.ingot.framework.security.provider.IngotAccessDeniedHandler;
import com.ingot.framework.security.provider.IngotAuthenticationEntryPoint;
import com.ingot.framework.security.provider.ClientToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.provider.expression.OAuth2WebSecurityExpressionHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static com.ingot.framework.core.constants.BeanIds.*;

/**
 * <p>Description  : OAuth2ClientConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/19.</p>
 * <p>Time         : 上午11:07.</p>
 */
@Slf4j
@Configuration
public class OAuth2ClientConfiguration {

    @Bean(OAUTH2_REST_TEMPLATE)
    public OAuth2RestTemplate oAuth2RestTemplate(OAuth2ClientContext oAuth2ClientContext,
                                                 ClientCredentialsResourceDetails details) {
        final OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(details, oAuth2ClientContext);
        oAuth2RestTemplate.setRequestFactory(new OkHttp3ClientHttpRequestFactory());
        return oAuth2RestTemplate;
    }

    @Bean(ACCESS_DENIED_HANDLER)
    public IngotAccessDeniedHandler ingotAccessDeniedHandler() {
        return new IngotAccessDeniedHandler();
    }

    @Bean(AUTHENTICATION_ENTRY_POINT)
    public IngotAuthenticationEntryPoint ingotAuthenticationEntryPoint(HandlerExceptionResolver handlerExceptionResolver) {
        return new IngotAuthenticationEntryPoint(handlerExceptionResolver);
    }

    @Bean
    public OAuth2WebSecurityExpressionHandler oAuth2WebSecurityExpressionHandler(ApplicationContext applicationContext) {
        OAuth2WebSecurityExpressionHandler expressionHandler = new OAuth2WebSecurityExpressionHandler();
        expressionHandler.setApplicationContext(applicationContext);
        return expressionHandler;
    }

    @Bean(CLIENT_TOKEN_UTILS)
    public ClientToken clientToken(OAuth2ClientContext oAuth2ClientContext,
                                   OAuth2RestTemplate ingotOAuth2RestTemplate,
                                   @Value("${spring.application.name}") String applicationName){
        return new ClientToken(oAuth2ClientContext, ingotOAuth2RestTemplate, applicationName);
    }
}
