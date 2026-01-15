package com.ingot.framework.tss.spring.processor;

import com.ingot.framework.tss.common.annotation.ScheduledTask;
import com.ingot.framework.tss.common.handler.TaskHandler;
import com.ingot.framework.tss.common.model.TaskDefinition;
import com.ingot.framework.tss.common.registry.TaskRegistry;
import com.ingot.framework.tss.common.scheduler.TaskScheduler;
import com.ingot.framework.tss.spring.handler.MethodInvokerTaskHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.Environment;

import java.lang.reflect.Method;

/**
 * <p>Description  : 处理 @ScheduledTask 注解的处理器.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@RequiredArgsConstructor
public class ScheduledTaskAnnotationProcessor implements BeanPostProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskAnnotationProcessor.class);
    
    private final TaskScheduler taskScheduler;
    private final TaskRegistry taskRegistry;
    private final Environment environment;
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Method[] methods = bean.getClass().getDeclaredMethods();
        
        for (Method method : methods) {
            ScheduledTask annotation = method.getAnnotation(ScheduledTask.class);
            if (annotation == null || !annotation.enabled()) {
                continue;
            }
            
            String taskName = annotation.name();
            String cron = resolveCron(annotation.cron());
            
            // 如果没有配置 cron，跳过（可能在 XXL-Job Admin 中配置）
            if (cron == null || cron.trim().isEmpty()) {
                log.debug("任务 {} 没有配置 Cron 表达式，跳过注册", taskName);
                continue;
            }
            
            // 注册任务处理器
            TaskHandler handler = new MethodInvokerTaskHandler(bean, method);
            taskRegistry.register(taskName, handler);
            
            // 注册到调度器
            TaskDefinition definition = TaskDefinition.builder()
                .taskName(taskName)
                .description(annotation.description())
                .cron(cron)
                .group(annotation.group())
                .enabled(annotation.enabled())
                .build();
            
            taskScheduler.registerTask(definition);
            
            log.info("注册 @ScheduledTask: {} ({}), cron: {}", 
                taskName, annotation.description(), cron);
        }
        
        return bean;
    }
    
    /**
     * 解析 Cron 表达式（支持配置文件占位符）
     */
    private String resolveCron(String cron) {
        if (cron == null || cron.trim().isEmpty()) {
            return cron;
        }
        
        try {
            return environment.resolveRequiredPlaceholders(cron);
        } catch (Exception e) {
            log.warn("解析 Cron 表达式失败: {}, 使用原始值", cron);
            return cron;
        }
    }
}
