package com.ingot.framework.security.oauth2.server.resource.authentication;

import java.util.*;
import java.util.stream.Collectors;

import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.jwt.JwtClaimNamesExtension;
import com.ingot.framework.security.oauth2.server.authorization.OnlineToken;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.util.StringUtils;

/**
 * JWT 转 InUser 转换器 <br>
 * 1. 从JWT中提取核心字段（userId, tenantId, scope） <br>
 * 2. 从OnlineTokenService获取扩展信息（authType, userType, authorities）<br>
 * 3. 合并JWT中的scope和Redis中的完整权限列表 <br>
 *
 * <p>Author: wangchao</p>
 * <p>Date: 2021/9/17</p>
 */
@Slf4j
@RequiredArgsConstructor
public class JwtInUserConverter implements Converter<Jwt, InUser> {
    private static final String DEFAULT_AUTHORITIES_CLAIM_DELIMITER = " ";

    private final OnlineTokenService onlineTokenService;

    @Override
    public InUser convert(@NonNull Jwt source) {
        // 1. 从JWT提取核心字段
        String jti = source.getClaim(JwtClaimNames.JTI);
        String username = JwtClaimNamesExtension.getUsername(source);
        Long id = JwtClaimNamesExtension.getId(source);
        Long tenantId = JwtClaimNamesExtension.getTenantId(source);
        String clientId = JwtClaimNamesExtension.getAud(source);

        if (jti == null) {
            log.error("[JwtInUserConverter] JTI is missing in JWT claims");
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token", "JTI is missing", null));
        }

        // 2. 从OnlineTokenService获取扩展信息
        OnlineToken onlineToken = onlineTokenService
                .getByJti(jti)
                .orElse(null);

        // 3. 如果Redis中没有找到，尝试降级处理（兼容旧Token或Redis故障）
        if (onlineToken == null) {
            log.warn("[JwtInUserConverter] Online token not found in Redis for jti={}, falling back to JWT claims", jti);
            return convertFromJwtOnly(source, id, tenantId, clientId, username);
        }

        // 4. 合并JWT中的scope和Redis中的完整权限
        Collection<GrantedAuthority> authorities = mergeAuthorities(source, onlineToken);

        // 5. 构建InUser（包含完整信息）
        log.debug("[JwtInUserConverter] Converted JWT to InUser: userId={}, authType={}, userType={}",
                id, onlineToken.getAuthType(), onlineToken.getUserType());

        return InUser.stateless(
                id,
                tenantId,
                clientId,
                onlineToken.getAuthType(),
                onlineToken.getUserType(),
                username,
                authorities
        );
    }

    /**
     * 降级处理：仅从JWT中提取字段（兼容旧Token或Redis故障）
     */
    private InUser convertFromJwtOnly(Jwt source, Long id, Long tenantId, String clientId, String username) {
        // 尝试从JWT获取（如果JWT中还有这些字段）
        String authType = JwtClaimNamesExtension.getAuthType(source);
        String userType = JwtClaimNamesExtension.getUserType(source);
        Collection<GrantedAuthority> authorities = getAuthorities(source);

        log.warn("[JwtInUserConverter] Using fallback mode with JWT-only claims");

        return InUser.stateless(id, tenantId, clientId, authType, userType, username, authorities);
    }

    /**
     * 合并JWT中的scope和Redis中的完整权限
     */
    private Collection<GrantedAuthority> mergeAuthorities(Jwt jwt, OnlineToken onlineToken) {

        // 1. 从JWT中获取scope
        Collection<GrantedAuthority> jwtAuthorities = getAuthorities(jwt);
        Set<GrantedAuthority> merged = new HashSet<>(jwtAuthorities);

        // 2. 从Redis中获取完整权限列表
        if (onlineToken.getAuthorities() != null && !onlineToken.getAuthorities().isEmpty()) {
            onlineToken.getAuthorities().forEach(auth ->
                    merged.add(new SimpleGrantedAuthority(InJwtAuthenticationConverter.AUTHORITY_PREFIX + auth))
            );
        }

        log.debug("[JwtInUserConverter] Merged authorities: JWT={}, Redis={}, Total={}",
                jwtAuthorities.size(),
                onlineToken.getAuthorities() != null ? onlineToken.getAuthorities().size() : 0,
                merged.size());

        return merged;
    }

    private Collection<GrantedAuthority> getAuthorities(Jwt jwt) {
        return getInnerAuthorities(jwt).stream()
                .map(authority -> new SimpleGrantedAuthority(
                        InJwtAuthenticationConverter.AUTHORITY_PREFIX + authority))
                .collect(Collectors.toList());
    }

    private Collection<String> getInnerAuthorities(Jwt jwt) {
        String claimName = JwtClaimNamesExtension.SCOPE;
        Object authorities = jwt.getClaim(claimName);
        if (authorities instanceof String) {
            if (StringUtils.hasText((String) authorities)) {
                return Arrays.asList(((String) authorities).split(DEFAULT_AUTHORITIES_CLAIM_DELIMITER));
            }
            return Collections.emptyList();
        }
        if (authorities instanceof Collection) {
            return castAuthoritiesToCollection(authorities);
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private Collection<String> castAuthoritiesToCollection(Object authorities) {
        return (Collection<String>) authorities;
    }
}
