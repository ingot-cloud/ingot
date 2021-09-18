package com.ingot.framework.security.oauth2.server.resource.authentication;

import com.ingot.framework.security.core.userdetails.IngotUser;
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
        // todo
        return null;
    }
}
