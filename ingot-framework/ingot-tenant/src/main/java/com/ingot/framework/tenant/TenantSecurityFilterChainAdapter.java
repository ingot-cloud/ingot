package com.ingot.framework.tenant;

import com.ingot.framework.tenant.filter.TenantFilter;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.provider.client.ClientCredentialsTokenEndpointFilter;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.stereotype.Component;

/**
 * <p>Description  : TenantSecurityFilterChainAdapter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/24.</p>
 * <p>Time         : 10:40 下午.</p>
 */
public class TenantSecurityFilterChainAdapter extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

    @Override
    public void configure(HttpSecurity builder) throws Exception {
        TenantFilter filter = new TenantFilter();
        builder.addFilterBefore(filter, ClientCredentialsTokenEndpointFilter.class);
    }
}
