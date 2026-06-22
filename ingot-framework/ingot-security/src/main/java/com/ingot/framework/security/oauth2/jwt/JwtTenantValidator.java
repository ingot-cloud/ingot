package com.ingot.framework.security.oauth2.jwt;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.utils.RequestParamsUtil;
import com.ingot.framework.core.context.RequestContextHolder;
import com.ingot.framework.security.core.InSecurityProperties;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import com.ingot.framework.security.oauth2.server.resource.authentication.InJwtAuthenticationConverter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.util.Assert;

/**
 * <p>Description  : JwtTenantValidator.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/8.</p>
 * <p>Time         : 1:59 下午.</p>
 */
@Slf4j
public class JwtTenantValidator implements OAuth2TokenValidator<Jwt> {
    private final Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter;
    private final JwtClaimValidator<Long> validator;
    private final InSecurityProperties properties;
    private final OnlineTokenService onlineTokenService;

    public JwtTenantValidator(InSecurityProperties properties, OnlineTokenService service) {
        this.properties = properties;
        this.onlineTokenService = service;
        jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        ((JwtGrantedAuthoritiesConverter) jwtGrantedAuthoritiesConverter)
                .setAuthoritiesClaimName(JwtClaimNamesExtension.SCOPE);
        ((JwtGrantedAuthoritiesConverter) jwtGrantedAuthoritiesConverter)
                .setAuthorityPrefix(InJwtAuthenticationConverter.AUTHORITY_PREFIX);

        Predicate<Long> testClaimValue = (tenantId) -> {
            log.info("[JwtTenantValidator] token中的tenantId={}", tenantId);
            if (tenantId == null) {
                return false;
            }
            HttpServletRequest request = RequestContextHolder.getRequest().orElse(null);
            if (request == null) {
                return false;
            }

            // 保证token中的tenantId和请求中的tenantId一致
            String tenantValue = RequestParamsUtil.getTenantId(request);
            return StrUtil.equals(tenantValue, String.valueOf(tenantId));
        };
        this.validator = new JwtClaimValidator<>(JwtClaimNamesExtension.TENANT, testClaimValue);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Assert.notNull(token, "token cannot be null");

        List<String> ignoreRoleCodes = CollUtil.emptyIfNull(
                        properties.getIgnoreTenantValidateRoleCodeList())
                .stream()
                .map(item -> InJwtAuthenticationConverter.AUTHORITY_PREFIX + item)
                .collect(Collectors.toList());

        Collection<GrantedAuthority> authorities = Optional.ofNullable(jwtGrantedAuthoritiesConverter.convert(token))
                .orElse(Collections.emptyList());
        Set<GrantedAuthority> merged = new HashSet<>(authorities);

        // 拼凑缓存中的SCOPE
        String jti = token.getClaim(JwtClaimNames.JTI);
        onlineTokenService
                .getByJti(jti)
                .ifPresent(onlineToken -> {
                    if (onlineToken.getAuthorities() != null && !onlineToken.getAuthorities().isEmpty()) {
                        onlineToken.getAuthorities().forEach(auth ->
                                merged.add(new SimpleGrantedAuthority(InJwtAuthenticationConverter.AUTHORITY_PREFIX + auth))
                        );
                    }
                });

        boolean ignoreValidate = merged
                .stream()
                .anyMatch(auth -> CollUtil.contains(ignoreRoleCodes, auth.getAuthority()));

        if (ignoreValidate) {
            return OAuth2TokenValidatorResult.success();
        }

        return this.validator.validate(token);
    }
}
