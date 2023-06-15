package com.ingot.framework.vc.config;

import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.VCRepository;
import com.ingot.framework.vc.VCSendChecker;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.common.VCVerifyResolver;
import com.ingot.framework.vc.module.servlet.*;
import com.ingot.framework.vc.module.sms.DefaultSmsVCProvider;
import com.ingot.framework.vc.module.sms.SmsCodeSender;
import com.ingot.framework.vc.properties.IngotVCProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

/**
 * <p>Description  : VCServletConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/15.</p>
 * <p>Time         : 3:53 PM.</p>
 */
@Configuration(proxyBeanMethods = false)
@Import({VCController.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class VCServletConfig {

    @Bean
    public VCVerifyResolver vcVerifyResolver(WebApplicationContext applicationContext,
                                             IngotVCProperties properties) {
        return new VCVerifyResolver(applicationContext, properties);
    }

    @Bean
    @ConditionalOnMissingBean(VCFailureHandler.class)
    public VCFailureHandler failureHandler() {
        return new DefaultVCFailureHandler();
    }

    @Bean
    @ConditionalOnMissingBean(VCProviderManager.class)
    public VCProviderManager vcProviderManager(Map<String, VCProvider> providerMap,
                                               Map<String, VCGenerator> generatorMap,
                                               Map<String, VCSendChecker> checkerMap) {
        return new DefaultVCProviderManager(providerMap, generatorMap, checkerMap);
    }

    @Bean
    public VCHttpConfigurer vcHttpConfigurer(VCProviderManager vcProviderManager,
                                             VCVerifyResolver vcVerifyResolver,
                                             VCFailureHandler failureHandler) {
        return new VCHttpConfigurer(vcProviderManager, vcVerifyResolver, failureHandler);
    }

    @Bean(VCConstants.BEAN_NAME_PROVIDER_SMS)
    @ConditionalOnMissingBean(name = {VCConstants.BEAN_NAME_PROVIDER_SMS})
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public VCProvider smsProvider(VCRepository repository,
                                  SmsCodeSender smsCodeSender) {
        return new DefaultSmsVCProvider(repository, smsCodeSender);
    }
}
