package com.ingot.framework.security.provider.token.store;

import com.ingot.framework.core.constants.RedisConstants;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.model.UserStoreToken;
import com.ingot.framework.security.provider.token.IngotAuthenticationKeyGenerator;
import com.ingot.framework.security.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * <p>Description  : IngotJwtTokenStore.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/20.</p>
 * <p>Time         : 2:06 下午.</p>
 */
@Slf4j
public class IngotJwtTokenStore extends JwtTokenStore {

    private static final String ACCESS = RedisConstants.BASE_PREFIX + "token:access:";

    private static final long DEFAULT_ACCESS_TOKEN_EXPIRES_IN = 7200;

    private final RedisTemplate<String, Object> redisTemplate;
    private final IngotAuthenticationKeyGenerator keyGenerator = new IngotAuthenticationKeyGenerator();

    /**
     * Create a JwtTokenStore with this token enhancer (should be shared with the DefaultTokenServices if used).
     *
     * @param jwtTokenEnhancer token enhancer
     */
    public IngotJwtTokenStore(JwtAccessTokenConverter jwtTokenEnhancer,
                              RedisTemplate<String, Object> redisTemplate) {
        super(jwtTokenEnhancer);
        this.redisTemplate = redisTemplate;
    }

    /**
     * 根据 Token 获取当前保存的授权信息
     * @param token OAuth2AccessToken
     * @return UserStoreToken
     */
    public UserStoreToken getUserStoreToken(OAuth2AccessToken token){
        String key = keyGenerator.extractKey(token);
        String finalKey = getKey(key);

        return (UserStoreToken) redisTemplate.opsForValue().get(finalKey);
    }

    @Override
    public void storeAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
        super.storeAccessToken(token, authentication);
        String jti = SecurityUtils.getJTI(token);
        String key = keyGenerator.extractKey(jti, authentication);
        String finalKey = getKey(key);

        Date expiration = token.getExpiration();
        long expiresIn = DEFAULT_ACCESS_TOKEN_EXPIRES_IN;
        if (expiration != null) {
            long now = System.currentTimeMillis();
            expiresIn = (expiration.getTime() - now) / 1000;
        }

        UserStoreToken userStoreToken = getUserStoreToken(jti, authentication);
        redisTemplate.opsForValue().set(finalKey, userStoreToken, expiresIn, TimeUnit.SECONDS);
    }

    @Override
    public void removeAccessToken(OAuth2AccessToken token) {
        super.removeAccessToken(token);
        String key = keyGenerator.extractKey(token);
        String finalKey = getKey(key);

        redisTemplate.opsForValue().getOperations().delete(finalKey);
    }

    @Override
    public void storeRefreshToken(OAuth2RefreshToken refreshToken, OAuth2Authentication authentication) {
        super.storeRefreshToken(refreshToken, authentication);
    }

    @Override
    public void removeRefreshToken(OAuth2RefreshToken token) {
        super.removeRefreshToken(token);
    }

    @Override
    public void removeAccessTokenUsingRefreshToken(OAuth2RefreshToken refreshToken) {
        super.removeAccessTokenUsingRefreshToken(refreshToken);
    }

    private UserStoreToken getUserStoreToken(String jti, OAuth2Authentication authentication){
        IngotUser user = SecurityAuthContext.getUser(authentication);
        if (user == null){
            throw new InvalidTokenException("Invalid token");
        }
        UserStoreToken userStoreToken = new UserStoreToken();
        userStoreToken.setJti(jti);
        userStoreToken.setUserId(String.valueOf(user.getId()));
        userStoreToken.setUsername(user.getUsername());
        userStoreToken.setAuthType(user.getAuthType());
        userStoreToken.setTenantId(String.valueOf(user.getTenantId()));
        return userStoreToken;
    }

    private String getKey(String value) {
        return ACCESS + value;
    }
}
