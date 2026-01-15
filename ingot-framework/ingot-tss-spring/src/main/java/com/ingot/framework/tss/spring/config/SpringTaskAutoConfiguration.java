package com.ingot.framework.tss.spring.config;

import com.ingot.framework.tss.common.management.TaskManagement;
import com.ingot.framework.tss.common.registry.DefaultTaskRegistry;
import com.ingot.framework.tss.common.registry.TaskRegistry;
import com.ingot.framework.tss.common.scheduler.TaskScheduler;
import com.ingot.framework.tss.spring.management.SpringTaskManagement;
import com.ingot.framework.tss.spring.processor.IScheduledTaskProcessor;
import com.ingot.framework.tss.spring.processor.ScheduledTaskAnnotationProcessor;
import com.ingot.framework.tss.spring.properties.SpringTaskProperties;
import com.ingot.framework.tss.spring.scheduler.SpringTaskScheduler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * <p>Description  : Spring 任务调度自动配置.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties(SpringTaskProperties.class)
@ConditionalOnProperty(name = "ingot.tss.type", havingValue = "spring", matchIfMissing = true)
public class SpringTaskAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TaskRegistry.class)
    public TaskRegistry taskRegistry() {
        return new DefaultTaskRegistry();
    }

    @Bean
    @ConditionalOnMissingBean(org.springframework.scheduling.TaskScheduler.class)
    public org.springframework.scheduling.TaskScheduler springScheduler(SpringTaskProperties properties) {
        SpringTaskProperties.ThreadPool pool = properties.getThreadPool();

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(pool.getSize());
        scheduler.setThreadNamePrefix(pool.getThreadNamePrefix());
        scheduler.setAwaitTerminationSeconds(pool.getAwaitTerminationSeconds());
        scheduler.setWaitForTasksToCompleteOnShutdown(pool.isWaitForTasksToCompleteOnShutdown());
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    @ConditionalOnMissingBean(TaskScheduler.class)
    public TaskScheduler springTaskScheduler(TaskRegistry taskRegistry,
                                             org.springframework.scheduling.TaskScheduler springScheduler,
                                             SpringTaskProperties properties) {
        return new SpringTaskScheduler(taskRegistry, springScheduler, properties);
    }

    @Bean
    @ConditionalOnMissingBean(TaskManagement.class)
    public TaskManagement springTaskManagement(TaskScheduler taskScheduler,
                                               TaskRegistry taskRegistry) {
        if (taskScheduler instanceof SpringTaskScheduler) {
            return new SpringTaskManagement((SpringTaskScheduler) taskScheduler, taskRegistry);
        }
        throw new IllegalStateException("TaskScheduler must be SpringTaskScheduler");
    }

    @Bean
    public ScheduledTaskAnnotationProcessor scheduledTaskAnnotationProcessor(
            TaskScheduler taskScheduler,
            TaskRegistry taskRegistry,
            Environment environment) {
        return new ScheduledTaskAnnotationProcessor(taskScheduler, taskRegistry, environment);
    }

    @Bean
    public IScheduledTaskProcessor iScheduledTaskProcessor(
            TaskScheduler taskScheduler,
            TaskRegistry taskRegistry) {
        return new IScheduledTaskProcessor(taskScheduler, taskRegistry);
    }
}
