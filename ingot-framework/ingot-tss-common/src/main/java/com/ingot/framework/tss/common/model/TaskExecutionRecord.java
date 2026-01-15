package com.ingot.framework.tss.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>Description  : 任务执行记录.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionRecord {
    
    /**
     * 记录ID
     */
    private String id;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 执行时间
     */
    private Long executeTime;
    
    /**
     * 执行耗时（毫秒）
     */
    private Long duration;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 执行结果消息
     */
    private String message;
    
    /**
     * 异常信息
     */
    private String exceptionMessage;
}
