package com.ingot.framework.minio;

import com.ingot.framework.core.oss.OSSService;
import com.ingot.framework.minio.properties.MinioProperties;
import com.ingot.framework.minio.service.MinioOSSService;
import com.ingot.framework.minio.service.MinioService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * <p>Description  : AutoConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-27.</p>
 * <p>Time         : 14:12.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties({MinioProperties.class})
public class MinioAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MinioService.class)
    @ConditionalOnProperty(name = "ingot.minio.url")
    public MinioService minioService(MinioProperties properties) {
        return new MinioService(
                properties.getUrl(),
                properties.getAccessKey(),
                properties.getSecretKey()
        );
    }

    @Bean
    @ConditionalOnMissingBean(OSSService.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public OSSService minioOSSService(MinioService minioService,
                                      MinioProperties properties) {
        return new MinioOSSService(minioService, properties);
    }
}
