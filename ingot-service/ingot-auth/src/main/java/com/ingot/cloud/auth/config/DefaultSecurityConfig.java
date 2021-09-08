package com.ingot.cloud.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * <p>Description  : DefaultSecurityConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/6.</p>
 * <p>Time         : 11:16 上午.</p>
 */
@EnableWebSecurity
public class DefaultSecurityConfig {

    @Bean
    UserDetailsService users() {
        UserDetails user = User.builder()
                .username("admin")
                .password("{noop}admin")
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .formLogin(withDefaults())
                .authorizeRequests(authorizeRequests ->
                        authorizeRequests.anyRequest().authenticated()
                ).oauth2ResourceServer().jwt();
        return http.build();
    }
}
