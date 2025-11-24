package com.ingot.framework.security.oauth2.server.resource.authentication;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.jwt.JwtClaimNamesExtension;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

/**
 * <p>Description  : {@link InUser} Converter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/17.</p>
 * <p>Time         : 5:51 下午.</p>
 */
public class JwtInUserConverter implements Converter<Jwt, InUser> {
    private static final String DEFAULT_AUTHORITIES_CLAIM_DELIMITER = " ";

    @Override
    public InUser convert(@NonNull Jwt source) {
        String username = JwtClaimNamesExtension.getUsername(source);
        Long id = JwtClaimNamesExtension.getId(source);
        Long tenantId = JwtClaimNamesExtension.getTenantId(source);
        String authType = JwtClaimNamesExtension.getAuthType(source);
        String userType = JwtClaimNamesExtension.getUserType(source);
        String clientId = JwtClaimNamesExtension.getAud(source);
        Collection<GrantedAuthority> authorities = getAuthorities(source);
        return InUser.stateless(id, tenantId, clientId, authType, userType, username, authorities);
    }

    private Collection<GrantedAuthority> getAuthorities(Jwt jwt) {
        return getInnerAuthorities(jwt).stream()
                .map(authority -> new SimpleGrantedAuthority(
                        InJwtAuthenticationConverter.AUTHORITY_PREFIX + authority))
                .collect(Collectors.toList());
    }

    private Collection<String> getInnerAuthorities(Jwt jwt) {
        String claimName = JwtClaimNamesExtension.SCOPE;
        Object authorities = jwt.getClaim(claimName);
        if (authorities instanceof String) {
            if (StringUtils.hasText((String) authorities)) {
                return Arrays.asList(((String) authorities).split(DEFAULT_AUTHORITIES_CLAIM_DELIMITER));
            }
            return Collections.emptyList();
        }
        if (authorities instanceof Collection) {
            return castAuthoritiesToCollection(authorities);
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private Collection<String> castAuthoritiesToCollection(Object authorities) {
        return (Collection<String>) authorities;
    }
}
