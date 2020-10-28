package com.ingot.framework.security.config;

import com.ingot.framework.security.core.feign.OAuth2FeignErrorDecoder;
import com.ingot.framework.security.core.feign.interceptor.OAuth2FeignRequestInterceptor;
import com.ingot.framework.security.utils.ClientTokenUtils;
import feign.Feign;
import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Description  : OAuth2FeignConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/12.</p>
 * <p>Time         : 下午10:29.</p>
 */
@Slf4j
@Configuration
@ConditionalOnClass(Feign.class)
public class OAuth2DefaultFeignConfiguration {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new OAuth2FeignErrorDecoder();
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor oauth2FeignRequestInterceptor(ClientTokenUtils clientTokenUtils) {
        return new OAuth2FeignRequestInterceptor(clientTokenUtils);
    }

}
