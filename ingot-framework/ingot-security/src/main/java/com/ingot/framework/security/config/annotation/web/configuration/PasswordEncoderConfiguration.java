package com.ingot.framework.security.config.annotation.web.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;


/**
 * <p>Description  : DefaultPasswordConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/1/2.</p>
 * <p>Time         : 5:11 PM.</p>
 */
@Configuration(proxyBeanMethods = false)
public class PasswordEncoderConfiguration {

    /**
     * https://spring.io/blog/2017/11/01/spring-security-5-0-0-rc1-released#password-storage-updated
     * Encoded password does not look like BCrypt
     *
     * @return PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
