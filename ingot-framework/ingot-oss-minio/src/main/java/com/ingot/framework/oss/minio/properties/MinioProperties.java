package com.ingot.framework.oss.minio.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Description  : MinioProperties.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-27.</p>
 * <p>Time         : 14:19.</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ingot.oss.minio")
public class MinioProperties {
    /**
     * minio 服务地址 http://ip:port
     */
    private String url;
    /**
     * 用户名
     */
    private String accessKey;
    /**
     * 密码
     */
    private String secretKey;
    /**
     * 过期时间，单位秒，默认300秒过期时间
     */
    private Integer expiredTime = 300;

}

