package com.ingot.framework.security.oauth2.jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.utils.RequestParamsUtil;
import com.ingot.framework.core.context.RequestContextHolder;
import com.ingot.framework.security.core.InSecurityProperties;
import com.ingot.framework.security.oauth2.server.authorization.OnlineToken;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import com.ingot.framework.security.oauth2.server.resource.authentication.InJwtAuthenticationConverter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.util.Assert;

/**
 * <p>校验 JWT 中的租户声明与当前请求、会话身份是否一致。</p>
 *
 * <p>平台身份的 {@code org} 为空，不能按租户头比对；租户身份仍要求 claim 与请求头
 * {@code Tenant} 一致。管理域以在线会话中的 {@link AuthorizationContext} 为准，
 * 不从 JWT 读取 domain。会话不可用时退回严格租户校验。</p>
 *
 * @author wangchao
 * @since 1.0.0
 * @see OnlineToken#getAuthorizationContext()
 */
@Slf4j
public class JwtTenantValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INVALID_ORG = new OAuth2Error(
            OAuth2ErrorCodes.INVALID_TOKEN,
            "The org claim is not valid",
            "https://tools.ietf.org/html/rfc6750#section-3.1");

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

        OnlineToken session = null;
        // 补齐会话中的完整权限：忽略租户校验的角色不进 JWT，只存在于会话
        // 会话读取失败不在此处判定 Token 有效性（由 JwtInUserConverter 统一裁决），退化为严格租户校验
        try {
            session = onlineTokenService.getBySid(JwtClaimNamesExtension.getSid(token)).orElse(null);
            if (session != null && session.getAuthorities() != null) {
                session.getAuthorities().forEach(auth ->
                        merged.add(new SimpleGrantedAuthority(InJwtAuthenticationConverter.AUTHORITY_PREFIX + auth)));
            }
        } catch (DataAccessException e) {
            log.error("[JwtTenantValidator] 读取会话失败，按严格租户校验处理", e);
        }

        boolean ignoreValidate = merged
                .stream()
                .anyMatch(auth -> CollUtil.contains(ignoreRoleCodes, auth.getAuthority()));

        if (ignoreValidate) {
            return OAuth2TokenValidatorResult.success();
        }

        AuthorizationContext context = session == null ? null : session.getAuthorizationContext();
        if (context != null && context.domain() == AuthorizationDomain.PLATFORM) {
            if (JwtClaimNamesExtension.getTenantId(token) != null) {
                return OAuth2TokenValidatorResult.failure(INVALID_ORG);
            }
            return OAuth2TokenValidatorResult.success();
        }

        return this.validator.validate(token);
    }
}
