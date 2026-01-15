package com.ingot.framework.tss.xxljob.processor;

import com.ingot.framework.tss.common.annotation.ScheduledTask;
import com.ingot.framework.tss.common.model.TaskDefinition;
import com.ingot.framework.tss.common.registry.TaskRegistry;
import com.ingot.framework.tss.common.scheduler.TaskScheduler;
import com.ingot.framework.tss.spring.handler.MethodInvokerTaskHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Method;

/**
 * <p>Description  : 处理 @ScheduledTask 注解，注册到 XXL-Job.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@RequiredArgsConstructor
public class XxlJobTaskRegistrationProcessor implements BeanPostProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(XxlJobTaskRegistrationProcessor.class);
    
    private final TaskScheduler taskScheduler;
    private final TaskRegistry taskRegistry;
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Method[] methods = bean.getClass().getDeclaredMethods();
        
        for (Method method : methods) {
            ScheduledTask annotation = method.getAnnotation(ScheduledTask.class);
            if (annotation == null || !annotation.enabled()) {
                continue;
            }
            
            String taskName = annotation.name();
            
            // 注册任务处理器到注册中心
            MethodInvokerTaskHandler handler = new MethodInvokerTaskHandler(bean, method);
            taskRegistry.register(taskName, handler);
            
            // 注册到 XXL-Job（创建独立的 Handler）
            TaskDefinition definition = TaskDefinition.builder()
                .taskName(taskName)
                .description(annotation.description())
                .group(annotation.group())
                .enabled(annotation.enabled())
                .build();
            
            taskScheduler.registerTask(definition);
            
            log.info("注册 @ScheduledTask 到 XXL-Job: {} ({})", 
                taskName, annotation.description());
        }
        
        return bean;
    }
}
