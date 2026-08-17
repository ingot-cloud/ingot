package com.ingot.framework.security.recording.store.log.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>结构化文件日志 Store 配置。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "ingot.security.event.log")
public class LogStoreProperties {

    private boolean enabled = true;

    private String directory = "./logs/security-recording/events";

    private String segmentBytes = "64MB";

    private String totalSizeCap = "2GB";

    private int retentionDays = 30;
}
