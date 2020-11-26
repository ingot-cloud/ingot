package com.ingot.framework.security.service;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.constants.SecurityConstants;
import com.ingot.framework.security.model.dto.UserTokenDto;
import com.ingot.framework.security.utils.ObjectUtils;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <p>Description  : TokenServiceUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/1.</p>
 * <p>Time         : 3:54 PM.</p>
 */
@Service
@ConditionalOnBean(TokenStore.class)
@AllArgsConstructor
public class TokenService {
    private final TokenStore tokenStore;

    /**
     * 设置当前 SecurityContext Authentication
     * @param authorization 用户 Token
     */
    public void setSecurityContextAuthentication(String authorization){
        OAuth2Authentication authentication = tokenStore.readAuthentication(authorization);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 解析 Token
     * @param authorization 用户 Token
     * @return {@link OAuth2AccessToken}
     */
    public OAuth2AccessToken readAccessToken(String authorization){
        return tokenStore.readAccessToken(authorization);
    }

    /**
     * 获取 token 中的 additional 信息
     * @param authorization 用户 Token
     * @return {@link Map}
     */
    public Map<String, Object> getAdditionalInformation(String authorization){
        OAuth2AccessToken auth2AccessToken = tokenStore.readAccessToken(authorization);
        return auth2AccessToken.getAdditionalInformation();
    }

    /**
     * 获取 token 授权信息
     * @param authorization 用户 token
     */
    @SuppressWarnings("unchecked")
    public List<String> getAuthorizeList(@NonNull String authorization){
        OAuth2AccessToken auth2AccessToken = tokenStore.readAccessToken(authorization);
        Object result = auth2AccessToken.getAdditionalInformation().get("authorities");
        return result != null ? (List<String>) result : Collections.emptyList();
    }

    /**
     * 获取用户token DTO
     * @param authorization Authorization
     * @return {@link UserTokenDto}
     */
    public UserTokenDto getUserTokenDto(String authorization){
        UserTokenDto token = new UserTokenDto();
        if (StrUtil.isEmpty(authorization)){
            return token;
        }
        Map<String, Object> info = getAdditionalInformation(authorization);
        if (info != null){
            token.setUserId(ObjectUtils.toString(info.get(SecurityConstants.TokenEnhancer.KEY_FIELD_USER_ID)));
            token.setUsername(ObjectUtils.toString(info.get(SecurityConstants.TokenEnhancer.KEY_FIELD_USERNAME)));
            token.setAuthType(ObjectUtils.toString(info.get(SecurityConstants.TokenEnhancer.KEY_FIELD_AUTH_TYPE)));
            token.setTenantId(ObjectUtils.toString(info.get(SecurityConstants.TokenEnhancer.KEY_FIELD_TENANT_ID)));
        }
        token.setAccessToken(authorization);

        return token;
    }
}
