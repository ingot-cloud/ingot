package com.ingot.framework.tss.spring.handler;

import com.ingot.framework.tss.common.context.TaskContext;
import com.ingot.framework.tss.common.handler.TaskHandler;
import com.ingot.framework.tss.common.result.TaskResult;
import com.ingot.framework.tss.common.task.IScheduledTask;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Description  : IScheduledTask 接口适配器.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@RequiredArgsConstructor
public class IScheduledTaskHandler implements TaskHandler {
    
    private static final Logger log = LoggerFactory.getLogger(IScheduledTaskHandler.class);
    
    private final IScheduledTask scheduledTask;
    
    @Override
    public TaskResult execute(TaskContext context) {
        try {
            return scheduledTask.execute(context);
        } catch (Exception e) {
            log.error("IScheduledTask 执行失败", e);
            return TaskResult.failure("执行失败: " + e.getMessage(), e);
        }
    }
}
