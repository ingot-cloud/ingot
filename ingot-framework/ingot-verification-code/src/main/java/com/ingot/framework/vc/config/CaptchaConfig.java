package com.ingot.framework.vc.config;

import com.anji.captcha.service.CaptchaService;
import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.VCRepository;
import com.ingot.framework.vc.VCSendChecker;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.module.captcha.DefaultCaptchaVCGenerator;
import com.ingot.framework.vc.module.captcha.DefaultCaptchaVCProvider;
import com.ingot.framework.vc.module.captcha.DefaultCaptchaVCSendChecker;
import com.ingot.framework.vc.module.servlet.VCProvider;
import com.ingot.framework.vc.properties.IngotVCProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * <p>Description  : CaptchaConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/25.</p>
 * <p>Time         : 3:16 PM.</p>
 */
@Configuration(proxyBeanMethods = false)
public class CaptchaConfig {

    @Bean(VCConstants.BEAN_NAME_GENERATOR_IMAGE)
    @ConditionalOnMissingBean(name = {VCConstants.BEAN_NAME_GENERATOR_IMAGE})
    public VCGenerator imageGenerator(IngotVCProperties properties) {
        return new DefaultCaptchaVCGenerator(properties.getImage());
    }

    @Bean(VCConstants.BEAN_NAME_SEND_CHECKER_IMAGE)
    @ConditionalOnMissingBean(name = {VCConstants.BEAN_NAME_SEND_CHECKER_IMAGE})
    public VCSendChecker imageSendChecker(RedisTemplate<String, Object> redisTemplate,
                                          IngotVCProperties properties) {
        return new DefaultCaptchaVCSendChecker();
    }

    @Bean(VCConstants.BEAN_NAME_PROVIDER_IMAGE)
    @ConditionalOnMissingBean(name = {VCConstants.BEAN_NAME_PROVIDER_IMAGE})
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public VCProvider imageProvider(VCRepository repository,
                                    CaptchaService captchaService) {
        return new DefaultCaptchaVCProvider(captchaService);
    }
}
