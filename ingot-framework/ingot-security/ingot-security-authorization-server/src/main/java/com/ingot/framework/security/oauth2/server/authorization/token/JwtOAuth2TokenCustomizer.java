package com.ingot.framework.security.oauth2.server.authorization.token;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.constants.InOAuth2ParameterNames;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.jwt.JwtClaimNamesExtension;
import com.ingot.framework.security.oauth2.server.authorization.OnlineSessionRegistration;
import com.ingot.framework.security.oauth2.server.authorization.authentication.AuthenticatedMemberBinder;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationCodeRequestAuthenticationToken;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken;
import com.ingot.framework.security.oauth2.server.authorization.session.SessionRegistrar;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

/**
 * <p>JWT 定制器：写入瘦身后的核心声明，并把会话扩展信息落地到在线会话存储。</p>
 *
 * <p>JWT 只保留 {@code sid}、{@code id}、{@code tenant}、{@code scope} 等定位性声明；
 * authType、userType、完整权限列表均落在 Redis 会话中，由资源服务器按 sid 回读补全。
 * 预授权选定租户后通过 {@link AuthenticatedMemberBinder} 写入成员上下文，再注册在线会话。
 * {@code sid} 是撤销与在线态判定的锚点，缺失即视为不可用 Token。</p>
 *
 * @author wangchao
 * @since 1.0.0
 * @see SessionRegistrar
 * @see AuthenticatedMemberBinder
 */
