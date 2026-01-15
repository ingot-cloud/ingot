package com.ingot.framework.tss.common.handler;

import com.ingot.framework.tss.common.context.TaskContext;
import com.ingot.framework.tss.common.result.TaskResult;

/**
 * <p>Description  : 任务处理器接口.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
public interface TaskHandler {
    
    /**
     * 执行任务
     *
     * @param context 任务上下文
     * @return 任务执行结果
     */
    TaskResult execute(TaskContext context);
}
