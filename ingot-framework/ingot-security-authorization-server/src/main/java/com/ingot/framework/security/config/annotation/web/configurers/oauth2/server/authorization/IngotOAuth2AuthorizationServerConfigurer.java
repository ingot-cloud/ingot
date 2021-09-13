package com.ingot.framework.security.config.annotation.web.configurers.oauth2.server.authorization;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.ProviderSettings;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * <p>Description  : 代理OAuth2AuthorizationServerConfigurer
 * 1. 增加OAuth2UsernamePasswordAuthenticationConfigurer
 * 2. OAuth2TokenEndpointConfigurer增加OAuth2PasswordAuthenticationProvider</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/13.</p>
 * <p>Time         : 11:05 上午.</p>
 */
@Slf4j
public class IngotOAuth2AuthorizationServerConfigurer<B extends HttpSecurityBuilder<B>>
        extends AbstractHttpConfigurer<OAuth2AuthorizationServerConfigurer<B>, B> {
    private final OAuth2AuthorizationServerConfigurer<B> proxy =
            new OAuth2AuthorizationServerConfigurer<>();

    private final IngotOAuth2PasswordAuthenticationConfigurer<B> passwordConfigurer =
            new IngotOAuth2PasswordAuthenticationConfigurer<>();

    /**
     * Sets the repository of registered clients.
     *
     * @param registeredClientRepository the repository of registered clients
     * @return the {@link OAuth2AuthorizationServerConfigurer} for further configuration
     */
    public IngotOAuth2AuthorizationServerConfigurer<B> registeredClientRepository(
            RegisteredClientRepository registeredClientRepository) {
        proxy.registeredClientRepository(registeredClientRepository);
        return this;
    }

    /**
     * Sets the authorization service.
     *
     * @param authorizationService the authorization service
     * @return the {@link OAuth2AuthorizationServerConfigurer} for further configuration
     */
    public IngotOAuth2AuthorizationServerConfigurer<B> authorizationService(OAuth2AuthorizationService authorizationService) {
        proxy.authorizationService(authorizationService);
        return this;
    }

    /**
     * Sets the authorization consent service.
     *
     * @param authorizationConsentService the authorization consent service
     * @return the {@link OAuth2AuthorizationServerConfigurer} for further configuration
     */
    public IngotOAuth2AuthorizationServerConfigurer<B> authorizationConsentService(OAuth2AuthorizationConsentService authorizationConsentService) {
        proxy.authorizationConsentService(authorizationConsentService);
        return this;
    }

    /**
     * Sets the provider settings.
     *
     * @param providerSettings the provider settings
     * @return the {@link OAuth2AuthorizationServerConfigurer} for further configuration
     */
    public IngotOAuth2AuthorizationServerConfigurer<B> providerSettings(ProviderSettings providerSettings) {
        proxy.providerSettings(providerSettings);
        return this;
    }

    /**
     * Configures OAuth 2.0 Client Authentication.
     *
     * @param clientAuthenticationCustomizer the {@link Customizer} providing access to the {@link OAuth2ClientAuthenticationConfigurer}
     * @return the {@link OAuth2AuthorizationServerConfigurer} for further configuration
     */
    public IngotOAuth2AuthorizationServerConfigurer<B> clientAuthentication(Customizer<OAuth2ClientAuthenticationConfigurer> clientAuthenticationCustomizer) {
        proxy.clientAuthentication(clientAuthenticationCustomizer);
        return this;
    }

    /**
     * Configures the OAuth 2.0 Authorization Endpoint.
     *
     * @param authorizationEndpointCustomizer the {@link Customizer} providing access to the {@link OAuth2AuthorizationEndpointConfigurer}
     * @return the {@link OAuth2AuthorizationServerConfigurer} for further configuration
     */
    public IngotOAuth2AuthorizationServerConfigurer<B> authorizationEndpoint(Customizer<OAuth2AuthorizationEndpointConfigurer> authorizationEndpointCustomizer) {
        proxy.authorizationEndpoint(authorizationEndpointCustomizer);
        return this;
    }

    /**
     * Configures the OAuth 2.0 Token Endpoint.
     *
     * @param tokenEndpointCustomizer the {@link Customizer} providing access to the {@link OAuth2TokenEndpointConfigurer}
     * @return the {@link OAuth2AuthorizationServerConfigurer} for further configuration
     */
    public IngotOAuth2AuthorizationServerConfigurer<B> tokenEndpoint(Customizer<OAuth2TokenEndpointConfigurer> tokenEndpointCustomizer) {
        proxy.tokenEndpoint(tokenEndpointCustomizer);
        return this;
    }

    /**
     * Configures OpenID Connect 1.0 support.
     *
     * @param oidcCustomizer the {@link Customizer} providing access to the {@link OidcConfigurer}
     * @return the {@link OAuth2AuthorizationServerConfigurer} for further configuration
     */
    public IngotOAuth2AuthorizationServerConfigurer<B> oidc(Customizer<OidcConfigurer> oidcCustomizer) {
        proxy.oidc(oidcCustomizer);
        return this;
    }

    /**
     * Returns a {@link RequestMatcher} for the authorization server endpoints.
     *
     * @return a {@link RequestMatcher} for the authorization server endpoints
     */
    public RequestMatcher getEndpointsMatcher() {
        return proxy.getEndpointsMatcher();
    }

    @Override
    public void init(B builder) throws Exception {
        proxy.addObjectPostProcessor(this::postProcess);
        passwordConfigurer.addObjectPostProcessor(this::postProcess);

        proxy.init(builder);
        passwordConfigurer.init(builder);
    }

    @Override
    public void configure(B builder) throws Exception {
        proxy.configure(builder);
        passwordConfigurer.configure(builder);
    }
}
