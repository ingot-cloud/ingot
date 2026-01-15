package com.ingot.framework.tss.xxljob.processor;

import com.ingot.framework.tss.common.model.TaskDefinition;
import com.ingot.framework.tss.common.registry.TaskRegistry;
import com.ingot.framework.tss.common.scheduler.TaskScheduler;
import com.ingot.framework.tss.common.task.IScheduledTask;
import com.ingot.framework.tss.spring.handler.IScheduledTaskHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * <p>Description  : 处理 IScheduledTask 接口实现，注册到 XXL-Job.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@RequiredArgsConstructor
public class IScheduledTaskProcessor implements BeanPostProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(IScheduledTaskProcessor.class);
    
    private final TaskScheduler taskScheduler;
    private final TaskRegistry taskRegistry;
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof IScheduledTask) {
            IScheduledTask scheduledTask = (IScheduledTask) bean;
            
            // 获取任务配置
            var config = scheduledTask.getConfig();
            if (config == null || !config.isEnabled()) {
                return bean;
            }
            
            String taskName = config.getName();
            
            // 注册任务处理器
            IScheduledTaskHandler handler = new IScheduledTaskHandler(scheduledTask);
            taskRegistry.register(taskName, handler);
            
            // 注册到 XXL-Job
            TaskDefinition definition = TaskDefinition.builder()
                .taskName(taskName)
                .description(config.getDescription())
                .group(config.getGroup())
                .enabled(config.isEnabled())
                .build();
            
            taskScheduler.registerTask(definition);
            
            log.info("注册 IScheduledTask 到 XXL-Job: {} ({})", 
                taskName, config.getDescription());
        }
        
        return bean;
    }
}
