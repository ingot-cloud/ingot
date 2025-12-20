package com.ingot.framework.security.oauth2.server.resource.authentication;

import java.util.Collection;

import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.jwt.JwtClaimNamesExtension;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.util.Assert;

/**
 * JWT认证转换器（优化版）
 * 集成OnlineTokenService，从Redis获取Token扩展信息
 *
 * <p>Author: wangchao</p>
 * <p>Date: 2021/9/17</p>
 */
public class InJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    public static final String AUTHORITY_PREFIX = "SCOPE_";

    private Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter;
    private final Converter<Jwt, InUser> jwtInUserConverter;

    private String principalClaimName;

    public InJwtAuthenticationConverter(OnlineTokenService onlineTokenService) {
        Assert.notNull(onlineTokenService, "onlineTokenService cannot be null");

        // 初始化 JwtInUserConverter
        this.jwtInUserConverter = new JwtInUserConverter(onlineTokenService);
        
        // 初始化 JwtGrantedAuthoritiesConverter
        jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        ((JwtGrantedAuthoritiesConverter) jwtGrantedAuthoritiesConverter)
                .setAuthoritiesClaimName(JwtClaimNamesExtension.SCOPE);
        ((JwtGrantedAuthoritiesConverter) jwtGrantedAuthoritiesConverter)
                .setAuthorityPrefix(AUTHORITY_PREFIX);
    }

    @Override
    public final AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        // 使用 JwtInUserConverte r转换（包含 Redis 中完整信息）
        InUser principal = this.jwtInUserConverter.convert(jwt);
        
        // 使用用户的完整权限（已从Redis获取并合并）
        Collection<GrantedAuthority> authorities = principal.getAuthorities();
        
        if (this.principalClaimName == null) {
            return new InJwtAuthenticationToken(jwt, principal, authorities);
        }
        String principalClaimValue = jwt.getClaimAsString(this.principalClaimName);
        return new InJwtAuthenticationToken(jwt, principal, authorities, principalClaimValue);
    }

    /**
     * Sets the {@link Converter Converter&lt;Jwt, Collection&lt;GrantedAuthority&gt;&gt;}
     * to use. Defaults to {@link JwtGrantedAuthoritiesConverter}.
     *
     * @param jwtGrantedAuthoritiesConverter The converter
     * @see JwtGrantedAuthoritiesConverter
     * @since 5.2
     */
    public void setJwtGrantedAuthoritiesConverter(
            Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter) {
        Assert.notNull(jwtGrantedAuthoritiesConverter, "jwtGrantedAuthoritiesConverter cannot be null");
        this.jwtGrantedAuthoritiesConverter = jwtGrantedAuthoritiesConverter;
    }

    /**
     * Sets the principal claim name. Defaults to {@link org.springframework.security.oauth2.jwt.JwtClaimNames#SUB}.
     *
     * @param principalClaimName The principal claim name
     * @since 5.4
     */
    public void setPrincipalClaimName(String principalClaimName) {
        Assert.hasText(principalClaimName, "principalClaimName cannot be empty");
        this.principalClaimName = principalClaimName;
    }
}
