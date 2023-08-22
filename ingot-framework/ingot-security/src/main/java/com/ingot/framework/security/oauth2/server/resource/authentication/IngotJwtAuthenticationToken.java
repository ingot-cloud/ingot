package com.ingot.framework.security.oauth2.server.resource.authentication;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;

import java.io.Serial;
import java.util.Collection;
import java.util.Map;

/**
 * <p>Description  : IngotJwtAuthenticationToken.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/17.</p>
 * <p>Time         : 5:33 下午.</p>
 */
public class IngotJwtAuthenticationToken extends AbstractOAuth2TokenAuthenticationToken<Jwt> {

    @Serial
    private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

    private final String name;

    /**
     * Constructs a {@code JwtAuthenticationToken} using the provided parameters.
     *
     * @param jwt         the JWT
     * @param authorities the authorities assigned to the JWT
     */
    public IngotJwtAuthenticationToken(Jwt jwt, Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(jwt, principal, jwt, authorities);
        this.setAuthenticated(true);
        this.name = jwt.getSubject();
    }

    /**
     * Constructs a {@code JwtAuthenticationToken} using the provided parameters.
     *
     * @param jwt         the JWT
     * @param authorities the authorities assigned to the JWT
     * @param name        the principal name
     */
    public IngotJwtAuthenticationToken(Jwt jwt, Object principal, Collection<? extends GrantedAuthority> authorities, String name) {
        super(jwt, principal, jwt, authorities);
        this.setAuthenticated(true);
        this.name = name;
    }

    @Override
    public Map<String, Object> getTokenAttributes() {
        return this.getToken().getClaims();
    }

    /**
     * The principal name which is, by default, the {@link Jwt}'s subject
     */
    @Override
    public String getName() {
        return this.name;
    }
}
