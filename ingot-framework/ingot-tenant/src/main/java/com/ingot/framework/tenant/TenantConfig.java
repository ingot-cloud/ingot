package com.ingot.framework.tenant;

import com.ingot.framework.tenant.interceptor.TenantFeignInterceptor;
import com.ingot.framework.tenant.interceptor.TenantRequestInterceptor;
import com.ingot.framework.tenant.properties.TenantProperties;
import feign.RequestInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;

/**
 * <p>Description  : TenantConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/23.</p>
 * <p>Time         : 6:05 下午.</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(value = TenantProperties.class)
public class TenantConfig {

    @Bean
    public RequestInterceptor feignTenantInterceptor() {
        return new TenantFeignInterceptor();
    }

    @Bean
    public ClientHttpRequestInterceptor tenantRequestInterceptor() {
        return new TenantRequestInterceptor();
    }
}
