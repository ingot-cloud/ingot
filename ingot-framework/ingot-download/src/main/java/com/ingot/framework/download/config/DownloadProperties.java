package com.ingot.framework.download.config;

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>下载中心配置，前缀 {@code ingot.download}。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = DownloadProperties.PREFIX)
public class DownloadProperties {
    /**
     * 配置前缀。
     */
    public static final String PREFIX = "ingot.download";
    /**
     * 默认 bucket。
     */
    public static final String DEFAULT_BUCKET = "ingot-download";

    /**
     * 保存导出文件的 bucket，须预先创建。
     */
    private String bucket = DEFAULT_BUCKET;
    /**
     * 文件保留时长，从登记起算。
     */
    private Duration retention = Duration.ofDays(7);
    /**
     * 失败、过期任务记录保留时长，从登记起算。
     */
    private Duration recordRetention = Duration.ofDays(30);
    /**
     * 下载链接有效期。
     */
    private Duration linkTtl = Duration.ofMinutes(5);
    /**
     * 任务执行时限，超过仍未完成标为失败。
     */
    private Duration runTimeout = Duration.ofMinutes(30);
    /**
     * 清理间隔。
     */
    private Duration cleanupInterval = Duration.ofHours(1);
    /**
     * 每轮清理的到期文件上限。
     */
    private int cleanupBatchSize = 200;
    /**
     * 生成文件的工作线程数。
     */
    private int workerThreads = 2;
    /**
     * 等待执行的任务上限，超出时任务直接失败。
     */
    private int queueCapacity = 100;
}
