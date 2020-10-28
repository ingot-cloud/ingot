package com.ingot.framework.security.config;

import com.ingot.framework.security.core.encoder.NoPasswordEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import static com.ingot.framework.core.constants.BeanIds.CLIENT_DETAIL_PASSWORD_ENCODER;
import static com.ingot.framework.core.constants.BeanIds.USER_PASSWORD_ENCODER;


/**
 * <p>Description  : DefaultPasswordConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/1/2.</p>
 * <p>Time         : 5:11 PM.</p>
 */
@Configuration
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

    /**
     * Client detail PasswordEncoder
     */
    @Bean(CLIENT_DETAIL_PASSWORD_ENCODER)
    public PasswordEncoder clientDetailPasswordEncoder() {
        return new NoPasswordEncoder();
    }

    /**
     * User PasswordEncoder
     */
    @Bean(USER_PASSWORD_ENCODER)
    public PasswordEncoder userPasswordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
