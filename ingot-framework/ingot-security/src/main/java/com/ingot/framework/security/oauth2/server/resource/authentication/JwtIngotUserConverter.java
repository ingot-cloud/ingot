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
    private static final String N_A = "N/A";

    @Override
    public IngotUser convert(@NonNull Jwt source) {
        String username = JwtClaimNamesExtension.getUsername(source);
        Long id = JwtClaimNamesExtension.getId(source);
        Long deptId = JwtClaimNamesExtension.getDept(source);
        Integer tenantId = JwtClaimNamesExtension.getTenantId(source);
        String authMethod = JwtClaimNamesExtension.getAuthMethod(source);
        String clientId = JwtClaimNamesExtension.getAud(source);
        return new IngotUser(id, deptId, tenantId, authMethod, username, clientId);
    }
}
