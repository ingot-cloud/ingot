package com.ingot.framework.tss.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>Description  : 任务定义.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDefinition {
    
    /**
     * 任务ID（内部使用）
     */
    private String taskId;
    
    /**
     * 任务名称（唯一标识）
     */
    private String taskName;
    
    /**
     * 任务描述
     */
    private String description;
    
    /**
     * Cron 表达式
     */
    private String cron;
    
    /**
     * 任务分组
     */
    private String group;
    
    /**
     * 是否启用
     */
    private boolean enabled;
}
