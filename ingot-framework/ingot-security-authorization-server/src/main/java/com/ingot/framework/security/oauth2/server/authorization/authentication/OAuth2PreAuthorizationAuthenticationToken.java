package com.ingot.framework.security.oauth2.server.authorization.authentication;

import com.ingot.framework.core.model.dto.common.AllowTenantDTO;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.List;

/**
 * <p>Description  : OAuth2PreAuthorizationToken.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 2:49 PM.</p>
 */
public class OAuth2PreAuthorizationAuthenticationToken extends AbstractAuthenticationToken {
    private final Object principal;
    @Getter
    private String preAuthorization;
    @Getter
    private List<AllowTenantDTO> allowList;

    public static OAuth2PreAuthorizationAuthenticationToken unauthenticated(Object userPrincipal,
                                                                            String preAuthorization) {
        return new OAuth2PreAuthorizationAuthenticationToken(userPrincipal, preAuthorization);
    }

    public static OAuth2PreAuthorizationAuthenticationToken authenticated(String authorizationCode,
                                                                          List<AllowTenantDTO> allowList) {
        return new OAuth2PreAuthorizationAuthenticationToken(authorizationCode, allowList);
    }

    public OAuth2PreAuthorizationAuthenticationToken(Object principal,
                                                     String preAuthorization) {
        super(null);
        this.principal = principal;
        this.preAuthorization = preAuthorization;
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
