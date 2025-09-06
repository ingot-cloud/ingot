package com.ingot.cloud.auth.web;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.auth.model.dto.OAuth2AuthorizationDTO;
import com.ingot.cloud.auth.service.InJdbcOAuth2AuthorizationService;
import com.ingot.cloud.auth.utils.OAuth2AuthorizationUtils;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.commons.utils.CookieUtils;
import com.ingot.framework.security.utils.SecurityUtils;
import com.ingot.framework.security.oauth2.server.authorization.AuthorizationCacheService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final AuthorizationCacheService authorizationCacheService;
    private final SecurityContextRevokeRepository securityContextRevokeRepository;

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

        CookieUtils.removeCookie(CookieUtils.SESSION_ID_NAME, null, null, response);
        return ok();
    }

    /**
     * 强制下线相关token
     *
     * @param id token id
     * @return {@link R}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@ingot.requiredAdmin")
    public R<?> revokeTarget(@PathVariable String id) {
        OAuth2Authorization record = oAuth2AuthorizationService.findById(id);
        oAuth2AuthorizationService.remove(record);
        OAuth2AuthorizationUtils.getUser(record).ifPresent(authorizationCacheService::remove);
        return ok();
    }

    /**
     * 获取当前Token分页信息
     *
     * @param page   {@link Page}
     * @param params 条件参数
     * @return {@link R}
     */
    @GetMapping("/page")
    @PreAuthorize("@ingot.requiredAdmin")
    public R<?> page(Page<OAuth2AuthorizationDTO> page, OAuth2AuthorizationDTO params) {
        return ok(((InJdbcOAuth2AuthorizationService) oAuth2AuthorizationService).page(page, params));
    }
}
