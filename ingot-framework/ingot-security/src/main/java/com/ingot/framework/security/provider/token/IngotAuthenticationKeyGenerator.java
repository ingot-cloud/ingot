package com.ingot.framework.security.provider.token;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.constants.SecurityConstants;
import com.ingot.framework.core.utils.DigestUtils;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.IngotUser;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.AuthenticationKeyGenerator;

import java.util.Map;

/**
 * <p>Description  : IngotAuthenticationKeyGenerator.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/27.</p>
 * <p>Time         : 11:14 上午.</p>
 */
public class IngotAuthenticationKeyGenerator implements AuthenticationKeyGenerator {

    /**
     * Extract key
     *
     * @param authentication an OAuth2Authentication
     * @return a unique key identifying the authentication
     */
    @Override public String extractKey(OAuth2Authentication authentication) {
        IngotUser user = SecurityAuthContext.getUser(authentication);
        if (user != null) {
            return generateKey(user.getId(), user.getTenantId(), user.getAuthType(), null);
        }

        throw new InvalidTokenException("Invalid token");
    }

    /**
     * Extract key
     *
     * @param jti JWT ID
     * @param authentication an OAuth2Authentication
     * @return a unique key identifying the authentication
     */
    public String extractKey(String jti, OAuth2Authentication authentication) {
        IngotUser user = SecurityAuthContext.getUser(authentication);
        if (user != null) {
            return generateKey(user.getId(), user.getTenantId(), user.getAuthType(), jti);
        }

        throw new InvalidTokenException("Invalid token");
    }

    @SuppressWarnings("unchecked")
    public String extractKey(OAuth2AccessToken token) {
        String jti = MapUtil.get(token.getAdditionalInformation(),
                SecurityConstants.TokenEnhancer.KEY_JTI, String.class);

        Map<String, ?> userMap = MapUtil.get(token.getAdditionalInformation(),
                SecurityConstants.TokenEnhancer.KEY_USER_OBJECT, Map.class);
        Long id = MapUtil.get(userMap, SecurityConstants.TokenEnhancer.KEY_FIELD_USER_ID, Long.class);
        Integer tenantId = MapUtil.get(userMap, SecurityConstants.TokenEnhancer.KEY_FIELD_TENANT_ID, Integer.class);
        String authType = MapUtil.get(userMap, SecurityConstants.TokenEnhancer.KEY_FIELD_AUTH_TYPE, String.class);

        return generateKey(id, tenantId, authType, jti);
    }

    /**
     * 生成 key
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @param authType 授权类型
     * @param jti      JWT ID
     * @return 生成key格式: tenantID:authType:sha256(userID), eg. std:aa221
     */
    protected String generateKey(long userId,
                                 int tenantId,
                                 String authType,
                                 String jti) {
        String raw = tenantId + "-" + userId;
        if (StrUtil.equals(authType, SecurityConstants.TokenAuthType.STANDARD)) {
            return SecurityConstants.TokenAuthType.STANDARD_SHORT
                    + StrUtil.COLON + DigestUtils.sha256(jti + raw);
        } else if (StrUtil.equals(authType, SecurityConstants.TokenAuthType.UNIQUE)) {
            return SecurityConstants.TokenAuthType.UNIQUE_SHORT
                    + StrUtil.COLON + DigestUtils.sha256(raw);
        }

        throw new InvalidTokenException("Invalid token");
    }
}
