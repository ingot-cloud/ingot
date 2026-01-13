package com.ingot.framework.oss.rustfs.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Description  : RustfsProperties.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/12.</p>
 * <p>Time         : 10:30.</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ingot.oss.rustfs")
public class RustfsProperties {
    /**
     * RustFS 服务地址 http://ip:port
     */
    private String url;
    
    /**
     * Access Key
     */
    private String accessKey;
    
    /**
     * Secret Key
     */
    private String secretKey;
    
    /**
     * 区域，默认为 us-east-1
     */
    private String region = "us-east-1";
    
    /**
     * 过期时间，单位秒，默认300秒过期时间
     */
    private Integer expiredTime = 300;
    
    /**
     * 端点配置
     */
    private Endpoint endpoint;

    @Data
    public static class Endpoint {
        /**
         * 是否开启端点
         */
        private boolean enable = false;
        
        /**
         * 端点访问前缀，e.g. oss，那么端点为/oss/rustfs
         */
        private String name;
    }
}
