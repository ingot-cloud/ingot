package com.ingot.cloud.iam.role;

import java.time.Duration;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>角色应用的本地调度配置；数据库租约固定30秒，配置不参与授权判断。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = RoleApplicationWorkerProperties.PREFIX)
public class RoleApplicationWorkerProperties {
    /** 调度配置根路径，保留既有worker-enabled键。 */
    public static final String PREFIX = "ingot.iam.role-applications";
    /** 领取开关键。 */
    public static final String WORKER_ENABLED = "worker-enabled";
    /** 注解所需的开启字面量。 */
    public static final String ENABLED_VALUE = "true";
    private static final Duration MIN_SCAN_INTERVAL = Duration.ofSeconds(10);
    private static final Duration MAX_BATCH_DELAY = Duration.ofSeconds(10);

    /** 是否接收及执行任务提示，默认true；关闭后仍可发送跨节点广播。 */
    private boolean workerEnabled = true;
    /** 正常扫描完成后的兜底间隔，默认60秒，至少10秒；首次扫描在此区间随机错峰。 */
    private Duration fallbackScanInterval = Duration.ofSeconds(60);
    /** 未完成任务再次排队的间隔，默认250毫秒，必须为正且小于租约三分之一（10秒）。 */
    private Duration activeBatchDelay = Duration.ofMillis(250);
    /** 本节点任务提示及通知发送队列容量，默认256，必须为正数。 */
    private int queueCapacity = 256;

    /** 非法配置阻止启动，不静默截断调度间隔。 */
    @PostConstruct
    public void validate() {
        if (fallbackScanInterval == null || fallbackScanInterval.compareTo(MIN_SCAN_INTERVAL) < 0) {
            throw new IllegalArgumentException("fallback-scan-interval must be at least 10s");
        }
        if (activeBatchDelay == null || activeBatchDelay.isZero() || activeBatchDelay.isNegative()
                || activeBatchDelay.compareTo(MAX_BATCH_DELAY) >= 0) {
            throw new IllegalArgumentException("active-batch-delay must be positive and less than 10s");
        }
        if (queueCapacity <= 0) throw new IllegalArgumentException("queue-capacity must be positive");
        fallbackScanInterval.toNanos(); // 确认调度器可表示配置值，溢出时启动失败。
    }
}
