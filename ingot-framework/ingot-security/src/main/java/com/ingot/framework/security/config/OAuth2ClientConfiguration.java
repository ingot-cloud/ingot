package com.ingot.framework.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;

import static com.ingot.framework.core.constants.BeanIds.OAUTH2_REST_TEMPLATE;

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
}
