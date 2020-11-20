package com.ingot.framework.security.provider.token.store;

import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

/**
 * <p>Description  : IngotJwtTokenStore.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/20.</p>
 * <p>Time         : 2:06 下午.</p>
 */
public class IngotJwtTokenStore extends JwtTokenStore {
    /**
     * Create a JwtTokenStore with this token enhancer (should be shared with the DefaultTokenServices if used).
     *
     * @param jwtTokenEnhancer token enhancer
     */
    public IngotJwtTokenStore(JwtAccessTokenConverter jwtTokenEnhancer) {
        super(jwtTokenEnhancer);
    }
}
