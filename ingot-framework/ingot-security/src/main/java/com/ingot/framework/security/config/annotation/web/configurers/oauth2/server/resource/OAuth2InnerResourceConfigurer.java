package com.ingot.framework.security.config.annotation.web.configurers.oauth2.server.resource;

import com.ingot.framework.security.oauth2.core.PermitResolver;
import com.ingot.framework.security.oauth2.server.resource.web.OAuth2InnerResourceFilter;
import lombok.Getter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * <p>Description  : OAuth2InnerResourceConfigurer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/23.</p>
 * <p>Time         : 11:30 上午.</p>
 */
@Getter
public class OAuth2InnerResourceConfigurer
        extends AbstractHttpConfigurer<OAuth2InnerResourceConfigurer, HttpSecurity> {
    private final RequestMatcher requestMatcher;

    public OAuth2InnerResourceConfigurer(PermitResolver permitResolver) {
        this.requestMatcher = permitResolver.innerRequestMatcher();
    }

    @Override
    public void configure(HttpSecurity builder) throws Exception {
        OAuth2InnerResourceFilter filter =
                new OAuth2InnerResourceFilter();
        builder.addFilterAfter(postProcess(filter), HeaderWriterFilter.class);
    }
}
