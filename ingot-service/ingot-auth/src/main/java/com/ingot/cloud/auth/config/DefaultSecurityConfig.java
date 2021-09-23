package com.ingot.cloud.auth.config;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.model.enums.UserStatusEnum;
import com.ingot.framework.core.wrapper.R;
import com.ingot.framework.security.common.constants.TokenAuthMethod;
import com.ingot.framework.security.core.userdetails.RemoteUserDetailsService;
import com.ingot.framework.security.core.userdetails.UserDetailsRequest;
import com.ingot.framework.security.core.userdetails.UserDetailsResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Description  : DefaultSecurityConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/6.</p>
 * <p>Time         : 11:16 上午.</p>
 */
@Configuration(proxyBeanMethods = false)
public class DefaultSecurityConfig {

//    @Bean
//    UserDetailsService users() {
//        UserDetails user = User.builder()
//                .username("admin")
//                .password("{noop}admin")
//                .roles("ADMIN")
//                .build();
//        return new InMemoryUserDetailsManager(user);
//    }

    @Bean
    public RemoteUserDetailsService remoteUserDetailsService() {
        return (UserDetailsRequest params) -> {
            String username = params.getUniqueCode();
            if (!StrUtil.equals(username, "admin")) {
                return R.error("N00", "a");
            }

            return R.ok(UserDetailsResponse.builder()
                    .username("admin")
                    .password("{noop}admin")
                    .roles(ListUtil.of("ADMIN", "MANAGER"))
                    .status(UserStatusEnum.ENABLE)
                    .tokenAuthenticationMethod(TokenAuthMethod.STANDARD.getValue())
                    .id(1L)
                    .tenantId(1)
                    .deptId(1L)
                    .build());
        };
    }
}
