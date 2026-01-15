package com.ingot.framework.tss.common.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * <p>Description  : 任务执行上下文.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskContext {
    
    /**
     * 任务ID（内部使用）
     */
    private String taskId;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 任务参数（JSON格式）
     */
    private String params;
    
    /**
     * 调度时间（时间戳）
     */
    private Long scheduleTime;
    
    /**
     * 执行时间（时间戳）
     */
    private Long executeTime;
    
    /**
     * 分片索引（从0开始）
     */
    private Integer shardIndex;
    
    /**
     * 分片总数
     */
    private Integer shardTotal;
    
    /**
     * 扩展数据
     */
    private Map<String, Object> extendedData;
    
    /**
     * 是否是分片任务
     */
    public boolean isSharding() {
        return shardTotal != null && shardTotal > 1;
    }
}
