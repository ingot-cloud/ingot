package com.ingot.framework.security.oauth2.server.authorization.authentication;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ingot.framework.commons.model.common.TenantMainDTO;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/**
 * <p>Description  : OAuth2PreAuthorizationCodeRequestAuthenticationToken.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 2:49 PM.</p>
 */
public class OAuth2PreAuthorizationCodeRequestAuthenticationToken extends AbstractAuthenticationToken {
    private final Object principal;
    @Getter
    private final Map<String, Object> additionalParameters;
    @Getter
    private final RegisteredClient registeredClient;
    @Getter
    private final String preAuthorization;
    @Getter
    private final List<TenantMainDTO> allowList;
    @Getter
    private final long timeToLive;

    public static OAuth2PreAuthorizationCodeRequestAuthenticationToken unauthenticated() {
        return new OAuth2PreAuthorizationCodeRequestAuthenticationToken(
                "", "", null, null);
    }

    public static OAuth2PreAuthorizationCodeRequestAuthenticationToken unauthenticated(Object userPrincipal,
                                                                                       String preAuthorization,
                                                                                       RegisteredClient registeredClient,
                                                                                       Map<String, Object> additionalParameters) {
        return new OAuth2PreAuthorizationCodeRequestAuthenticationToken(
                userPrincipal, preAuthorization, registeredClient, additionalParameters);
    }

    public static OAuth2PreAuthorizationCodeRequestAuthenticationToken authenticated(Object userPrincipal,
                                                                                     List<TenantMainDTO> allowList,
                                                                                     Map<String, Object> additionalParameters,
                                                                                     long timeToLive) {
        return new OAuth2PreAuthorizationCodeRequestAuthenticationToken(userPrincipal,
                additionalParameters, allowList, timeToLive);
    }

    public OAuth2PreAuthorizationCodeRequestAuthenticationToken(Object principal,
                                                                String preAuthorization,
                                                                RegisteredClient registeredClient,
                                                                Map<String, Object> additionalParameters) {
        super(null);
        this.principal = principal;
        this.additionalParameters = Collections.unmodifiableMap(
                additionalParameters != null ?
                        new HashMap<>(additionalParameters) :
                        Collections.emptyMap());
        this.registeredClient = registeredClient;
        this.preAuthorization = preAuthorization;
        this.allowList = Collections.emptyList();
        this.timeToLive = 0L;
    }

    /**
     * Creates a token with the supplied array of authorities.
     *
     * @param authorities the collection of <tt>GrantedAuthority</tt>s for the principal
     *                    represented by this authentication object.
     */
    public OAuth2PreAuthorizationCodeRequestAuthenticationToken(Object userPrincipal,
                                                                Map<String, Object> additionalParameters,
                                                                List<TenantMainDTO> allowList,
                                                                long timeToLive) {
        super(null);
        this.principal = userPrincipal;
        this.additionalParameters = Collections.unmodifiableMap(
                additionalParameters != null ?
                        new HashMap<>(additionalParameters) :
                        Collections.emptyMap());
        this.registeredClient = null;
        this.preAuthorization = null;
        this.allowList = allowList;
        this.timeToLive = timeToLive;
        super.setAuthenticated(true); // must use super, as we override
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
    }
}
