package com.ingot.framework.sentinel;

import com.alibaba.cloud.sentinel.feign.SentinelFeignAutoConfiguration;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.RequestOriginParser;
import com.ingot.framework.sentinel.feign.SentinelFeignBuilder;
import com.ingot.framework.sentinel.webmvc.callback.IngotBlockExceptionHandler;
import com.ingot.framework.sentinel.webmvc.callback.IngotRequestOriginParser;
import feign.Feign;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

/**
 * <p>Description  : SentinelAutoConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/12/31.</p>
 * <p>Time         : 10:29 上午.</p>
 */
@AutoConfiguration
@AutoConfigureBefore(SentinelFeignAutoConfiguration.class)
public class SentinelAutoConfiguration {

    @Bean
    @Scope("prototype")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.cloud.openfeign.sentinel.enabled", matchIfMissing = true)
    public Feign.Builder feignSentinelBuilder() {
        return new SentinelFeignBuilder();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public BlockExceptionHandler blockExceptionHandler() {
        return new IngotBlockExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public RequestOriginParser requestOriginParser() {
        return new IngotRequestOriginParser();
    }

}
