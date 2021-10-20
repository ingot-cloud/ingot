package com.ingot.cloud.auth.rest;

import com.ingot.framework.core.wrapper.BaseController;
import com.ingot.framework.core.wrapper.R;
import com.ingot.framework.security.common.utils.SecurityUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : AuthTokenApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/20.</p>
 * <p>Time         : 3:41 下午.</p>
 */
@Slf4j
@RestController
@RequestMapping("/token")
@AllArgsConstructor
public class AuthTokenApi extends BaseController {
    private final OAuth2AuthorizationService oAuth2AuthorizationService;

    @DeleteMapping
    public R<?> revoke(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        String token = SecurityUtils.getBearerTokenValue(authorization);
        OAuth2Authorization record =
                oAuth2AuthorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
        oAuth2AuthorizationService.remove(record);
        return ok();
    }
}
