package com.ingot.framework.security.provider.token.store;

import com.ingot.framework.security.provider.token.IngotAuthenticationKeyGenerator;
import com.ingot.framework.security.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
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

    private static final String ACCESS = "token:access:";

    private static final long DEFAULT_ACCESS_TOKEN_EXPIRES_IN = 7200;

    private final RedisTemplate<String, Object> redisTemplate;
    private final IngotAuthenticationKeyGenerator keyGenerator = new IngotAuthenticationKeyGenerator();

    private String prefix;

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

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void storeAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
        super.storeAccessToken(token, authentication);
        String jti = SecurityUtils.getJTI(token);
        String key = keyGenerator.extractKey(jti, authentication);
        String finalKey = getKey(ACCESS + key);

        Date expiration = token.getExpiration();
        long expiresIn = DEFAULT_ACCESS_TOKEN_EXPIRES_IN;
        if (expiration != null) {
            long now = System.currentTimeMillis();
            expiresIn = (expiration.getTime() - now) / 1000;
        }
        redisTemplate.opsForValue().set(finalKey, authentication, expiresIn, TimeUnit.SECONDS);
    }

    @Override
    public void removeAccessToken(OAuth2AccessToken token) {
        super.removeAccessToken(token);
        String key = keyGenerator.extractKey(token);
        String finalKey = getKey(ACCESS + key);

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

    private String getKey(String value) {
        return prefix + value;
    }
}
