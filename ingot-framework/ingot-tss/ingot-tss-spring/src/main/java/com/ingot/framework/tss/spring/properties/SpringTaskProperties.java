package com.ingot.framework.tss.spring.properties;

import java.time.ZoneId;

import com.ingot.framework.commons.constants.TimeConstants;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * <p>Description  : Spring 任务调度配置属性.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ingot.tss.spring")
public class SpringTaskProperties {

    /** Cron 业务时区，缺省 Asia/Shanghai；与数据库 UTC 无关，必须为有效的 ZoneId。 */
    @NotNull
    private ZoneId timeZone = TimeConstants.DEFAULT_BUSINESS_ZONE;

    /**
     * 线程池配置
     */
    private ThreadPool threadPool = new ThreadPool();

    /**
     * 执行历史最大保留数量
     */
    private int maxHistorySize = 1000;

    @Data
    public static class ThreadPool {
        /**
         * 线程池大小
         */
        private int size = 10;

        /**
         * 线程名称前缀
         */
        private String threadNamePrefix = "tss-task-";

        /**
         * 等待终止时间（秒）
         */
        private int awaitTerminationSeconds = 60;

        /**
         * 关闭时是否等待任务完成
         */
        private boolean waitForTasksToCompleteOnShutdown = true;
    }
}
