package com.ingot.cloud.auth.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.auth.model.dto.OAuth2AuthorizationDTO;
import com.ingot.cloud.auth.service.IngotJdbcOAuth2AuthorizationService;
import com.ingot.cloud.auth.utils.OAuth2AuthorizationUtils;
import com.ingot.framework.core.wrapper.BaseController;
import com.ingot.framework.core.wrapper.R;
import com.ingot.framework.security.common.utils.SecurityUtils;
import com.ingot.framework.security.oauth2.server.authorization.AuthorizationCacheService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.core.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final AuthorizationCacheService authorizationCacheService;

    /**
     * 退出登录，清空当前用户授权信息
     *
     * @param authorization Bearer Token
     * @return {@link R}
     */
    @DeleteMapping
    public R<?> revoke(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        String token = SecurityUtils.getBearerTokenValue(authorization);
        OAuth2Authorization record =
                oAuth2AuthorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
        oAuth2AuthorizationService.remove(record);
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
        return ok(((IngotJdbcOAuth2AuthorizationService) oAuth2AuthorizationService).page(page, params));
    }

}
