package com.ingot.framework.tss.spring.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>Description  : SpringTaskProperties.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/14.</p>
 * <p>Time         : 18:02.</p>
 */
@Data
@ConfigurationProperties(prefix = "ingot.tss.spring")
public class SpringTaskProperties {
    /**
     * 线程池配置
     */
    private ThreadPool threadPool;
    /**
     * 最大历史记录条数
     */
    private int maxHistorySize = 100;

    @Data
    public static class ThreadPool {
        /**
         * 线程池大小
         */
        private int size = 10;
        /**
         * 执行器前缀
         */
        private String threadNamePrefix = "tss-task-";
        /**
         * 设置此执行器在关闭时应该阻塞的最大秒数
         */
        private int awaitTerminationSeconds = 60;
        /**
         * 设置是否在关机时等待计划任务完成，不中断正在运行的任务，并执行队列中的所有任务。
         */
        private boolean waitForTasksToCompleteOnShutdown = true;
    }
}
