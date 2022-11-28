package com.ingot.framework.security.oauth2.jwt;

import java.util.ArrayList;

import cn.hutool.core.map.MapUtil;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;

/**
 * <p>Description  : JwtClaimNamesExtension.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/18.</p>
 * <p>Time         : 2:19 下午.</p>
 */
public interface JwtClaimNamesExtension extends JwtClaimNames {

    String ID = "i";
    String TENANT = "tenant";
    String DEPT = "dept";
    String AUTH_TYPE = "tat";
    String SCOPE = OAuth2ParameterNames.SCOPE;

    static String getUsername(Jwt source) {
        return MapUtil.get(source.getClaims(), SUB, String.class);
    }

    static Integer getId(Jwt source) {
        return MapUtil.get(source.getClaims(), JwtClaimNamesExtension.ID, Integer.class);
    }

    static Integer getDept(Jwt source) {
        return MapUtil.get(source.getClaims(), JwtClaimNamesExtension.DEPT, Integer.class);
    }

    static Integer getTenantId(Jwt source) {
        return MapUtil.get(source.getClaims(), JwtClaimNamesExtension.TENANT, Integer.class);
    }

    static String getAuthType(Jwt source) {
        return MapUtil.get(source.getClaims(), JwtClaimNamesExtension.AUTH_TYPE, String.class);
    }

    @SuppressWarnings("unchecked")
    static String getAud(Jwt source) {
        ArrayList<String> clientIds = MapUtil.get(source.getClaims(), AUD, ArrayList.class);
        return clientIds.get(0);
    }
}
