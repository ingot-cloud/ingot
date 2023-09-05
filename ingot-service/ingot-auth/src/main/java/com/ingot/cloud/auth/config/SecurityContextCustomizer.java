package com.ingot.cloud.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.web.context.RedisSecurityContextRepository;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.SecurityContextConfigurer;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

/**
 * <p>Description  : SecurityContextCustomizer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/5.</p>
 * <p>Time         : 4:19 PM.</p>
 */
@RequiredArgsConstructor
public class SecurityContextCustomizer implements Customizer<SecurityContextConfigurer<HttpSecurity>> {
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void customize(SecurityContextConfigurer<HttpSecurity> configurer) {
        configurer.securityContextRepository(new DelegatingSecurityContextRepository(
                new RedisSecurityContextRepository(redisTemplate),
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository()));
    }
}
