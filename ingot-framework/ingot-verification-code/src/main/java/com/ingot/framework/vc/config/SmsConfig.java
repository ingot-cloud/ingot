package com.ingot.framework.vc.config;

import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.VCRepository;
import com.ingot.framework.vc.VCPreChecker;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.module.reactive.VCProcessor;
import com.ingot.framework.vc.module.servlet.VCProvider;
import com.ingot.framework.vc.module.sms.*;
import com.ingot.framework.vc.properties.IngotVCProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * <p>Description  : SmsConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/16.</p>
 * <p>Time         : 3:59 PM.</p>
 */
@Configuration(proxyBeanMethods = false)
public class SmsConfig {

    @Bean
    @ConditionalOnMissingBean(SmsCodeSender.class)
    public SmsCodeSender smsCodeSender() {
        return new DefaultSmsCodeSender();
    }

    @Bean(VCConstants.BEAN_NAME_GENERATOR_SMS)
    @ConditionalOnMissingBean(name = {VCConstants.BEAN_NAME_GENERATOR_SMS})
    public VCGenerator smsGenerator(IngotVCProperties properties) {
        return new DefaultSmsVCGenerator(properties.getSms());
    }

    @Bean(VCConstants.BEAN_NAME_SEND_CHECKER_SMS)
    @ConditionalOnMissingBean(name = {VCConstants.BEAN_NAME_SEND_CHECKER_SMS})
    public VCPreChecker smsSendChecker(RedisTemplate<String, Object> redisTemplate,
                                       IngotVCProperties properties) {
        return new DefaultSmsVCPreChecker(redisTemplate, properties.getSms());
    }

    @Bean(VCConstants.BEAN_NAME_PROVIDER_SMS)
    @ConditionalOnMissingBean(name = {VCConstants.BEAN_NAME_PROVIDER_SMS})
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public VCProvider smsProvider(VCRepository repository,
                                  SmsCodeSender codeSender) {
        return new DefaultSmsVCProvider(repository, codeSender);
    }

    @Bean(VCConstants.BEAN_NAME_PROCESSOR_SMS)
    @ConditionalOnMissingBean(name = {VCConstants.BEAN_NAME_PROCESSOR_SMS})
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public VCProcessor smsProcessor(VCRepository repository,
                                    SmsCodeSender codeSender) {
        return new DefaultSmsVCProcessor(repository, codeSender);
    }

}
