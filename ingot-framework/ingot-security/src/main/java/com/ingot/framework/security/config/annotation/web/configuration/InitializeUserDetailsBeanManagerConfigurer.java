package com.ingot.framework.security.config.annotation.web.configuration;

import com.ingot.framework.security.authentication.IngotDaoAuthenticationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * <p>Description  : 由于{@link IngotOAuth2ResourceServerConfiguration}注入了
 * 多个{@link UserDetailsService}，所以Spring默认的
 * InitializeUserDetailsBeanManagerConfigurer无法提供{@link DaoAuthenticationProvider}
 * 导致无法认证{@link UsernamePasswordAuthenticationToken}.
 * 因为Spring默认配置中，在获取{@link UserDetailsService}时，如果注入了多个会返回null，从而
 * 使得{@link DaoAuthenticationProvider}无法实例成功</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/30.</p>
 * <p>Time         : 7:01 PM.</p>
 */
@Slf4j
@Order(InitializeUserDetailsBeanManagerConfigurer.DEFAULT_ORDER)
public class InitializeUserDetailsBeanManagerConfigurer extends GlobalAuthenticationConfigurerAdapter {

    /**
     * 要在Spring的InitializeUserDetailsBeanManagerConfigurer之前生效
     */
    static final int DEFAULT_ORDER = Ordered.LOWEST_PRECEDENCE - 5010;

    private final ApplicationContext context;

    InitializeUserDetailsBeanManagerConfigurer(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void init(AuthenticationManagerBuilder auth) throws Exception {
        auth.apply(new InitializeUserDetailsBeanManagerConfigurer.InitializeUserDetailsManagerConfigurer());
    }

    class InitializeUserDetailsManagerConfigurer extends GlobalAuthenticationConfigurerAdapter {

        @Override
        public void configure(AuthenticationManagerBuilder auth) throws Exception {
            if (auth.isConfigured()) {
                return;
            }
            UserDetailsService userDetailsService;
            try {
                userDetailsService = getUserDetailsService();
            } catch (BeansException e) {
                log.debug("获取UserDetailsService失败", e);
                return;
            }
            PasswordEncoder passwordEncoder = getBeanOrNull(PasswordEncoder.class);
            UserDetailsPasswordService passwordManager = getBeanOrNull(UserDetailsPasswordService.class);
            IngotDaoAuthenticationProvider provider = new IngotDaoAuthenticationProvider();
            provider.setUserDetailsService(userDetailsService);
            if (passwordEncoder != null) {
                provider.setPasswordEncoder(passwordEncoder);
            }
            if (passwordManager != null) {
                provider.setUserDetailsPasswordService(passwordManager);
            }
            provider.afterPropertiesSet();
            auth.authenticationProvider(provider);
        }

        /**
         * @return a bean of the requested class if there's just a single registered
         * component, null otherwise.
         */
        private <T> T getBeanOrNull(Class<T> type) {
            String[] beanNames = InitializeUserDetailsBeanManagerConfigurer.this.context.getBeanNamesForType(type);
            if (beanNames.length != 1) {
                return null;
            }
            return InitializeUserDetailsBeanManagerConfigurer.this.context.getBean(beanNames[0], type);
        }

        private UserDetailsService getUserDetailsService() {
            return InitializeUserDetailsBeanManagerConfigurer.this.context.getBean(UserDetailsService.class);
        }

    }
}
