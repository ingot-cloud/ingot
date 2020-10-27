package org.springframework.cloud.openfeign;

import feign.hystrix.HystrixFeign;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * <p>Description  : IngotHystrixFeignTargeterConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/26.</p>
 * <p>Time         : 3:42 PM.</p>
 */
@Configuration
@ConditionalOnClass(HystrixFeign.class)
@ConditionalOnProperty("feign.hystrix.enabled")
public class IngotHystrixFeignTargeterConfiguration {

    @Bean
    @Primary
    public Targeter ingotFeignTargeter() {
        return new IngotHystrixTargeter();
    }
}
