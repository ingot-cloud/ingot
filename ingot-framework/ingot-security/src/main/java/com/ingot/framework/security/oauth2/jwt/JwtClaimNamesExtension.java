package com.ingot.framework.security.oauth2.jwt;

import cn.hutool.core.map.MapUtil;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;

import java.util.ArrayList;

/**
 * <p>Description  : {@link JwtClaimNames}的扩展，并且包含之前的常量.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/18.</p>
 * <p>Time         : 2:19 下午.</p>
 */
public interface JwtClaimNamesExtension {

    /**
     * {@code iss} - the Issuer claim identifies the principal that issued the JWT
     */
    String ISS = JwtClaimNames.ISS;

    /**
     * {@code sub} - the Subject claim identifies the principal that is the subject of the
     * JWT
     */
    String SUB = JwtClaimNames.SUB;

    /**
     * {@code aud} - the Audience claim identifies the recipient(s) that the JWT is
     * intended for
     */
    String AUD = JwtClaimNames.AUD;

    /**
     * {@code exp} - the Expiration time claim identifies the expiration time on or after
     * which the JWT MUST NOT be accepted for processing
     */
    String EXP = JwtClaimNames.EXP;

    /**
     * {@code nbf} - the Not Before claim identifies the time before which the JWT MUST
     * NOT be accepted for processing
     */
    String NBF = JwtClaimNames.NBF;

    /**
     * {@code iat} - The Issued at claim identifies the time at which the JWT was issued
     */
    String IAT = JwtClaimNames.IAT;

    /**
     * {@code jti} - The JWT ID claim provides a unique identifier for the JWT
     */
    String JTI = JwtClaimNames.JTI;

    /**
     * ID
     */
    String ID = "i";
    String TENANT = "org";
    String AUTH_TYPE = "tat";
    String USER_TYPE = "ut";
    String SCOPE = OAuth2ParameterNames.SCOPE;

    static String getUsername(Jwt source) {
        return MapUtil.get(source.getClaims(), SUB, String.class);
    }

    static Long getId(Jwt source) {
        return MapUtil.get(source.getClaims(), JwtClaimNamesExtension.ID, Long.class);
    }

    static Long getTenantId(Jwt source) {
        return MapUtil.get(source.getClaims(), JwtClaimNamesExtension.TENANT, Long.class);
    }

    static String getAuthType(Jwt source) {
        return MapUtil.get(source.getClaims(), JwtClaimNamesExtension.AUTH_TYPE, String.class);
    }

    static String getUserType(Jwt source) {
        return MapUtil.get(source.getClaims(), JwtClaimNamesExtension.USER_TYPE, String.class);
    }

    @SuppressWarnings("unchecked")
    static String getAud(Jwt source) {
        ArrayList<String> clientIds = MapUtil.get(source.getClaims(), AUD, ArrayList.class);
        return clientIds.get(0);
    }
}
