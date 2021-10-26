package com.ingot.framework.security.oauth2.server.authorization;

import java.io.Serializable;

import lombok.Data;

/**
 * <p>Description  : AuthorizationCache.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/26.</p>
 * <p>Time         : 9:41 上午.</p>
 */
@Data
public class AuthorizationCache implements Serializable {
    private String id;
    private String registeredClientId;
    private String principalName;
    private String authorizationGrantType;
    private String tokenValue;

    public static AuthorizationCache create(String id, String clientId, String name, String grantType, String tokenValue) {
        AuthorizationCache instance = new AuthorizationCache();
        instance.setId(id);
        instance.setRegisteredClientId(clientId);
        instance.setPrincipalName(name);
        instance.setAuthorizationGrantType(grantType);
        instance.setTokenValue(tokenValue);
        return instance;
    }
}
