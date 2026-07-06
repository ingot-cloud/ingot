package com.ingot.framework.security.config.annotation.web.configurers.oauth2.server.resource;

import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import com.ingot.framework.security.web.InTokenAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * <p>Description  : Token认证配置.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/22.</p>
 * <p>Time         : 5:09 下午.</p>
 */
@RequiredArgsConstructor
public class InTokenAuthConfigurer
        extends AbstractHttpConfigurer<InTokenAuthConfigurer, HttpSecurity> {
    private final RequestMatcher ignoreRequestMatcher;
    private final OnlineTokenService onlineTokenService;

    @Override
    public void configure(HttpSecurity builder) throws Exception {
        InTokenAuthFilter filter = new InTokenAuthFilter(this.ignoreRequestMatcher, onlineTokenService);
        builder.addFilterAfter(postProcess(filter), BearerTokenAuthenticationFilter.class);
    }
}
