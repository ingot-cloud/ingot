package com.ingot.cloud.pms.social.config;

import com.ingot.cloud.pms.service.domain.SysSocialDetailsService;
import com.ingot.cloud.pms.social.provider.LocalSocialDetailsProvider;
import com.ingot.framework.social.common.provider.SocialDetailsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Description  : PMS社交配置.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 18:30.</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class PmsSocialConfiguration {

    /**
     * 本地社交详情提供者
     * 优先注册，如果已经有其他提供者则不注册
     */
    @Bean
    public SocialDetailsProvider localSocialDetailsProvider(SysSocialDetailsService sysSocialDetailsService) {
        log.info("PmsSocialConfiguration - 注册本地社交详情提供者");
        return new LocalSocialDetailsProvider(sysSocialDetailsService);
    }
}

