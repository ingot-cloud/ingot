package com.ingot.framework.tss.xxljob.config;

import com.ingot.framework.tss.common.management.TaskManagement;
import com.ingot.framework.tss.common.registry.DefaultTaskRegistry;
import com.ingot.framework.tss.common.registry.TaskRegistry;
import com.ingot.framework.tss.common.scheduler.TaskScheduler;
import com.ingot.framework.tss.xxljob.management.XxlJobTaskManagement;
import com.ingot.framework.tss.xxljob.processor.IScheduledTaskProcessor;
import com.ingot.framework.tss.xxljob.processor.XxlJobTaskRegistrationProcessor;
import com.ingot.framework.tss.xxljob.properties.XxlJobTaskProperties;
import com.ingot.framework.tss.xxljob.scheduler.XxlJobTaskScheduler;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * <p>Description  : XXL-Job 任务调度自动配置.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(XxlJobTaskProperties.class)
@ConditionalOnProperty(name = "ingot.tss.type", havingValue = "xxljob")
public class XxlJobTaskAutoConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(XxlJobTaskAutoConfiguration.class);
    
    @Bean
    @ConditionalOnMissingBean(TaskRegistry.class)
    public TaskRegistry taskRegistry() {
        return new DefaultTaskRegistry();
    }
    
    @Bean
    @ConditionalOnMissingBean(XxlJobSpringExecutor.class)
    public XxlJobSpringExecutor xxlJobExecutor(XxlJobTaskProperties properties) {
        log.info("初始化 XXL-Job 执行器");
        
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        
        // Admin 配置
        executor.setAdminAddresses(properties.getAdmin().getAddresses());
        executor.setAccessToken(properties.getAdmin().getAccessToken());
        
        // 执行器配置
        executor.setAppname(properties.getExecutor().getAppName());
        executor.setAddress(properties.getExecutor().getAddress());
        executor.setIp(properties.getExecutor().getIp());
        executor.setPort(properties.getExecutor().getPort());
        executor.setLogPath(properties.getExecutor().getLogPath());
        executor.setLogRetentionDays(properties.getExecutor().getLogRetentionDays());
        
        return executor;
    }
    
    @Bean
    @ConditionalOnMissingBean(TaskScheduler.class)
    public TaskScheduler xxlJobTaskScheduler(TaskRegistry taskRegistry) {
        return new XxlJobTaskScheduler(taskRegistry);
    }
    
    @Bean
    @ConditionalOnMissingBean(TaskManagement.class)
    public TaskManagement xxlJobTaskManagement(TaskRegistry taskRegistry) {
        return new XxlJobTaskManagement(taskRegistry);
    }
    
    @Bean
    public XxlJobTaskRegistrationProcessor xxlJobTaskRegistrationProcessor(
            TaskScheduler taskScheduler,
            TaskRegistry taskRegistry) {
        return new XxlJobTaskRegistrationProcessor(taskScheduler, taskRegistry);
    }
    
    @Bean
    public IScheduledTaskProcessor iScheduledTaskProcessor(
            TaskScheduler taskScheduler,
            TaskRegistry taskRegistry) {
        return new IScheduledTaskProcessor(taskScheduler, taskRegistry);
    }
}
