package com.ingot.framework.security.provider.token;

import com.ingot.framework.core.constants.SecurityConstants;
import com.ingot.framework.security.core.userdetails.IngotUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>Description  : IngotUserAuthenticationConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/1.</p>
 * <p>Time         : 1:22 PM.</p>
 */
public class IngotUserAuthenticationConverter implements UserAuthenticationConverter {
    private static final String N_A = "N/A";

    @Override public Map<String, ?> convertUserAuthentication(Authentication userAuthentication) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put(USERNAME, userAuthentication.getName());
        if (userAuthentication.getAuthorities() != null && !userAuthentication.getAuthorities().isEmpty()) {
            response.put(AUTHORITIES, AuthorityUtils.authorityListToSet(userAuthentication.getAuthorities()));
        }
        return response;
    }

    @Override public Authentication extractAuthentication(Map<String, ?> map) {
        if (map.containsKey(USERNAME)) {
            Collection<? extends GrantedAuthority> authorities = getAuthorities(map);

            String username = (String) map.get(USERNAME);
            Long id = (Long) map.get(SecurityConstants.TOKEN_ENHANCER_KEY_USER_ID);
            Long tenantId = (Long) map.get(SecurityConstants.TOKEN_ENHANCER_KEY_TENANT_ID);
            String authType = (String) map.get(SecurityConstants.TOKEN_ENHANCER_KEY_AUTH_TYPE);
            IngotUser user = new IngotUser(id, tenantId, authType, username, N_A, true,
                    true, true, true, authorities);
            return new UsernamePasswordAuthenticationToken(user, N_A, authorities);
        }
        return null;
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Map<String, ?> map) {
        Object authorities = map.get(AUTHORITIES);
        if (authorities instanceof String) {
            return AuthorityUtils.commaSeparatedStringToAuthorityList((String) authorities);
        }
        if (authorities instanceof Collection) {
            return AuthorityUtils.commaSeparatedStringToAuthorityList(StringUtils
                    .collectionToCommaDelimitedString((Collection<?>) authorities));
        }
        throw new IllegalArgumentException("Authorities must be either a String or a Collection");
    }
}
