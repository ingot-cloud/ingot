package com.ingot.framework.tss.common.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>Description  : 任务执行结果.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResult {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 结果消息
     */
    private String message;
    
    /**
     * 返回数据
     */
    private Object data;
    
    /**
     * 执行耗时（毫秒）
     */
    private Long executeTime;
    
    /**
     * 异常信息
     */
    private Throwable throwable;
    
    /**
     * 创建成功结果
     */
    public static TaskResult success(String message) {
        return TaskResult.builder()
                .success(true)
                .message(message)
                .build();
    }
    
    /**
     * 创建成功结果（带数据）
     */
    public static TaskResult success(String message, Object data) {
        return TaskResult.builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static TaskResult failure(String message) {
        return TaskResult.builder()
                .success(false)
                .message(message)
                .build();
    }
    
    /**
     * 创建失败结果（带异常）
     */
    public static TaskResult failure(String message, Throwable throwable) {
        return TaskResult.builder()
                .success(false)
                .message(message)
                .throwable(throwable)
                .build();
    }
}
