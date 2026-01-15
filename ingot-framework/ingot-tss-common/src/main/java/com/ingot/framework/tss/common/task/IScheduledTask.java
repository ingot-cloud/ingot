package com.ingot.framework.tss.common.task;

import com.ingot.framework.tss.common.config.TaskConfig;
import com.ingot.framework.tss.common.context.TaskContext;
import com.ingot.framework.tss.common.result.TaskResult;

/**
 * <p>Description  : 定时任务接口（接口方式定义任务）.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
public interface IScheduledTask {
    
    /**
     * 执行任务
     *
     * @param context 任务上下文
     * @return 任务执行结果
     */
    TaskResult execute(TaskContext context);
    
    /**
     * 获取任务配置
     *
     * @return 任务配置
     */
    TaskConfig getConfig();
}
