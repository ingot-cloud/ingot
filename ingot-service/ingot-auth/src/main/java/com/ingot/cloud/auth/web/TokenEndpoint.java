package com.ingot.cloud.auth.web;

import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.auth.model.dto.UserRevokeDTO;
import com.ingot.cloud.auth.model.dto.UserTokenQueryDTO;
import com.ingot.cloud.auth.model.vo.UserTokenVO;
import com.ingot.cloud.auth.service.biz.BizUserTokenService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.commons.utils.CookieUtil;
import com.ingot.framework.security.access.RequiredAdmin;
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
import org.springframework.web.bind.annotation.*;

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
    private final BizUserTokenService bizUserTokenService;

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

    @RequiredAdmin
    @GetMapping("/tokens")
    public R<List<UserTokenVO>> userTokens(UserTokenQueryDTO params) {
        return ok(bizUserTokenService.userTokenPage(params));
    }

    @RequiredAdmin
    @DeleteMapping("/jti/{jit}")
    public R<?> revoke(@PathVariable String jti) {
        onlineTokenService.removeByJti(jti);
        return ok();
    }

    @RequiredAdmin
    @DeleteMapping("/user")
    public R<?> revokeUser(@RequestBody UserRevokeDTO params) {
        onlineTokenService.removeByUser(params.getUserId(), params.getTenantId(), StrUtil.toString(params.getClientId()));
        return ok();
    }
}
