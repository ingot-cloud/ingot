package com.ingot.framework.tenant;

import com.ingot.framework.tenant.interceptor.TenantFeignInterceptor;
import com.ingot.framework.tenant.interceptor.TenantRequestInterceptor;
import com.ingot.framework.tenant.properties.TenantProperties;
import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestInterceptor;

/**
 * <p>Description  : TenantConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/23.</p>
 * <p>Time         : 6:05 下午.</p>
 */
@AutoConfiguration
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

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public TenantHttpConfigurer tenantHttpConfigurer(TenantProperties tenantProperties) {
        return new TenantHttpConfigurer(tenantProperties);
    }
}
