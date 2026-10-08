package com.ingot.framework.security.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.constants.PermissionConstants;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.oauth2.server.resource.access.expression.TrustedAuthoritySource;
import com.ingot.framework.security.oauth2.server.resource.authentication.InJwtAuthenticationConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.context.SessionContextHolder;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.security.oauth2.jwt.JwtClaimNamesExtension;
import com.ingot.framework.security.oauth2.server.resource.authentication.InJwtAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * <p>恢复会话上下文并执行强制改密门禁，受限账号不能进入业务或仅登录接口。</p>
 *
 * <p>位于
 * {@link org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter}
 * 之后，此时 JWT 已验签且会话已确认存在（见
 * {@link com.ingot.framework.security.oauth2.server.resource.authentication.JwtInUserConverter}）。
 * 被踢下线的 Token 在 Converter 阶段因 sid 主数据缺失已被拒绝，本过滤器不再重复校验。
 * 必须改密的 IAM 账号只允许精确标记的密码端点及已有公开入口；
 * 会话存储降级无法证明账号状态时，受保护请求失败关闭。</p>
 *
 * @author wangchao
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class InTokenAuthFilter extends OncePerRequestFilter {
    private static final ObjectMapper ERROR_MAPPER = new ObjectMapper();
    private final RequestMatcher ignoreRequestMatcher;
    private final RequestMatcher passwordChangeRequestMatcher;
    private final ObjectProvider<TrustedAuthoritySource> trustedAuthorities;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (ignoreRequestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            InUser user = SecurityAuthContext.getUser();
            if (user == null) {
                OAuth2ErrorUtils.throwInvalidToken();
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication instanceof InJwtAuthenticationToken jwtAuthentication)) {
                OAuth2ErrorUtils.throwInvalidToken();
                return;
            }

            String sid = JwtClaimNamesExtension.getSid(jwtAuthentication.getToken());
            if (StrUtil.isEmpty(sid)) {
                OAuth2ErrorUtils.throwInvalidToken();
            }
            SessionContextHolder.set(sid);
            if (!passwordChangeRequestMatcher.matches(request)) {
                try {
                    boolean restricted = user.getAuthorities().stream().anyMatch(authority ->
                            (InJwtAuthenticationConverter.AUTHORITY_PREFIX + PermissionConstants.INIT_PASSWORD)
                                    .equals(authority.getAuthority())
                                    || PermissionConstants.INIT_PASSWORD.equals(authority.getAuthority()));
                    if (user.getAuthorizationContext() == null && user.getUserType() == null) {
                        // 会话存储降级无法证明账号安全状态，禁止只凭 JWT 访问仅登录接口。
                        throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
                    }
                    if (user.getAuthorizationContext() != null) {
                        var source = trustedAuthorities.getIfAvailable();
                        if (source == null) throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
                        restricted |= source.requiresPasswordChange();
                    }
                    if (restricted) {
                        reject(response, IamReasonCode.PASSWORD_CHANGE_REQUIRED);
                        return;
                    }
                } catch (RuntimeException failure) {
                    IamReasonCode reason = failure instanceof BizException business
                            ? IamReasonCode.find(business.getCode()) : null;
                    if (reason != IamReasonCode.IDENTITY_INVALID) {
                        logger.warn("无法确认当前账号改密状态，拒绝受保护请求", failure);
                    }
                    reject(response, reason == IamReasonCode.IDENTITY_INVALID ? reason : IamReasonCode.AUTHORIZATION_UNAVAILABLE);
                    return;
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            SessionContextHolder.clear();
        }
    }

    private void reject(HttpServletResponse response, IamReasonCode reason) throws IOException {
        response.setStatus(reason.getHttpStatus());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ERROR_MAPPER.writeValue(response.getWriter(), new R<Void>(reason));
    }
}
