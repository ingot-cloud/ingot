package com.ingot.framework.security.oauth2.server.resource.authentication;

import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.oauth2.core.ExtensionClaimNames;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.util.Assert;

import java.util.Collection;

/**
 * <p>Description  : IngotJwtAuthenticationConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/17.</p>
 * <p>Time         : 5:27 下午.</p>
 */
public class IngotJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter;
    private final Converter<Jwt, IngotUser> jwtIngotUserConverter = new JwtIngotUserConverter();

    private String principalClaimName;

    public IngotJwtAuthenticationConverter() {
        jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        ((JwtGrantedAuthoritiesConverter) jwtGrantedAuthoritiesConverter)
                .setAuthoritiesClaimName(ExtensionClaimNames.SCOPE);
    }

    @Override
    public final AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = this.jwtGrantedAuthoritiesConverter.convert(jwt);
        IngotUser principal = this.jwtIngotUserConverter.convert(jwt);
        if (this.principalClaimName == null) {
            return new IngotJwtAuthenticationToken(jwt, principal, authorities);
        }
        String principalClaimValue = jwt.getClaimAsString(this.principalClaimName);
        return new IngotJwtAuthenticationToken(jwt, principal, authorities, principalClaimValue);
    }

    /**
     * Sets the {@link Converter Converter&lt;Jwt, Collection&lt;GrantedAuthority&gt;&gt;}
     * to use. Defaults to {@link JwtGrantedAuthoritiesConverter}.
     *
     * @param jwtGrantedAuthoritiesConverter The converter
     * @see JwtGrantedAuthoritiesConverter
     * @since 5.2
     */
    public void setJwtGrantedAuthoritiesConverter(
            Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter) {
        Assert.notNull(jwtGrantedAuthoritiesConverter, "jwtGrantedAuthoritiesConverter cannot be null");
        this.jwtGrantedAuthoritiesConverter = jwtGrantedAuthoritiesConverter;
    }

    /**
     * Sets the principal claim name. Defaults to {@link org.springframework.security.oauth2.jwt.JwtClaimNames#SUB}.
     *
     * @param principalClaimName The principal claim name
     * @since 5.4
     */
    public void setPrincipalClaimName(String principalClaimName) {
        Assert.hasText(principalClaimName, "principalClaimName cannot be empty");
        this.principalClaimName = principalClaimName;
    }
}
