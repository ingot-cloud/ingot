package com.ingot.framework.security.oauth2.server.resource.authentication;

import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.oauth2.jwt.JwtClaimNamesExtension;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * <p>Description  : JwtIngotUserConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/17.</p>
 * <p>Time         : 5:51 下午.</p>
 */
public class JwtIngotUserConverter implements Converter<Jwt, IngotUser> {

    @Override
    public IngotUser convert(@NonNull Jwt source) {
        String username = JwtClaimNamesExtension.getUsername(source);
        Long id = JwtClaimNamesExtension.getId(source);
        Long deptId = JwtClaimNamesExtension.getDept(source);
        Long tenantId = JwtClaimNamesExtension.getTenantId(source);
        String authType = JwtClaimNamesExtension.getAuthType(source);
        String userType = JwtClaimNamesExtension.getUserType(source);
        String clientId = JwtClaimNamesExtension.getAud(source);
        return IngotUser.simple(id, deptId, tenantId, clientId, authType, userType, username);
    }
}
