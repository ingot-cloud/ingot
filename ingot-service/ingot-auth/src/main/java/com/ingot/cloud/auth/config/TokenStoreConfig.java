package com.ingot.cloud.auth.config;

import com.ingot.cloud.auth.token.JwtKeyGenerator;
import com.ingot.framework.security.provider.token.IngotTokenEnhancer;
import com.ingot.framework.security.provider.token.store.IngotJwtTokenStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import static com.ingot.framework.core.constants.BeanIds.JWT_ACCESS_TOKEN_CONVERTER;
import static com.ingot.framework.core.constants.BeanIds.TOKEN_ENHANCER;

/**
 * <p>Description  : TokenStoreConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/3.</p>
 * <p>Time         : 4:59 下午.</p>
 */
@Configuration
public class TokenStoreConfig {

    /**
     * Jwt access token converter jwt access token converter.
     *
     * @return the jwt access token converter
     */
    @Bean(JWT_ACCESS_TOKEN_CONVERTER)
    public JwtAccessTokenConverter jwtAccessTokenConverter(JwtKeyGenerator generator) throws Exception {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        converter.setKeyPair(generator.getKeyPair());
        return converter;
    }

    /**
     * Jwt token enhancer token enhancer.
     *
     * @return the token enhancer
     */
    @Bean(TOKEN_ENHANCER)
    @ConditionalOnBean(TokenEnhancer.class)
    public TokenEnhancer tokenEnhancer() {
        return new IngotTokenEnhancer();
    }

    /**
     * Jwt token store token store.
     *
     * @return the token store
     */
    @Bean
    public IngotJwtTokenStore jwtTokenStore(JwtAccessTokenConverter jwtAccessTokenConverter,
                                            RedisTemplate<String, Object> redisTemplate) {
        return new IngotJwtTokenStore(jwtAccessTokenConverter, redisTemplate);
    }

}
