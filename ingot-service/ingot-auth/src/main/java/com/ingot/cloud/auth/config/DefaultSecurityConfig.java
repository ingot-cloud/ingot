package com.ingot.cloud.auth.config;

import com.ingot.cloud.auth.service.DefaultRemoteTenantDetailsService;
import com.ingot.cloud.auth.service.DefaultRemoteUserDetailsService;
import com.ingot.cloud.pms.api.rpc.PmsUserDetailsService;
import com.ingot.framework.security.core.tenantdetails.RemoteTenantDetailsService;
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
    public RemoteUserDetailsService remoteUserDetailsService(PmsUserDetailsService pmsApi) {
        return new DefaultRemoteUserDetailsService(pmsApi);
    }

    @Bean
    public RemoteTenantDetailsService remoteTenantDetailsService(PmsUserDetailsService pmsApi) {
        return new DefaultRemoteTenantDetailsService(pmsApi);
    }
}
