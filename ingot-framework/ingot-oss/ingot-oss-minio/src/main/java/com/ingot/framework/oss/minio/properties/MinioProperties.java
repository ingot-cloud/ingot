package com.ingot.framework.oss.minio.properties;

import java.time.Duration;

import com.ingot.framework.oss.common.OssDefaults;
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
    /**
     * 签名区域，默认 {@link OssDefaults#DEFAULT_REGION}。
     * <p>写入 {@code MinioClient} 后预签名不再调用 {@code GetBucketLocation}。</p>
     */
    private String region = OssDefaults.DEFAULT_REGION;
    /**
     * TCP 连接超时，默认 {@link OssDefaults#DEFAULT_CONNECT_TIMEOUT}（3s）。
     * <p>YAML 示例：{@code ingot.oss.minio.connect-timeout: 3s}。不缩短读写超时。</p>
     */
    private Duration connectTimeout = OssDefaults.DEFAULT_CONNECT_TIMEOUT;
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
         * 端点访问前缀，e.g. oss，那么端点为/oss/minio
         */
        private String name;
    }
}

