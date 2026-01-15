package com.ingot.framework.tss.common.model;

import com.ingot.framework.tss.common.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>Description  : 任务状态信息.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusInfo {
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 任务状态
     */
    private TaskStatus status;
    
    /**
     * 状态描述
     */
    private String description;
    
    /**
     * 上次执行时间
     */
    private Long lastExecuteTime;
    
    /**
     * 下次执行时间
     */
    private Long nextExecuteTime;
}
