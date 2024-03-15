package com.ingot.framework.vc.config;

import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.VCRepository;
import com.ingot.framework.vc.VCPreChecker;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.module.email.*;
import com.ingot.framework.vc.module.reactive.VCProcessor;
import com.ingot.framework.vc.module.servlet.VCProvider;
import com.ingot.framework.vc.properties.IngotVCProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * <p>Description  : EmailConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/16.</p>
 * <p>Time         : 4:01 PM.</p>
 */
@Configuration(proxyBeanMethods = false)
public class EmailConfig {

    @Bean
    @ConditionalOnMissingBean(EmailCodeSender.class)
    public EmailCodeSender emailCodeSender() {
        return new DefaultEmailCodeSender();
    }

    @Bean(VCConstants.BEAN_NAME_GENERATOR_EMAIL)
    @ConditionalOnMissingBean(name = {VCConstants.BEAN_NAME_GENERATOR_EMAIL})
    public VCGenerator emailGenerator(IngotVCProperties properties) {
        return new DefaultEmailVCGenerator(properties.getEmail());
    }

    @Bean(VCConstants.BEAN_NAME_SEND_CHECKER_EMAIL)
    @ConditionalOnMissingBean(name = {VCConstants.BEAN_NAME_SEND_CHECKER_EMAIL})
    public VCPreChecker smsSendChecker(RedisTemplate<String, Object> redisTemplate,
                                       IngotVCProperties properties) {
        return new DefaultEmailVCPreChecker(redisTemplate, properties.getEmail());
    }

    @Bean(VCConstants.BEAN_NAME_PROVIDER_EMAIL)
    @ConditionalOnMissingBean(name = {VCConstants.BEAN_NAME_PROVIDER_EMAIL})
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public VCProvider emailProvider(VCRepository repository,
                                    EmailCodeSender codeSender) {
        return new DefaultEmailVCProvider(repository, codeSender);
    }

    @Bean(VCConstants.BEAN_NAME_PROCESSOR_EMAIL)
    @ConditionalOnMissingBean(name = {VCConstants.BEAN_NAME_PROCESSOR_EMAIL})
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public VCProcessor emailProcessor(VCRepository repository,
                                      EmailCodeSender codeSender) {
        return new DefaultEmailVCProcessor(repository, codeSender);
    }
}
