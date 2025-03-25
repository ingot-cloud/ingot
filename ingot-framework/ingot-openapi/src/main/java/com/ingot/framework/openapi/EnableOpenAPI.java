package com.ingot.framework.openapi;

import com.ingot.framework.core.io.YamlPropertySourceFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import java.lang.annotation.*;

/**
 * <p>Description  : EnableOpenAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/20.</p>
 * <p>Time         : 09:47.</p>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@EnableConfigurationProperties(OpenAPIProperties.class)
@Import({OpenAPIDefinitionImportSelector.class})
@PropertySource(value = "classpath:openapi-config.yaml", factory = YamlPropertySourceFactory.class)
public @interface EnableOpenAPI {
    /**
     * 网关路由前缀
     *
     * @return String
     */
    String value() default "";

    /**
     * 是否是微服务架构
     * @return true
     */
    boolean isMicro() default true;
}
