package com.ingot.framework.security.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * <p>Description  : DefaultSecurityConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/25.</p>
 * <p>Time         : 3:48 下午.</p>
 */
public class DefaultSecurityConfig {

    @Bean
    @ConditionalOnMissingBean(SecurityConfigManager.class)
    public SecurityConfigManager securityConfigManager(List<SecurityConfigProvider> providers) {
        return new DefaultSecurityConfigManager(providers);
    }
}
