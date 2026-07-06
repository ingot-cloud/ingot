package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configurers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2ClientAuthenticationConfigurer;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * <p>Description  : 增强{@link OAuth2AuthorizationServerConfigurer}.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/10.</p>
 * <p>Time         : 10:05 上午.</p>
 */
@Slf4j
public class OAuth2AuthorizationServerEnhanceConfigurer
        extends AbstractHttpConfigurer<OAuth2AuthorizationServerEnhanceConfigurer, HttpSecurity> {
    private final Map<Class<? extends AbstractOAuth2Configurer>, AbstractOAuth2Configurer> configurers = createConfigurers();
    private RequestMatcher endpointsMatcher;

    public RequestMatcher getEndpointsMatcher() {
        // Return a deferred RequestMatcher
        // since endpointsMatcher is constructed in init(HttpSecurity).
        return (request) -> this.endpointsMatcher.matches(request);
    }

    @Override
    public void init(HttpSecurity httpSecurity) throws Exception {
        List<RequestMatcher> requestMatchers = new ArrayList<>();
        this.configurers.values().forEach(configurer -> {
            configurer.init(httpSecurity);
            requestMatchers.add(configurer.getRequestMatcher());
        });
        this.endpointsMatcher = new OrRequestMatcher(requestMatchers);
    }

    @Override
    public void configure(HttpSecurity httpSecurity) throws Exception {
        this.configurers.values().forEach(configurer -> configurer.configure(httpSecurity));
    }

    @SuppressWarnings("unchecked")
    <T> T getConfigurer(Class<T> type) {
        return (T) this.configurers.get(type);
    }

    /**
     * 配置自定义PreAuthorization流程
     *
     * @param customizer the {@link Customizer} providing access to
     *                   the {@link OAuth2ClientAuthenticationConfigurer}
     * @return the {@link OAuth2AuthorizationServerConfigurer} for further configuration
     */
    public OAuth2AuthorizationServerEnhanceConfigurer preAuthorizationEndpoint(
            Customizer<OAuth2PreAuthorizationRequestEndpointConfigurer> customizer) {
        customizer.customize(getConfigurer(OAuth2PreAuthorizationRequestEndpointConfigurer.class));
        return this;
    }

    private Map<Class<? extends AbstractOAuth2Configurer>, AbstractOAuth2Configurer> createConfigurers() {
        Map<Class<? extends AbstractOAuth2Configurer>, AbstractOAuth2Configurer> configurers = new LinkedHashMap<>();
        configurers.put(OAuth2TokenEndpointEnhanceConfigurer.class,
                new OAuth2TokenEndpointEnhanceConfigurer(this::postProcess));
        configurers.put(OAuth2PreAuthorizationRequestEndpointConfigurer.class,
                new OAuth2PreAuthorizationRequestEndpointConfigurer(this::postProcess));
        return configurers;
    }

}
