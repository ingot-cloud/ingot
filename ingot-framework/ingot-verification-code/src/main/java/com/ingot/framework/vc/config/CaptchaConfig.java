package com.ingot.framework.vc.config;

import com.anji.captcha.config.AjCaptchaServiceAutoConfiguration;
import com.anji.captcha.properties.AjCaptchaProperties;
import com.anji.captcha.service.CaptchaService;
import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.VCSendChecker;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.module.captcha.DefaultCaptchaVCGenerator;
import com.ingot.framework.vc.module.captcha.DefaultCaptchaVCProvider;
import com.ingot.framework.vc.module.captcha.DefaultCaptchaVCSendChecker;
import com.ingot.framework.vc.module.servlet.VCProvider;
import com.ingot.framework.vc.properties.IngotVCProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

/**
 * <p>Description  : CaptchaConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/25.</p>
 * <p>Time         : 3:16 PM.</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class CaptchaConfig implements BeanPostProcessor {
    private final AjCaptchaProperties ajCaptchaProperties;
    private final IngotVCProperties ingotVCProperties;

    @Override
    public Object postProcessBeforeInitialization(@NonNull Object bean,
                                                  @NonNull String beanName) throws BeansException {
        if (bean instanceof CaptchaService) {
            // 替换captchaService
            ajCaptchaProperties.setWaterMark(ingotVCProperties.getImage().getWaterMark());
            return new AjCaptchaServiceAutoConfiguration().captchaService(ajCaptchaProperties);
        }
        return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
    }

    @Bean(VCConstants.BEAN_NAME_GENERATOR_IMAGE)
    @ConditionalOnMissingBean(name = {VCConstants.BEAN_NAME_GENERATOR_IMAGE})
    public VCGenerator imageGenerator(IngotVCProperties properties) {
        return new DefaultCaptchaVCGenerator(properties.getImage());
    }

    @Bean(VCConstants.BEAN_NAME_SEND_CHECKER_IMAGE)
    @ConditionalOnMissingBean(name = {VCConstants.BEAN_NAME_SEND_CHECKER_IMAGE})
    public VCSendChecker imageSendChecker() {
        return new DefaultCaptchaVCSendChecker();
    }

    @Bean(VCConstants.BEAN_NAME_PROVIDER_IMAGE)
    @ConditionalOnMissingBean(name = {VCConstants.BEAN_NAME_PROVIDER_IMAGE})
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public VCProvider imageProvider(CaptchaService captchaService) {
        return new DefaultCaptchaVCProvider(captchaService);
    }
}
