package com.ingot.framework.tss.common.enums;

/**
 * <p>Description  : 任务状态.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
public enum TaskStatus {
    /**
     * 运行中
     */
    RUNNING,
    
    /**
     * 已暂停
     */
    PAUSED,
    
    /**
     * 已停止
     */
    STOPPED,
    
    /**
     * 未知状态
     */
    UNKNOWN
}
