package com.ingot.cloud.iam.authorization.read;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * <p>装配 IAM 内部读取复用，不改变安全过滤链和其他服务配置。</p>
 * @author jy
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
public class IamReadRequestConfiguration implements WebMvcConfigurer {
    /** {@inheritDoc} */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new IamReadRequestInterceptor());
    }
}
