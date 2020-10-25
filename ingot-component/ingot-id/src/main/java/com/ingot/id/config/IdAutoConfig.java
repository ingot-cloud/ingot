package com.ingot.id.config;

import com.ingot.id.properties.IdProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Description  : IdAutoConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/25.</p>
 * <p>Time         : 3:30 下午.</p>
 */
@Configuration
@EnableConfigurationProperties(value = IdProperties.class)
public class IdAutoConfig {

}
