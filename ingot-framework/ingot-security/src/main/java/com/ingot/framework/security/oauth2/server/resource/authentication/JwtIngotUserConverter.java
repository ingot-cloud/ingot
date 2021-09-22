package com.ingot.framework.security.oauth2.server.resource.authentication;

import cn.hutool.core.map.MapUtil;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.oauth2.core.ExtensionClaimNames;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;

import java.util.Collections;
import java.util.Map;

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
        Map<String, Object> claims = source.getClaims();

        String username = MapUtil.get(claims, JwtClaimNames.SUB, String.class);
        Long id = MapUtil.get(claims, ExtensionClaimNames.ID, Long.class);
        Long deptId = MapUtil.get(claims, ExtensionClaimNames.DEPT, Long.class);
        Integer tenantId = MapUtil.get(claims, ExtensionClaimNames.TENANT, Integer.class);
        String authMethod = MapUtil.get(claims, ExtensionClaimNames.AUTH_METHOD, String.class);
        return new IngotUser(id, deptId, tenantId, authMethod, username, N_A, true,
                true, true, true,
                Collections.emptyList());
    }
}
