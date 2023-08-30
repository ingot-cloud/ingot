package com.ingot.framework.security.oauth2.server.authorization.authentication;

import com.ingot.framework.core.model.common.AllowTenantDTO;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Description  : OAuth2PreAuthorizationToken.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 2:49 PM.</p>
 */
public class OAuth2PreAuthorizationAuthenticationToken extends AbstractAuthenticationToken {
    private final Object principal;
    @Getter
    private final Map<String, Object> additionalParameters;
    @Getter
    private final RegisteredClient registeredClient;
    @Getter
    private final String preAuthorization;
    @Getter
    private final List<AllowTenantDTO> allowList;

    public static OAuth2PreAuthorizationAuthenticationToken unauthenticated() {
        return new OAuth2PreAuthorizationAuthenticationToken(
                "", "", null, null);
    }

    public static OAuth2PreAuthorizationAuthenticationToken unauthenticated(Object userPrincipal,
                                                                            String preAuthorization,
                                                                            RegisteredClient registeredClient,
                                                                            Map<String, Object> additionalParameters) {
        return new OAuth2PreAuthorizationAuthenticationToken(
                userPrincipal, preAuthorization, registeredClient, additionalParameters);
    }

    public static OAuth2PreAuthorizationAuthenticationToken authenticated(String authorizationCode,
                                                                          List<AllowTenantDTO> allowList) {
        return new OAuth2PreAuthorizationAuthenticationToken(authorizationCode, allowList);
    }

    public OAuth2PreAuthorizationAuthenticationToken(Object principal,
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
    }

    /**
     * Creates a token with the supplied array of authorities.
     *
     * @param authorities the collection of <tt>GrantedAuthority</tt>s for the principal
     *                    represented by this authentication object.
     */
    public OAuth2PreAuthorizationAuthenticationToken(String authorizationCode,
                                                     List<AllowTenantDTO> allowList) {
        super(null);
        this.principal = authorizationCode;
        this.additionalParameters = Collections.emptyMap();
        this.registeredClient = null;
        this.preAuthorization = null;
        this.allowList = allowList;
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
