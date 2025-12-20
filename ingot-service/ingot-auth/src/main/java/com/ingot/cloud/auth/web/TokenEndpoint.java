package com.ingot.cloud.auth.web;

import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.commons.utils.CookieUtil;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import com.ingot.framework.security.oauth2.server.resource.authentication.JwtContextHolder;
import com.ingot.framework.security.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.web.context.SecurityContextRevokeRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : TokenEndpoint.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/19.</p>
 * <p>Time         : 1:54 PM.</p>
 */
@Slf4j
@RestController
@RequestMapping("/token")
@RequiredArgsConstructor
public class TokenEndpoint implements RShortcuts {
    private final OAuth2AuthorizationService oAuth2AuthorizationService;
    private final SecurityContextRevokeRepository securityContextRevokeRepository;
    private final OnlineTokenService onlineTokenService;

    /**
     * 退出登录，清空当前用户授权信息
     *
     * @param authorization Bearer Token
     * @return {@link R}
     */
    @DeleteMapping
    public R<?> revoke(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                       HttpServletRequest request,
                       HttpServletResponse response) {
        String token = SecurityUtils.getBearerTokenValue(authorization);
        OAuth2Authorization record =
                oAuth2AuthorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
        if (record != null) {
            oAuth2AuthorizationService.remove(record);
        }

        securityContextRevokeRepository.revokeContext(request);

        CookieUtil.removeCookie(CookieUtil.SESSION_ID_NAME, null, null, response);

        onlineTokenService.removeByJti(JwtContextHolder.get());
        return ok();
    }

}
