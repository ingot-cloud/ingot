package com.ingot.framework.security.oauth2.jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.constants.HeaderConstants;
import com.ingot.framework.core.context.RequestContextHolder;
import com.ingot.framework.security.core.IngotSecurityProperties;
import com.ingot.framework.security.oauth2.server.resource.authentication.IngotJwtAuthenticationConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
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
    private final IngotSecurityProperties properties;

    public JwtTenantValidator(IngotSecurityProperties properties) {
        this.properties = properties;
        jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        ((JwtGrantedAuthoritiesConverter) jwtGrantedAuthoritiesConverter)
                .setAuthoritiesClaimName(JwtClaimNamesExtension.SCOPE);
        ((JwtGrantedAuthoritiesConverter) jwtGrantedAuthoritiesConverter)
                .setAuthorityPrefix(IngotJwtAuthenticationConverter.AUTHORITY_PREFIX);

        Predicate<Long> testClaimValue = (tenantId) -> {
            if (tenantId == null) {
                return false;
            }
            String headerValue = RequestContextHolder.getRequest()
                    .map(request -> request.getHeader(HeaderConstants.TENANT))
                    .orElse("");
            return StrUtil.isEmpty(headerValue)
                    || StrUtil.equals(headerValue, String.valueOf(tenantId));
        };
        this.validator = new JwtClaimValidator<>(JwtClaimNamesExtension.TENANT, testClaimValue);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Assert.notNull(token, "token cannot be null");

        List<String> ignoreRoleCodes = CollUtil.emptyIfNull(
                        properties.getIgnoreTenantValidateRoleCodeList())
                .stream()
                .map(item -> IngotJwtAuthenticationConverter.AUTHORITY_PREFIX + item)
                .collect(Collectors.toList());
        boolean ignoreValidate = Optional.ofNullable(jwtGrantedAuthoritiesConverter.convert(token))
                .orElse(Collections.emptyList())
                .stream()
                .anyMatch(auth -> CollUtil.contains(ignoreRoleCodes, auth.getAuthority()));

        if (ignoreValidate) {
            return OAuth2TokenValidatorResult.success();
        }

        return this.validator.validate(token);
    }
}
