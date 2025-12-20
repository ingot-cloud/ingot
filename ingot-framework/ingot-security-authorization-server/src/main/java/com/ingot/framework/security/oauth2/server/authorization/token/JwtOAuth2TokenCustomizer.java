package com.ingot.framework.security.oauth2.server.authorization.token;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import cn.hutool.core.util.NumberUtil;
import com.ingot.framework.commons.constants.InOAuth2ParameterNames;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.jwt.JwtClaimNamesExtension;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationCodeRequestAuthenticationToken;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

/**
 * JWT Token定制器 <br/>
 * 优化点： <br/>
 * 1. JWT瘦身：只保留核心字段（userId, tenantId, scope），移除authType、userType、完整权限列表 <br/>
 * 2. Scope简化：只保留客户端授权的scope，不包含所有authorities <br/>
 * 3. Redis：保存用户其他信息以及完整权限<br/>
 * <p>Author: wangchao</p>
 * <p>Date: 2021/9/17</p>
 */
@Slf4j
@RequiredArgsConstructor
public class JwtOAuth2TokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {
    private final OnlineTokenService onlineTokenService;

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
            customizeWithUser(context,
                    user.toBuilder()
                            .tenantId(tenant)
                            .build());
        }
    }

    private void customizeWithUser(JwtEncodingContext context, UserDetails userDetails) {
        if (userDetails instanceof InUser user) {
            AtomicReference<Object> jti = new AtomicReference<>();
            AtomicReference<Object> exp = new AtomicReference<>();
            // 1. JWT瘦身：只保留核心字段
            context.getClaims().claims(claims -> {
                claims.put(JwtClaimNamesExtension.ID, user.getId());
                claims.put(JwtClaimNamesExtension.TENANT, user.getTenantId());
                jti.set(claims.get(JwtClaimNamesExtension.JTI));
                exp.set(claims.get(JwtClaimNamesExtension.EXP));
            });

            // 2. Scope简化：只保留客户端授权的scope
            // 不包含用户的所有权限（authorities），减少JWT体积
            Set<String> scopes = new HashSet<>(context.getAuthorizedScopes());
            context.getClaims().claim(JwtClaimNamesExtension.SCOPE, scopes);

            // 3. 移除字段（存储到Redis）：
            // - authType（AUTH_TYPE）
            // - userType（USER_TYPE）
            // - authorities（完整权限列表）
            // 这些信息通过TokenEnhancedInfoService存储，资源服务器从Redis获取
            onlineTokenService.save(user, jti.get().toString(), (Instant) exp.get());

            log.debug("[JwtOAuth2TokenCustomizer] Generated JWT with userId={}, scopes={}",
                    user.getId(), scopes.size());
        }
    }
}
