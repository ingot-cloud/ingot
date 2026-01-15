package com.ingot.framework.tss.common.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>Description  : 任务配置.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskConfig {
    
    /**
     * 任务名称
     */
    private String name;
    
    /**
     * 任务描述
     */
    private String description;
    
    /**
     * Cron 表达式
     */
    private String cron;
    
    /**
     * 是否启用
     */
    private boolean enabled;
    
    /**
     * 任务分组
     */
    private String group;
}
