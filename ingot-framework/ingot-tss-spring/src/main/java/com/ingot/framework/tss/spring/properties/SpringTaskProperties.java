package com.ingot.framework.tss.spring.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>Description  : Spring 任务调度配置属性.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@Data
@ConfigurationProperties(prefix = "ingot.tss.spring")
public class SpringTaskProperties {

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
