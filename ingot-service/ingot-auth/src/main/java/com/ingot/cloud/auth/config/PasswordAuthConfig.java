package com.ingot.cloud.auth.config;

import com.ingot.cloud.auth.authentication.IngotAuthenticationFailureHandler;
import com.ingot.cloud.auth.authentication.IngotAuthenticationSuccessHandler;
import com.ingot.cloud.auth.authentication.password.PasswordAuthenticationFilter;
import com.ingot.cloud.auth.authentication.password.PasswordAuthenticationProvider;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * <p>Description  : PasswordAuthConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/4.</p>
 * <p>Time         : 11:40 上午.</p>
 */
@Configuration
@AllArgsConstructor
public class PasswordAuthConfig extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {
    private final PasswordEncoder userPasswordEncoder;
    private final UserDetailsService userDetailsService;

    @SneakyThrows
    @Override public void configure(HttpSecurity http) {
        PasswordAuthenticationFilter filter = new PasswordAuthenticationFilter();
        filter.setAuthenticationManager(http.getSharedObject(AuthenticationManager.class));
        filter.setAuthenticationSuccessHandler(new IngotAuthenticationSuccessHandler());
        filter.setAuthenticationFailureHandler(new IngotAuthenticationFailureHandler());

        PasswordAuthenticationProvider provider = new PasswordAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(userPasswordEncoder);

        http.authenticationProvider(provider)
                .addFilterAfter(filter, UsernamePasswordAuthenticationFilter.class);

    }
}
