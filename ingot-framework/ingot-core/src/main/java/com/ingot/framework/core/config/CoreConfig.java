package com.ingot.framework.core.config;

import com.ingot.framework.core.context.SpringContextHolder;
import com.ingot.framework.commons.model.transform.CommonTypeTransform;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

/**
 * <p>Description  : CoreConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/9/6.</p>
 * <p>Time         : 4:37 PM.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(CoreProperties.class)
public class CoreConfig {

    /**
     * mapstruct 公共类型转换
     *
     * @return {@link CommonTypeTransform}
     */
    @Bean
    public CommonTypeTransform commonTypeTransform() {
        return new CommonTypeTransform();
    }

    @Bean
    @Lazy(false)
    public SpringContextHolder springContextHolder() {
        return new SpringContextHolder();
    }
}
