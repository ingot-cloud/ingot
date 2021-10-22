package com.ingot.framework.security.config.annotation.web.configurers.oauth2.server.resource;

import com.ingot.framework.security.web.IngotTokenAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * <p>Description  : IngotTokenAuthConfigurer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/22.</p>
 * <p>Time         : 5:09 下午.</p>
 */
@RequiredArgsConstructor
public class IngotTokenAuthConfigurer<H extends HttpSecurityBuilder<H>>
        extends AbstractHttpConfigurer<OAuth2InnerResourceConfigurer<H>, H> {
    private final RequestMatcher ignoreRequestMatcher;

    @Override
    public void configure(H builder) throws Exception {
        IngotTokenAuthFilter filter = new IngotTokenAuthFilter(this.ignoreRequestMatcher);
        builder.addFilterAfter(postProcess(filter), BearerTokenAuthenticationFilter.class);
    }
}
