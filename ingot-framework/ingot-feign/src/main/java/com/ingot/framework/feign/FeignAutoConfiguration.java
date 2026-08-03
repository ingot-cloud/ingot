package com.ingot.framework.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.feign.codec.InErrorDecoder;
import com.ingot.framework.feign.reactive.FeignReactiveContextWebFilter;
import com.ingot.framework.feign.reactive.ReactiveOAuth2FeignRequestInterceptor;
import feign.Feign;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.server.ServerWebExchange;

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
        return new InErrorDecoder();
    }

    /**
     * Servlet 应用（BFF / Provider 等）沿用的 Feign 拦截器，逻辑保持不变。
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(name = "oauth2FeignRequestInterceptor")
    public RequestInterceptor oauth2FeignRequestInterceptor() {
        return new OAuth2FeignRequestInterceptor();
    }

    /**
     * WebFlux / Gateway 环境：不依赖 {@code HttpServletRequest}，语义与 Servlet 拦截器一致。
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnMissingBean(name = "reactiveOAuth2FeignRequestInterceptor")
    public RequestInterceptor reactiveOAuth2FeignRequestInterceptor() {
        return new ReactiveOAuth2FeignRequestInterceptor();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnClass(ServerWebExchange.class)
    @ConditionalOnMissingBean
    public FeignReactiveContextWebFilter feignReactiveContextWebFilter() {
        return new FeignReactiveContextWebFilter();
    }

    /**
     * WebFlux 无 {@code spring-boot-starter-web}，不会注册 {@link HttpMessageConverters}，
     * 而 OpenFeign 的 {@code SpringDecoder}/{@code SpringEncoder} 依赖该 Bean。
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnMissingBean(HttpMessageConverters.class)
    public HttpMessageConverters feignHttpMessageConverters(
            ObjectProvider<HttpMessageConverter<?>> converters,
            ObjectProvider<ObjectMapper> objectMappers) {
        List<HttpMessageConverter<?>> messageConverters = converters.orderedStream().toList();
        if (messageConverters.isEmpty()) {
            ObjectMapper objectMapper = objectMappers.getIfAvailable(ObjectMapper::new);
            return new HttpMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper));
        }
        return new HttpMessageConverters(messageConverters);
    }
}