@Slf4j
@RequiredArgsConstructor
public class JwtOAuth2TokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final SessionRegistrar sessionRegistrar;
    private final AuthenticatedMemberBinder memberBinder;

    @Override
    public void customize(JwtEncodingContext context) {
        Object principal = context.getPrincipal();
        if (principal instanceof OAuth2UserDetailsAuthenticationToken userDetailsAuthenticationToken) {
            UserDetails user = (UserDetails) userDetailsAuthenticationToken.getPrincipal();
            customizeWithUser(context, user);
        } else if (principal instanceof UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
            UserDetails user = (UserDetails) usernamePasswordAuthenticationToken.getPrincipal();
            customizeWithUser(context, user);
        } else if (principal instanceof OAuth2PreAuthorizationCodeRequestAuthenticationToken preAuthToken) {
            InUser user = (InUser) preAuthToken.getPrincipal();
            Long tenant = NumberUtil.parseLong(
                    String.valueOf(preAuthToken.getAdditionalParameters().get(InOAuth2ParameterNames.TENANT)),
                    user.getTenantId());
            customizeWithUser(context, memberBinder.resolveForIssuance(user, tenant));
        }
    }

    private void customizeWithUser(JwtEncodingContext context, UserDetails userDetails) {
        if (!(userDetails instanceof InUser user)) {
            return;
        }

        String sid = resolveSid(context);
        AtomicReference<Object> jti = new AtomicReference<>();
        AtomicReference<Object> accessTokenExpiresAt = new AtomicReference<>();

        // 1. JWT 瘦身：只保留会话定位与租户上下文
        context.getClaims().claims(claims -> {
            claims.put(JwtClaimNamesExtension.SID, sid);
            claims.put(JwtClaimNamesExtension.ID, user.getId());
            claims.put(JwtClaimNamesExtension.TENANT, user.getTenantId());
            jti.set(claims.get(JwtClaimNamesExtension.JTI));
            accessTokenExpiresAt.set(claims.get(JwtClaimNamesExtension.EXP));
        });

        // 2. Scope 简化：只保留客户端已授权的 scope，不展开用户权限
        Set<String> scopes = new HashSet<>(context.getAuthorizedScopes());
        context.getClaims().claim(JwtClaimNamesExtension.SCOPE, scopes);

        // 3. authType、userType、authorities、deptIds 均不进 JWT，落到会话由资源服务器回读
        Instant accessTokenExp = (Instant) accessTokenExpiresAt.get();
        sessionRegistrar.register(user, new OnlineSessionRegistration(
                sid, String.valueOf(jti.get()), accessTokenExp, resolveSessionExpiresAt(context, accessTokenExp)));

        log.debug("[JwtOAuth2TokenCustomizer] 已签发 JWT: sid={}, userId={}, scopes={}",
                sid, user.getId(), scopes.size());
    }

    /**
     * 取本次签发所属 Authorization 的 ID 作为会话 ID。
     *
     * <p>授权码、刷新令牌等标准流程由 Spring Authorization Server 透传 Authorization；
     * 自定义 grant 需在生成 Token 前预置 Authorization ID（见
     * {@code OAuth2CustomAuthenticationProvider}）。此处缺失说明签发链未接会话模型，
     * 必须失败而不能签出无 sid 的 Token —— 那样的 Token 无法被撤销。</p>
     */
    private String resolveSid(JwtEncodingContext context) {
        OAuth2Authorization authorization = context.getAuthorization();
        if (authorization != null && StrUtil.isNotEmpty(authorization.getId())) {
            return authorization.getId();
        }

        log.error("[JwtOAuth2TokenCustomizer] 签发上下文缺少 Authorization，无法生成 sid: grantType={}",
                context.getAuthorizationGrantType().getValue());
        throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
                "The authorization id is required for session binding.", null));
    }

    /**
     * 计算会话过期时间：会话应活到整条凭据链彻底失效为止。
     *
     * <p>只要还能凭 Refresh Token 换发 Access Token，会话就仍然有效，故取 Refresh Token 的过期
     * 时间；无 Refresh Token 时会话随 Access Token 一起结束。口径与
     * {@code RedisOAuth2AuthorizationService} 按最长 Token 过期时间计算的 TTL 一致，
     * 保证 Authorization 与会话同生共死。</p>
     */
    private Instant resolveSessionExpiresAt(JwtEncodingContext context, Instant accessTokenExpiresAt) {
        if (!issuesRefreshToken(context)) {
            return accessTokenExpiresAt;
        }

        Instant refreshTokenExpiresAt = resolveReusedRefreshTokenExpiresAt(context);
        if (refreshTokenExpiresAt == null) {
            // 本次会签发新的 Refresh Token，寿命从当前时刻重新计算
            refreshTokenExpiresAt = Instant.now()
                    .plus(context.getRegisteredClient().getTokenSettings().getRefreshTokenTimeToLive());
        }
        return refreshTokenExpiresAt.isAfter(accessTokenExpiresAt) ? refreshTokenExpiresAt : accessTokenExpiresAt;
    }

    /**
     * 判断本次签发是否伴随 Refresh Token，口径对齐 Spring Authorization Server 各 grant provider：
     * 客户端须支持 refresh_token 授权类型，且不是免认证的公共客户端。
     */
    private boolean issuesRefreshToken(JwtEncodingContext context) {
        RegisteredClient registeredClient = context.getRegisteredClient();
        if (!registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
            return false;
        }
        Authentication authorizationGrant = context.getAuthorizationGrant();
        return !(authorizationGrant != null
                && authorizationGrant.getPrincipal() instanceof OAuth2ClientAuthenticationToken clientPrincipal
                && ClientAuthenticationMethod.NONE.equals(clientPrincipal.getClientAuthenticationMethod()));
    }

    /**
     * 取被复用的 Refresh Token 的过期时间；返回 {@code null} 表示本次会换发新的 Refresh Token。
     *
     * <p>refresh_token 换发场景下若配置了 {@code reuseRefreshTokens}，旧 Refresh Token 的寿命
     * 不会被重置，会话寿命必须跟随它而非重新计时，否则会话会被无限续期。</p>
     */
    private Instant resolveReusedRefreshTokenExpiresAt(JwtEncodingContext context) {
        OAuth2Authorization authorization = context.getAuthorization();
        if (!context.getRegisteredClient().getTokenSettings().isReuseRefreshTokens()
                || authorization == null || authorization.getRefreshToken() == null) {
            return null;
        }
        return authorization.getRefreshToken().getToken().getExpiresAt();
    }
}
