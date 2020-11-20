package com.ingot.framework.security.provider.token;

import com.ingot.framework.core.constants.SecurityConstants;
import com.ingot.framework.security.core.userdetails.IngotUser;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>Description  : IngotTokenEnh.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/3.</p>
 * <p>Time         : 2:35 下午.</p>
 */
@Slf4j
public class IngotTokenEnhancer implements TokenEnhancer {

    @Override public OAuth2AccessToken enhance(OAuth2AccessToken accessToken,
                                               OAuth2Authentication authentication) {
        log.info("IngotTokenEnhancer enhance, token={}, addInfo={}",
                accessToken.getValue(), accessToken.getAdditionalInformation());

        // 客户端模式不扩展属性
        if (SecurityConstants.CLIENT_CREDENTIALS
                .equals(authentication.getOAuth2Request().getGrantType())) {
            return accessToken;
        }
        // 扩展token属性
        final Map<String, Object> additionalInformation = Optional.ofNullable(
                accessToken.getAdditionalInformation())
                .orElse(new HashMap<>());
        additionalInformation.put(SecurityConstants.TOKEN_ENHANCER_KEY_TIMESTAMP, DateTime.now().getMillis());

        Authentication userAuthentication = authentication.getUserAuthentication();
        if (userAuthentication != null) {
            if (userAuthentication.getPrincipal() instanceof IngotUser) {
                IngotUser user = (IngotUser) userAuthentication.getPrincipal();
                additionalInformation.put(SecurityConstants.TOKEN_ENHANCER_KEY_USER_NAME, user.getUsername());
                additionalInformation.put(SecurityConstants.TOKEN_ENHANCER_KEY_USER_ID, user.getId());
                additionalInformation.put(SecurityConstants.TOKEN_ENHANCER_KEY_DEPT_ID, user.getDeptId());
                additionalInformation.put(SecurityConstants.TOKEN_ENHANCER_KEY_TENANT_ID, user.getTenantId());
                additionalInformation.put(SecurityConstants.TOKEN_ENHANCER_KEY_AUTH_TYPE, user.getAuthType());
            }
        }

        ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(additionalInformation);
        return accessToken;
    }
}
