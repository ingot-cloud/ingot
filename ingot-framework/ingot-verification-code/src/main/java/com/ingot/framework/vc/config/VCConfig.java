package com.ingot.framework.vc.config;

import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.VCRepository;
import com.ingot.framework.vc.VCSendChecker;
import com.ingot.framework.vc.common.DefaultVCRepository;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.module.sms.DefaultSmsCodeSender;
import com.ingot.framework.vc.module.sms.DefaultSmsVCGenerator;
import com.ingot.framework.vc.module.sms.DefaultSmsVCSendChecker;
import com.ingot.framework.vc.module.sms.SmsCodeSender;
import com.ingot.framework.vc.properties.IngotVCProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * <p>Description  : VCConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/3/21.</p>
 * <p>Time         : 10:07 PM.</p>
 */
@AutoConfiguration
@Import({VCServletConfig.class})
@EnableConfigurationProperties(IngotVCProperties.class)
public class VCConfig {

    @Bean
    @ConditionalOnMissingBean(VCRepository.class)
    public VCRepository repository(RedisTemplate<String, Object> redisTemplate) {
        return new DefaultVCRepository(redisTemplate);
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

}
