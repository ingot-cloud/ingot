package com.ingot.framework.store.tenant;

import com.ingot.framework.store.tenant.web.TenantFilter;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.ClientHttpRequestInterceptor;

/**
 * <p>Description  : TenantConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/23.</p>
 * <p>Time         : 6:05 下午.</p>
 */
@Configuration
public class TenantConfig {

    @Bean
    public RequestInterceptor feignTenantInterceptor() {
        return new TenantFeignInterceptor();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public TenantFilter tenantFilter(){
        return new TenantFilter();
    }

    @Bean
    public ClientHttpRequestInterceptor tenantRequestInterceptor() {
        return new TenantRequestInterceptor();
    }
}
