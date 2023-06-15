package com.ingot.framework.vc.config;

import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.VCRepository;
import com.ingot.framework.vc.VCSendChecker;
import com.ingot.framework.vc.common.DefaultVCRepository;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.common.VCVerifyResolver;
import com.ingot.framework.vc.module.servlet.*;
import com.ingot.framework.vc.module.sms.*;
import com.ingot.framework.vc.properties.IngotVCProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

/**
 * <p>Description  : VCConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/3/21.</p>
 * <p>Time         : 10:07 PM.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(IngotVCProperties.class)
public class VCConfig {

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public VCVerifyResolver vcVerifyResolver(WebApplicationContext applicationContext,
                                             IngotVCProperties properties) {
        return new VCVerifyResolver(applicationContext, properties);
    }

    @Bean
    @ConditionalOnMissingBean(VCRepository.class)
    public VCRepository repository(RedisTemplate<String, Object> redisTemplate) {
        return new DefaultVCRepository(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(VCFailureHandler.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public VCFailureHandler failureHandler() {
        return new DefaultVCFailureHandler();
    }

    @Bean
    @ConditionalOnMissingBean(VCProviderManager.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public VCProviderManager vcProviderManager(Map<String, VCProvider> providerMap,
                                               Map<String, VCGenerator> generatorMap,
                                               Map<String, VCSendChecker> checkerMap) {
        return new DefaultVCProviderManager(providerMap, generatorMap, checkerMap);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public VCHttpConfigurer vcHttpConfigurer(VCProviderManager vcProviderManager,
                                             VCVerifyResolver vcVerifyResolver,
                                             VCFailureHandler failureHandler) {
        return new VCHttpConfigurer(vcProviderManager, vcVerifyResolver, failureHandler);
    }

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
    public VCSendChecker smsSendChecker(RedisTemplate<String, Object> redisTemplate,
                                        IngotVCProperties properties) {
        return new DefaultSmsVCSendChecker(redisTemplate, properties.getSms());
    }

    @Bean(VCConstants.BEAN_NAME_PROVIDER_SMS)
    @ConditionalOnMissingBean(name = {VCConstants.BEAN_NAME_PROVIDER_SMS})
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public VCProvider smsProvider(VCRepository repository,
                                  SmsCodeSender smsCodeSender) {
        return new DefaultSmsVCProvider(repository, smsCodeSender);
    }

}
