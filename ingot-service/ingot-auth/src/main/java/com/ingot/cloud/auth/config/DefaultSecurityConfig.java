package com.ingot.cloud.auth.config;

import com.ingot.cloud.pms.api.rpc.PmsUserAuthFeignApi;
import com.ingot.framework.security.core.userdetails.RemoteUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Description  : DefaultSecurityConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/6.</p>
 * <p>Time         : 11:16 上午.</p>
 */
@Configuration(proxyBeanMethods = false)
public class DefaultSecurityConfig {

    @Bean
    public RemoteUserDetailsService remoteUserDetailsService(PmsUserAuthFeignApi pmsApi) {
        return (params) -> pmsApi.getUserAuthDetail(params.getUsername());
    }
}
