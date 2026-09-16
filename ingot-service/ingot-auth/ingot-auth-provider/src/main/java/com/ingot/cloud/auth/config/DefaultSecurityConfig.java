package com.ingot.cloud.auth.config;

import com.ingot.cloud.auth.service.biz.impl.CachingRemoteUserDetailsService;
import com.ingot.cloud.auth.service.biz.impl.DefaultMemberRemoteUserDetailsService;
import com.ingot.cloud.auth.service.biz.impl.DefaultIamRemoteUserDetailsService;
import com.ingot.cloud.auth.service.biz.impl.DefaultRemoteTenantDetailsService;
import com.ingot.cloud.member.api.rpc.RemoteMemberUserDetailsService;
import com.ingot.cloud.iam.api.rpc.RemoteIamTenantDetailsService;
import com.ingot.cloud.iam.api.rpc.RemoteIamUserDetailsService;
import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
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
    public RemoteUserDetailsService iamRemoteUserDetailsService(
            RemoteIamUserDetailsService remoteIamUserDetailsService,
            AccountLockSignalPort accountLockSignalPort) {
        return new CachingRemoteUserDetailsService(
                new DefaultIamRemoteUserDetailsService(remoteIamUserDetailsService),
                accountLockSignalPort);
    }

    @Bean
    public RemoteUserDetailsService memberRemoteUserDetailsService(
            RemoteMemberUserDetailsService remoteMemberUserDetailsService,
            AccountLockSignalPort accountLockSignalPort) {
        return new CachingRemoteUserDetailsService(
                new DefaultMemberRemoteUserDetailsService(remoteMemberUserDetailsService),
                accountLockSignalPort);
    }

    @Bean
    public RemoteTenantDetailsService remoteTenantDetailsService(RemoteIamTenantDetailsService remoteIamTenantDetailsService) {
        return new DefaultRemoteTenantDetailsService(remoteIamTenantDetailsService);
    }
}
