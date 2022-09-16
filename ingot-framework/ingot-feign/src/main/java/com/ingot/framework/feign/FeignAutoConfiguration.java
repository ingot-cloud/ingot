package com.ingot.framework.feign;

import com.ingot.framework.feign.codec.IngotErrorDecoder;
import feign.Feign;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * <p>Description  : FeignAutoConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/12/31.</p>
 * <p>Time         : 4:11 下午.</p>
 */
@AutoConfiguration
@ConditionalOnClass(Feign.class)
public class FeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ErrorDecoder errorDecoder() {
        return new IngotErrorDecoder();
    }

    @Bean
    public RequestInterceptor oauth2FeignRequestInterceptor() {
        return new OAuth2FeignRequestInterceptor();
    }
}
