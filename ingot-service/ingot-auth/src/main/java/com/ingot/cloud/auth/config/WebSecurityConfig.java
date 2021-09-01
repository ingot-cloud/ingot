package com.ingot.cloud.auth.config;

import com.ingot.framework.security.config.SecurityConfigManager;
import com.ingot.framework.security.service.ResourcePermitService;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import java.util.List;

/**
 * <p>Description  : WebSecurityConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/11.</p>
 * <p>Time         : 上午11:06.</p>
 */
@Slf4j
@Configuration
@AllArgsConstructor
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    private final ResourcePermitService resourcePermitService;
//    private final PasswordAuthConfig passwordAuthConfig;
//    private final MobileSecurityConfig mobileSecurityConfig;
    private final SecurityConfigManager securityConfigManager;

    /**
     * 注入默认 AuthenticationManager
     */
    @SneakyThrows
    @Bean(name = BeanIds.AUTHENTICATION_MANAGER)
    @Override public AuthenticationManager authenticationManagerBean() {
        return super.authenticationManagerBean();
    }

    @SneakyThrows
    @Override protected void configure(HttpSecurity http) {
        log.info(">> WebSecurityConfig [configure] http security");
        List<String> permit = resourcePermitService.allResourcePermitAntPatterns();
        log.info(">> WebSecurityConfig permit={}", permit);

        http
                .formLogin()
//                .loginPage("/token/login")
//                .loginProcessingUrl("/token/form")
//                    .and()
//                .apply(passwordAuthConfig)
//                    .and()
//                .apply(mobileSecurityConfig)
                    .and()
                .authorizeRequests().antMatchers(permit.toArray(new String[0])).permitAll()
                    .and()
                .csrf().disable();

        securityConfigManager.config(http);
    }

    /**
     * 不拦截静态资源
     */
    @Override public void configure(WebSecurity web) {
        web.ignoring().antMatchers("/css/**");
    }
}
