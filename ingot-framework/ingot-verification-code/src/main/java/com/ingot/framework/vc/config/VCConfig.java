package com.ingot.framework.vc.config;

import com.ingot.framework.vc.VCRepository;
import com.ingot.framework.vc.common.DefaultVCRepository;
import com.ingot.framework.vc.properties.InVCProperties;
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
@Import({VCReactiveConfig.class,
        VCServletConfig.class,
        SmsConfig.class,
        EmailConfig.class,
        CaptchaConfig.class})
@EnableConfigurationProperties(InVCProperties.class)
public class VCConfig {

    @Bean
    @ConditionalOnMissingBean(VCRepository.class)
    public VCRepository repository(RedisTemplate<String, Object> redisTemplate) {
        return new DefaultVCRepository(redisTemplate);
    }

}
