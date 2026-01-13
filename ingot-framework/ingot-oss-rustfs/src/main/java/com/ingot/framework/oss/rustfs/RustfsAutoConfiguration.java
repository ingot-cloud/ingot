package com.ingot.framework.oss.rustfs;

import com.ingot.framework.commons.oss.OssService;
import com.ingot.framework.oss.rustfs.properties.RustfsProperties;
import com.ingot.framework.oss.rustfs.service.RustfsOssService;
import com.ingot.framework.oss.rustfs.service.RustfsService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * <p>Description  : RustfsAutoConfiguration.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/12.</p>
 * <p>Time         : 10:30.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties({RustfsProperties.class})
public class RustfsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RustfsService.class)
    @ConditionalOnProperty(name = "ingot.oss.rustfs.url")
    public RustfsService rustfsService(RustfsProperties properties) {
        return new RustfsService(
                properties.getUrl(),
                properties.getAccessKey(),
                properties.getSecretKey(),
                properties.getRegion()
        );
    }

    @Bean
    @ConditionalOnMissingBean(OssService.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public OssService rustfsOSSService(RustfsService rustfsService,
                                       RustfsProperties properties) {
        return new RustfsOssService(rustfsService, properties);
    }
}
