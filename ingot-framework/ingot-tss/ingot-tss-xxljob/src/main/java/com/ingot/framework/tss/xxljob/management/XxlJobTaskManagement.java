package com.ingot.framework.tss.xxljob.management;

import com.ingot.framework.tss.common.context.TaskContext;
import com.ingot.framework.tss.common.enums.TaskStatus;
import com.ingot.framework.tss.common.handler.TaskHandler;
import com.ingot.framework.tss.common.management.TaskManagement;
import com.ingot.framework.tss.common.model.TaskExecutionRecord;
import com.ingot.framework.tss.common.model.TaskStatusInfo;
import com.ingot.framework.tss.common.registry.TaskRegistry;
import com.ingot.framework.tss.common.result.TaskResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * <p>Description  : XXL-Job 任务管理实现.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@RequiredArgsConstructor
public class XxlJobTaskManagement implements TaskManagement {
    
    private static final Logger log = LoggerFactory.getLogger(XxlJobTaskManagement.class);
    
    private final TaskRegistry taskRegistry;
    
    @Override
    public TaskResult triggerTask(String taskName, String params) {
        // 本地触发：直接调用 Handler
        TaskHandler handler = taskRegistry.getHandler(taskName);
        if (handler == null) {
            return TaskResult.failure("任务不存在: " + taskName);
        }
        
        TaskContext context = TaskContext.builder()
            .taskName(taskName)
            .params(params)
            .executeTime(System.currentTimeMillis())
            .build();
        
        long startTime = System.currentTimeMillis();
        try {
            TaskResult result = handler.execute(context);
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("手动触发 XXL-Job 任务: {}, 耗时: {}ms", taskName, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("手动触发 XXL-Job 任务失败: {}, 耗时: {}ms", taskName, duration, e);
            return TaskResult.failure("执行失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean stopTask(String taskName) {
        log.warn("请在 XXL-Job Admin 后台停止任务: {}", taskName);
        return false;
    }
    
    @Override
    public boolean pauseTask(String taskName) {
        log.warn("请在 XXL-Job Admin 后台暂停任务: {}", taskName);
        return false;
    }
    
    @Override
    public boolean resumeTask(String taskName) {
        log.warn("请在 XXL-Job Admin 后台恢复任务: {}", taskName);
        return false;
    }
    
    @Override
    public boolean updateTaskCron(String taskName, String cron) {
        log.warn("请在 XXL-Job Admin 后台修改 Cron 表达式: {}", taskName);
        return false;
    }
    
    @Override
    public List<TaskExecutionRecord> getExecutionHistory(String taskName, int page, int size) {
        log.info("请在 XXL-Job Admin 后台查看执行历史: {}", taskName);
        return Collections.emptyList();
    }
    
    @Override
    public TaskStatusInfo getTaskStatus(String taskName) {
        boolean exists = taskRegistry.contains(taskName);
        
        return TaskStatusInfo.builder()
            .taskName(taskName)
            .status(exists ? TaskStatus.RUNNING : TaskStatus.UNKNOWN)
            .description(exists ? "已注册到 XXL-Job，请在 Admin 后台查看详细状态" : "未注册")
            .build();
    }
}
