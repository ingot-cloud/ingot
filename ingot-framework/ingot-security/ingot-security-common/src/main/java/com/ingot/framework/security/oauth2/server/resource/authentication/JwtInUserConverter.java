package com.ingot.framework.security.oauth2.server.resource.authentication;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.jwt.JwtClaimNamesExtension;
import com.ingot.framework.security.oauth2.server.authorization.OnlineToken;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import com.ingot.framework.security.oauth2.server.authorization.SessionStoreAvailability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataAccessException;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

/**
 * <p>把校验通过的 JWT 还原成 {@link InUser}：定位性声明取自 JWT，权限与身份属性取自在线会话。</p>
 *
 * <p>Redis 中的会话是在线态的唯一权威来源，因此会话缺失即视为 Token 失效，不存在「仅凭 JWT 声明
 * 降级放行」的路径 —— 否则已撤销的 Token 会在 Redis 抖动期间重新可用。</p>
 *
 * @author wangchao
 * @since 1.0.0
 * @see OnlineTokenService
 */
@Slf4j
@RequiredArgsConstructor
public class JwtInUserConverter implements Converter<Jwt, InUser> {
    private static final String DEFAULT_AUTHORITIES_CLAIM_DELIMITER = " ";

    private final OnlineTokenService onlineTokenService;
    private final SessionStoreAvailability sessionStoreAvailability;

    @Override
    public InUser convert(@NonNull Jwt source) {
        String sid = JwtClaimNamesExtension.getSid(source);
        if (StrUtil.isEmpty(sid)) {
            log.error("[JwtInUserConverter] JWT 缺少 sid 声明，拒绝该 Token");
            throw invalidToken("The sid claim is missing.");
        }

        OnlineToken session = readSession(sid);
        Long id = JwtClaimNamesExtension.getId(source);
        Long tenantId = JwtClaimNamesExtension.getTenantId(source);
        String clientId = JwtClaimNamesExtension.getAud(source);
        String username = JwtClaimNamesExtension.getUsername(source);

        if (session == null) {
            // 宽限期内的降级形态：仅保留 JWT 自带的 scope，authType / userType / 部门均不可知
            return InUser.stateless(id, tenantId, clientId, null, null, username,
                    getAuthorities(source), List.of(), MapUtil.empty());
        }

        log.debug("[JwtInUserConverter] JWT 已还原为 InUser: sid={}, userId={}, authType={}, userType={}",
                sid, id, session.getAuthType(), session.getUserType());

        return InUser.stateless(
                id,
                tenantId,
                clientId,
                session.getAuthType(),
                session.getUserType(),
                username,
                mergeAuthorities(source, session),
                session.getDeptIds(),
                MapUtil.empty()
        );
    }

    /**
     * 读取会话；返回 {@code null} 表示会话存储不可用且仍在宽限期内，调用方按降级形态处理。
     *
     * <p>会话键不存在与存储访问异常是两件事：前者是明确的「已下线」，必须拒绝；后者只能在有界
     * 宽限期内放行，超期同样拒绝。</p>
     */
    private OnlineToken readSession(String sid) {
        try {
            OnlineToken session = onlineTokenService.getBySid(sid).orElse(null);
            sessionStoreAvailability.markAvailable();
            if (session == null) {
                log.warn("[JwtInUserConverter] 会话不存在或已撤销: sid={}", sid);
                throw invalidToken("The session is no longer active.");
            }
            return session;
        } catch (DataAccessException e) {
            log.error("[JwtInUserConverter] 读取会话失败: sid={}", sid, e);
            if (sessionStoreAvailability.markUnavailableAndAllow()) {
                return null;
            }
            throw invalidToken("The session store is unavailable.");
        }
    }

    /**
     * 合并 JWT 中的 scope 与会话中的完整权限列表。
     */
    private Collection<GrantedAuthority> mergeAuthorities(Jwt jwt, OnlineToken session) {
        Collection<GrantedAuthority> jwtAuthorities = getAuthorities(jwt);
        Set<GrantedAuthority> merged = new HashSet<>(jwtAuthorities);

        Set<String> sessionAuthorities = session.getAuthorities();
        if (sessionAuthorities != null) {
            sessionAuthorities.forEach(auth ->
                    merged.add(new SimpleGrantedAuthority(InJwtAuthenticationConverter.AUTHORITY_PREFIX + auth))
            );
        }

        log.debug("[JwtInUserConverter] 权限合并结果: jwt={}, session={}, total={}",
                jwtAuthorities.size(), sessionAuthorities == null ? 0 : sessionAuthorities.size(), merged.size());
        return merged;
    }

    private OAuth2AuthenticationException invalidToken(String description) {
        return new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, description, null));
    }

    private Collection<GrantedAuthority> getAuthorities(Jwt jwt) {
        return getInnerAuthorities(jwt).stream()
                .map(authority -> new SimpleGrantedAuthority(
                        InJwtAuthenticationConverter.AUTHORITY_PREFIX + authority))
                .collect(Collectors.toList());
    }

    private Collection<String> getInnerAuthorities(Jwt jwt) {
        Object authorities = jwt.getClaim(JwtClaimNamesExtension.SCOPE);
        if (authorities instanceof String scope) {
            return StringUtils.hasText(scope)
                    ? Arrays.asList(scope.split(DEFAULT_AUTHORITIES_CLAIM_DELIMITER)) : Collections.emptyList();
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
