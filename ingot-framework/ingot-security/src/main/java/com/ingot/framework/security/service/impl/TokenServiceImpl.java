package com.ingot.framework.security.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.constants.SecurityConstants;
import com.ingot.framework.security.exception.TokenEmptyException;
import com.ingot.framework.security.exception.TokenInvalidException;
import com.ingot.framework.security.exception.TokenSignBackException;
import com.ingot.framework.security.model.UserStoreToken;
import com.ingot.framework.security.provider.token.store.IngotJwtTokenStore;
import com.ingot.framework.security.service.AuthenticationService;
import com.ingot.framework.security.service.TokenService;
import com.ingot.framework.security.utils.SecurityUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * <p>Description  : TokenServiceUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/1.</p>
 * <p>Time         : 3:54 PM.</p>
 */
@Slf4j
@Service
@ConditionalOnBean(TokenStore.class)
@AllArgsConstructor
public class TokenServiceImpl implements TokenService {
    private final IngotJwtTokenStore tokenStore;
    private final AuthenticationService authenticationService;

    @Override
    public OAuth2AccessToken getToken(HttpServletRequest request) {
        String token = SecurityUtils.getBearerTokenValue(SecurityUtils.getBearerToken(request).orElse(""));
        if (StrUtil.isEmpty(token)) {
            log.error(">>> Token不能为空");
            throw new TokenEmptyException();
        }

        return tokenStore.readAccessToken(token);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void checkAuthentication(OAuth2AccessToken token) {
        UserStoreToken userStoreToken = tokenStore.getUserStoreToken(token);
        if (userStoreToken == null) {
            throw new TokenInvalidException();
        }


        Map<String, ?> userMap = MapUtil.get(token.getAdditionalInformation(),
                SecurityConstants.TokenEnhancer.KEY_USER_OBJECT, Map.class);

        String username = MapUtil.get(userMap, SecurityConstants.TokenEnhancer.KEY_FIELD_USERNAME, String.class);
        String authType = MapUtil.get(userMap, SecurityConstants.TokenEnhancer.KEY_FIELD_AUTH_TYPE, String.class);
        // 如果当前鉴权类型为唯一，那么需要判断使用token是否和当前登录用户token相同，不同则签退
        if (StrUtil.endWithIgnoreCase(authType, SecurityConstants.TokenAuthType.UNIQUE)) {
            String jti = MapUtil.get(token.getAdditionalInformation(),
                    SecurityConstants.TokenEnhancer.KEY_JTI, String.class);
            if (!StrUtil.equals(jti, userStoreToken.getJti())) {
                log.error(">>> 用户 {} 已被签退", username);
                throw new TokenSignBackException();
            }
        }

        // TODO 权限校验
//        // 校验用户权限
//        log.info(">>> UserAuthenticationFilter 开始验证用户权限, user={}", user.getUsername());
//        boolean authResult = authenticationService.authenticate(SecurityUtils.getAuthentication(), request);
//        log.info(">>> UserAuthenticationFilter 验证用户权限结束, user={}, 结果={}",
//                user.getUsername(), authResult);
//        if (!authResult) {
//            throw new UserForbiddenException();
//        }
    }
}
