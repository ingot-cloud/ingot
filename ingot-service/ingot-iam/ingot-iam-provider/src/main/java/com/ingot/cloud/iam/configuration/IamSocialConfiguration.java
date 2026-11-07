package com.ingot.cloud.iam.configuration;

import com.ingot.cloud.iam.infrastructure.persistence.store.SocialConfigStore;
import com.ingot.cloud.iam.infrastructure.social.LocalSocialDetailsProvider;
import com.ingot.framework.social.common.provider.SocialDetailsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Description  : IAM社交配置.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 18:30.</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class IamSocialConfiguration {

    /**
     * 本地社交详情提供者
     * 优先注册，如果已经有其他提供者则不注册
     */
    @Bean
    public SocialDetailsProvider localSocialDetailsProvider(SocialConfigStore socialConfigs) {
        log.info("IamSocialConfiguration - 注册本地社交详情提供者");
        return new LocalSocialDetailsProvider(socialConfigs);
    }
}

