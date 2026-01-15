package com.ingot.framework.tss.common.annotation;

import java.lang.annotation.*;

/**
 * <p>Description  : 定时任务注解，支持 Spring 和 XXL-Job 两种调度器.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ScheduledTask {
    
    /**
     * 任务名称（唯一标识）
     * - Spring: 作为任务ID在调度器中注册
     * - XXL-Job: 作为 JobHandler 名称注册到执行器
     */
    String name();
    
    /**
     * 任务描述
     */
    String description() default "";
    
    /**
     * Cron 表达式（支持配置文件占位符，如：${task.cron}）
     * - Spring: 在代码或配置文件中配置
     * - XXL-Job: 在 Admin 后台配置（此处配置的是默认值，可不填）
     */
    String cron() default "";
    
    /**
     * 是否启用
     */
    boolean enabled() default true;
    
    /**
     * 任务分组
     */
    String group() default "default";
}
