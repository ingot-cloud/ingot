package com.ingot.cloud.authold.endpoint;

import com.ingot.framework.core.wrapper.BaseController;
import com.ingot.framework.core.wrapper.IngotResponse;
import com.ingot.framework.security.utils.SecurityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>Description  : TokenEndpoint.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/27.</p>
 * <p>Time         : 4:04 下午.</p>
 */
@RequestMapping(value = "/token")
@RestController
public class IngotTokenEndpoint extends BaseController {
    @Resource
    private TokenStore tokenStore;

    @GetMapping("/revoke")
    public IngotResponse<?> revoke(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader){
        tokenStore.removeAccessToken(tokenStore.readAccessToken(SecurityUtils.getBearerTokenValue(authHeader)));
        return ok();
    }

    // todo 签退需要把对应的 refresh token 也 revoke
}
