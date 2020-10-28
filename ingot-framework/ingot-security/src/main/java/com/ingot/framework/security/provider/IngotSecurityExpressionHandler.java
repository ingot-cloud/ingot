package com.ingot.framework.security.provider;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.provider.expression.OAuth2WebSecurityExpressionHandler;

/**
 * <p>Description  : IngotSecurityExpressionHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/17.</p>
 * <p>Time         : 下午5:38.</p>
 */
@Configuration
public class IngotSecurityExpressionHandler extends OAuth2WebSecurityExpressionHandler {

    @Bean
    public OAuth2WebSecurityExpressionHandler oAuth2WebSecurityExpressionHandler(ApplicationContext applicationContext) {
        OAuth2WebSecurityExpressionHandler expressionHandler = new OAuth2WebSecurityExpressionHandler();
        expressionHandler.setApplicationContext(applicationContext);
        return expressionHandler;
    }
}
