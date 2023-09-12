package com.ingot.framework.security.oauth2.server.authorization.token;

import cn.hutool.core.util.NumberUtil;
import com.ingot.framework.security.core.authority.IngotAuthorityUtils;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.oauth2.core.endpoint.IngotOAuth2ParameterNames;
import com.ingot.framework.security.oauth2.jwt.JwtClaimNamesExtension;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationCodeRequestAuthenticationToken;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.Set;

/**
 * <p>Description  : JwtOAuth2TokenCustomizer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/17.</p>
 * <p>Time         : 6:01 下午.</p>
 */
@Slf4j
public class JwtOAuth2TokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

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
            IngotUser user = (IngotUser) preAuthToken.getPrincipal();
            Long tenant = NumberUtil.parseLong(
                    String.valueOf(preAuthToken.getAdditionalParameters().get(IngotOAuth2ParameterNames.TENANT)),
                    user.getTenantId());
            customizeWithUser(context,
                    user.toBuilder()
                            .tenantId(tenant)
                            .build());
        }
    }

    private void customizeWithUser(JwtEncodingContext context, UserDetails user) {
        if (user instanceof IngotUser) {
            context.getClaims().claim(JwtClaimNamesExtension.ID,
                    ((IngotUser) user).getId());
            context.getClaims().claim(JwtClaimNamesExtension.TENANT,
                    ((IngotUser) user).getTenantId());
            context.getClaims().claim(JwtClaimNamesExtension.DEPT,
                    ((IngotUser) user).getDeptId());
            context.getClaims().claim(JwtClaimNamesExtension.AUTH_TYPE,
                    ((IngotUser) user).getTokenAuthType());
            context.getClaims().claim(JwtClaimNamesExtension.USER_TYPE,
                    ((IngotUser) user).getUserType());

            Set<String> authorities = IngotAuthorityUtils.authorityListToSetWithoutClient(
                    user.getAuthorities());
            authorities.addAll(context.getAuthorizedScopes());

            context.getClaims().claim(JwtClaimNamesExtension.SCOPE, authorities);
        }
    }
}
