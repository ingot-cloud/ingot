package com.ingot.cloud.auth.config;

import com.ingot.cloud.auth.service.DefaultMemberRemoteUserDetailsService;
import com.ingot.cloud.auth.service.DefaultRemoteTenantDetailsService;
import com.ingot.cloud.auth.service.DefaultPmsRemoteUserDetailsService;
import com.ingot.cloud.member.api.rpc.RemoteMemberUserDetailsService;
import com.ingot.cloud.pms.api.rpc.RemotePmsTenantDetailsService;
import com.ingot.cloud.pms.api.rpc.RemotePmsUserDetailsService;
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
    public RemoteUserDetailsService pmsRemoteUserDetailsService(RemotePmsUserDetailsService remotePmsUserDetailsService) {
        return new DefaultPmsRemoteUserDetailsService(remotePmsUserDetailsService);
    }

    @Bean
    public RemoteUserDetailsService memberRemoteUserDetailsService(RemoteMemberUserDetailsService remoteMemberUserDetailsService) {
        return new DefaultMemberRemoteUserDetailsService(remoteMemberUserDetailsService);
    }

    @Bean
    public RemoteTenantDetailsService remoteTenantDetailsService(RemotePmsTenantDetailsService remotePmsTenantDetailsService) {
        return new DefaultRemoteTenantDetailsService(remotePmsTenantDetailsService);
    }
}
