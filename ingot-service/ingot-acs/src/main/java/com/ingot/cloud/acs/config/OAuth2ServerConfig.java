package com.ingot.cloud.acs.config;

import com.ingot.cloud.acs.service.IngotClientDetailService;
import com.ingot.cloud.acs.service.IngotUserDetailService;
import com.ingot.framework.security.provider.error.IngotWebResponseExceptionTranslator;
import com.ingot.framework.security.provider.token.store.IngotJwtTokenStore;
import com.ingot.framework.tenant.filter.TenantFilter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Description  : OAuth2ServerConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/3.</p>
 * <p>Time         : 2:04 下午.</p>
 */
@Slf4j
@Configuration
@AllArgsConstructor
@EnableAuthorizationServer
public class OAuth2ServerConfig extends AuthorizationServerConfigurerAdapter {
    private final AuthenticationManager authenticationManager;
    private final IngotJwtTokenStore tokenStore;
    private final IngotUserDetailService ingotUserDetailService;
    private final IngotClientDetailService ingotClientDetailService;
    private final JwtAccessTokenConverter jwtAccessTokenConverter;
    private final TokenEnhancer tokenEnhancer;
    private final PasswordEncoder clientDetailPasswordEncoder;
    private final TenantFilter filter = new TenantFilter();

    @Override public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        security.tokenKeyAccess("isAuthenticated()")
                .checkTokenAccess("isAuthenticated()")
                .allowFormAuthenticationForClients()
                .passwordEncoder(clientDetailPasswordEncoder)
                .addTokenEndpointAuthenticationFilter(filter);
    }

    @Override public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.withClientDetails(ingotClientDetailService);
    }

    @Override public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        TokenEnhancerChain enhancerChain = new TokenEnhancerChain();
        List<TokenEnhancer> enhancers = new ArrayList<>();
        // tokenEnhancer 必须在前面 jwtAccessTokenConverter，否则增加的字段不生效
        enhancers.add(tokenEnhancer);
        enhancers.add(jwtAccessTokenConverter);
        enhancerChain.setTokenEnhancers(enhancers);

        endpoints.allowedTokenEndpointRequestMethods(HttpMethod.GET, HttpMethod.POST)
                .tokenStore(tokenStore)
                .tokenEnhancer(enhancerChain)
                .accessTokenConverter(jwtAccessTokenConverter)
                .authenticationManager(authenticationManager)
                .userDetailsService(ingotUserDetailService)
                .reuseRefreshTokens(false)
                .exceptionTranslator(new IngotWebResponseExceptionTranslator())
                .pathMapping("/oauth/confirm_access", "/token/confirm_access");
    }
}
