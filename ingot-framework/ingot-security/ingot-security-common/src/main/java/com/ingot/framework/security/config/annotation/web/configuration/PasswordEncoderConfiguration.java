package com.ingot.framework.security.config.annotation.web.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


/**
 * <p>提供带算法前缀的委派密码编码器，并对无 {@code {id}} 前缀的历史哈希按 BCrypt 校验。</p>
 *
 * <p>{@link PasswordEncoderFactories#createDelegatingPasswordEncoder()} 写出
 * {@code {bcrypt}$2a$...}；{@link DelegatingPasswordEncoder#setDefaultPasswordEncoderForMatches}
 * 是 Spring 对存量裸 BCrypt 哈希的官方兼容方式，不改变新哈希的存储格式。</p>
 *
 * @author wangchao
 * @since 1.0.0
 * @see DelegatingPasswordEncoder#setDefaultPasswordEncoderForMatches(PasswordEncoder)
 */
@Configuration(proxyBeanMethods = false)
public class PasswordEncoderConfiguration {

    /**
     * 创建委派编码器，并把无前缀哈希的匹配算法设为 BCrypt。
     *
     * @return 可用于存储与校验的 {@link PasswordEncoder}
     */
    public static PasswordEncoder createDelegatingPasswordEncoder() {
        DelegatingPasswordEncoder encoder =
                (DelegatingPasswordEncoder) PasswordEncoderFactories.createDelegatingPasswordEncoder();
        encoder.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder());
        return encoder;
    }

    /**
     * 注册与登录、改密共用的 {@link PasswordEncoder}。
     *
     * @return 带 BCrypt 缺省匹配的委派编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return createDelegatingPasswordEncoder();
    }
}
