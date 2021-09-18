package com.ingot.framework.security.oauth2.server.authorization.authentication;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.authorization.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenCustomizer;

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
            // 密码登录增强
        }
    }
}
