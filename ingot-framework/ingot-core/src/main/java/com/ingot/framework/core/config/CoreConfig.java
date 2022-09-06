package com.ingot.framework.core.config;

import com.ingot.framework.core.model.transform.CommonTypeTransform;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * <p>Description  : CoreConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/9/6.</p>
 * <p>Time         : 4:37 PM.</p>
 */
@AutoConfiguration
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
}
