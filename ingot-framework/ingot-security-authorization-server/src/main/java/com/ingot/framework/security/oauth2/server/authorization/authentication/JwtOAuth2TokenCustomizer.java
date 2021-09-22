package com.ingot.framework.security.oauth2.server.authorization.authentication;

import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.oauth2.core.ExtensionClaimNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.server.authorization.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenCustomizer;

import java.util.List;
import java.util.stream.Collectors;

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
        if (principal instanceof OAuth2UsernamePasswordAuthenticationToken) {
            UserDetails user =
                    (UserDetails) ((OAuth2UsernamePasswordAuthenticationToken) principal).getPrincipal();
            if (user instanceof IngotUser) {
                context.getClaims().claim(ExtensionClaimNames.ID,
                        ((IngotUser) user).getId());
                context.getClaims().claim(ExtensionClaimNames.TENANT,
                        ((IngotUser) user).getTenantId());
                context.getClaims().claim(ExtensionClaimNames.DEPT,
                        ((IngotUser) user).getDeptId());
                context.getClaims().claim(ExtensionClaimNames.AUTH_METHOD,
                        ((IngotUser) user).getTokenAuthenticationMethod());
                List<String> authorities = user.getAuthorities()
                        .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
                context.getClaims().claim(ExtensionClaimNames.SCOPE, authorities);
            }
        }
    }
}
