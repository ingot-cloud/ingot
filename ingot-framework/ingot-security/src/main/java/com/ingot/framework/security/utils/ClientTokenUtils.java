package com.ingot.framework.security.utils;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.ingot.framework.core.constants.BeanIds.CLIENT_TOKEN_UTILS;

/**
 * <p>Description  : ClientTokenUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/19.</p>
 * <p>Time         : 上午11:28.</p>
 */
@Slf4j
@Component(CLIENT_TOKEN_UTILS)
public class ClientTokenUtils {

    @Resource
    private OAuth2ClientContext oAuth2ClientContext;
    @Resource
    private OAuth2RestTemplate ingotOAuth2RestTemplate;
    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * 获取 access token
     */
    public String getAccessToken(){
        OAuth2AccessToken accessToken = oAuth2ClientContext.getAccessToken();
        if (accessToken == null || accessToken.isExpired()) {
            accessToken = ingotOAuth2RestTemplate.getAccessToken();
        }
        return accessToken.getValue();
    }

    /**
     * 刷新 access token
     * @return {@link String} token
     */
    public String refreshAccessToken(){
        log.info(">>> ClientTokenUtils - refreshAccessToken start --- {}", DateTime.now());
        oAuth2ClientContext.setAccessToken(null);
        OAuth2AccessToken accessToken = ingotOAuth2RestTemplate.getAccessToken();
        log.info(">>> ClientTokenUtils - refreshAccessToken end --- {}", DateTime.now());
        return accessToken.getValue();
    }

}
