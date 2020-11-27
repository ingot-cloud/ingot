package com.ingot.framework.security.provider.token;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.constants.SecurityConstants;
import com.ingot.framework.core.constants.TenantConstants;
import com.ingot.framework.core.context.RequestContextHolder;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.exception.BadTenantException;
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

    /**
     * Extract information about the user to be used in an access token (i.e. for resource servers).
     *
     * @param userAuthentication an authentication representing a user
     * @return a map of key values representing the unique information about the user
     */
    @Override public Map<String, ?> convertUserAuthentication(Authentication userAuthentication) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put(USERNAME, userAuthentication.getName());
        if (userAuthentication.getAuthorities() != null && !userAuthentication.getAuthorities().isEmpty()) {
            response.put(AUTHORITIES, AuthorityUtils.authorityListToSet(userAuthentication.getAuthorities()));
        }
        return response;
    }

    /**
     * Inverse of {@link #convertUserAuthentication(Authentication)}. Extracts an Authentication from a map.
     *
     * @param map a map of user information
     * @return an Authentication representing the user or null if there is none
     */
    @SuppressWarnings("unchecked")
    @Override public Authentication extractAuthentication(Map<String, ?> map) {
        if (map.containsKey(USERNAME)) {
            Collection<? extends GrantedAuthority> authorities = getAuthorities(map);

            Map<String, ?> userMap = MapUtil.get(map,
                    SecurityConstants.TokenEnhancer.KEY_USER_OBJECT, Map.class);

            String username = MapUtil.get(userMap, SecurityConstants.TokenEnhancer.KEY_FIELD_USERNAME, String.class);
            Long id = MapUtil.get(userMap, SecurityConstants.TokenEnhancer.KEY_FIELD_USER_ID, Long.class);
            Long deptId = MapUtil.get(userMap, SecurityConstants.TokenEnhancer.KEY_FIELD_DEPT_ID, Long.class);
            Integer tenantId = MapUtil.get(userMap, SecurityConstants.TokenEnhancer.KEY_FIELD_TENANT_ID, Integer.class);
            String authType = MapUtil.get(userMap, SecurityConstants.TokenEnhancer.KEY_FIELD_AUTH_TYPE, String.class);

            validateTenant(tenantId);

            IngotUser user = new IngotUser(id, deptId, tenantId, authType, username, N_A, true,
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

    private void validateTenant(Integer tenantId) {
        String headerValue = RequestContextHolder.getRequest()
                .map(request -> request.getHeader(TenantConstants.TENANT_HEADER_KEY))
                .orElse("");
        if (StrUtil.isEmpty(headerValue) || StrUtil.equals(headerValue, String.valueOf(tenantId))) {
            return;
        }

        throw new BadTenantException();
    }
}
