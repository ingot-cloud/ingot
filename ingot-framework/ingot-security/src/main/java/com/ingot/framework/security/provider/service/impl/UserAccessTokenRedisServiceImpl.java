package com.ingot.framework.security.provider.service.impl;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.constants.RedisConstants;
import com.ingot.framework.core.constants.SecurityConstants;
import com.ingot.framework.security.model.dto.UserTokenDto;
import com.ingot.framework.security.provider.service.UserAccessTokenRedisService;
import com.ingot.framework.security.utils.ObjectUtils;
import com.ingot.framework.security.utils.SecurityUtils;
import com.ingot.framework.security.utils.TokenServiceUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.ingot.framework.core.constants.SecurityConstants.AUTH_TYPE_STANDARD;
import static com.ingot.framework.core.constants.SecurityConstants.AUTH_TYPE_UNIQUE;


/**
 * <p>Description  : UserTokenServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/6/4.</p>
 * <p>Time         : 下午1:53.</p>
 */
@Slf4j
@Service
@ConditionalOnBean(value = {TokenServiceUtils.class, RedisTemplate.class})
@AllArgsConstructor
public class UserAccessTokenRedisServiceImpl implements UserAccessTokenRedisService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final TokenServiceUtils tokenServiceUtils;

    @Override public void update(String oldToken, String newToken, int tokenValidateSeconds, UserTokenDto userTokenDto) {
        String authType = SecurityUtils.getAuthType(tokenServiceUtils.readAccessToken(newToken));
        if (StrUtil.isEmpty(authType) || StrUtil.endWithIgnoreCase(authType, AUTH_TYPE_STANDARD)){
            updateStandard(oldToken, newToken, tokenValidateSeconds, userTokenDto);
        } else if (StrUtil.endWithIgnoreCase(authType, AUTH_TYPE_UNIQUE)){
            updateUnique(newToken, tokenValidateSeconds, userTokenDto);
        }
    }

    @Override public void updateUnique(String accessToken, int accessTokenValidateSeconds, UserTokenDto userTokenDto) {
        final String key = getKey(tokenServiceUtils.readAccessToken(accessToken));
        set(key, userTokenDto, accessTokenValidateSeconds);
    }

    @Override public void updateStandard(String oldToken, String newToken, int tokenValidateSeconds, UserTokenDto userTokenDto) {
        if (StrUtil.isNotEmpty(oldToken)){
            String key = getKey(tokenServiceUtils.readAccessToken(oldToken));
            revokeByKey(key);
        }

        String key = getKey(tokenServiceUtils.readAccessToken(newToken));
        set(key, userTokenDto, tokenValidateSeconds);
    }

    @Override public void revoke(String accessToken) {
        OAuth2AccessToken token = tokenServiceUtils.readAccessToken(accessToken);
        revokeByKey(getKey(token));
    }

    @Override public void revokeByKey(String key) {
        if (StrUtil.isNotEmpty(key)){
            redisTemplate.opsForValue().getOperations().delete(key);
        }
    }

    @Override public void revokeByUserInfo(String userId, String username) {
        Set<String> keys = redisTemplate.keys(RedisConstants.userTokenPreKey(userId, username) + "*");
        if (keys != null){
            keys.forEach(this::revokeByKey);
        }
    }

    @Override public UserTokenDto getAccessToken(String accessToken) {
        OAuth2AccessToken token = tokenServiceUtils.readAccessToken(accessToken);
        String key = getKey(token);
        if (StrUtil.isEmpty(key)){
            return null;
        }
        return (UserTokenDto) redisTemplate.opsForValue().get(key);
    }

    @Override public boolean isValid(String accessToken) {
        return getAccessToken(accessToken) != null;
    }

    private void set(String key, UserTokenDto userTokenDto, int tokenValidateSeconds){
        if (StrUtil.isNotEmpty(key)){
            redisTemplate.opsForValue().set(key, userTokenDto, tokenValidateSeconds, TimeUnit.SECONDS);
        }
    }

    private String getKey(OAuth2AccessToken accessToken){
        Map<String, Object> info = accessToken.getAdditionalInformation();
        String userId = ObjectUtils.toString(info.get(SecurityConstants.TOKEN_ENHANCER_KEY_USER_ID));
        String userName = ObjectUtils.toString(info.get(SecurityConstants.TOKEN_ENHANCER_KEY_USER_NAME));
        String authType = ObjectUtils.toString(info.get(SecurityConstants.TOKEN_ENHANCER_KEY_AUTH_TYPE));
        if (StrUtil.isEmpty(authType) || StrUtil.endWithIgnoreCase(authType, AUTH_TYPE_STANDARD)){
            return RedisConstants.userStandardAccessTokenKey(accessToken.getValue(), userId, userName);
        } else if (StrUtil.endWithIgnoreCase(authType, AUTH_TYPE_UNIQUE)){
            return RedisConstants.userUniqueAccessTokenKey(userId, userName);
        }
        return null;
    }

}
