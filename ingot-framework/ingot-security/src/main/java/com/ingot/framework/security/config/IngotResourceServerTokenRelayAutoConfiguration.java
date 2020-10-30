package com.ingot.framework.security.config;

import com.ingot.framework.security.provider.IngotAccessTokenContextRelay;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2AutoConfiguration;
import org.springframework.cloud.security.oauth2.client.AccessTokenContextRelay;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2ClientContext;

/**
 * <p>Description  : IngotResourceServerTokenRelayAutoConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/26.</p>
 * <p>Time         : 11:27 AM.</p>
 */
@Configuration
@AutoConfigureAfter(OAuth2AutoConfiguration.class)
@ConditionalOnWebApplication
@ConditionalOnProperty("security.oauth2.client.client-id")
public class IngotResourceServerTokenRelayAutoConfiguration {

    @Bean
    public AccessTokenContextRelay accessTokenContextRelay(OAuth2ClientContext context) {
        return new IngotAccessTokenContextRelay(context);
    }
}
