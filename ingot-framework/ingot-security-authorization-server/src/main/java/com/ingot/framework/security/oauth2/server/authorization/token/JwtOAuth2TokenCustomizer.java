package com.ingot.framework.security.oauth2.server.authorization.token;

import java.util.Set;
import java.util.stream.Collectors;

import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.oauth2.jwt.JwtClaimNamesExtension;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

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
        if (principal instanceof OAuth2UserDetailsAuthenticationToken) {
            UserDetails user =
                    (UserDetails) ((OAuth2UserDetailsAuthenticationToken) principal).getPrincipal();
            customizeWithUser(context, user);
        } else if (principal instanceof UsernamePasswordAuthenticationToken) {
            UserDetails user =
                    (UserDetails) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
            customizeWithUser(context, user);
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

            Set<String> authorities = user.getAuthorities()
                    .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
            authorities.addAll(context.getAuthorizedScopes());

            context.getClaims().claim(JwtClaimNamesExtension.SCOPE, authorities);
        }
    }
}
