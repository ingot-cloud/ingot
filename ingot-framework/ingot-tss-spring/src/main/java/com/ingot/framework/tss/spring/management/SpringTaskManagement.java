package com.ingot.framework.tss.spring.management;

import com.ingot.framework.tss.common.context.TaskContext;
import com.ingot.framework.tss.common.enums.TaskStatus;
import com.ingot.framework.tss.common.handler.TaskHandler;
import com.ingot.framework.tss.common.management.TaskManagement;
import com.ingot.framework.tss.common.model.TaskDefinition;
import com.ingot.framework.tss.common.model.TaskExecutionRecord;
import com.ingot.framework.tss.common.model.TaskStatusInfo;
import com.ingot.framework.tss.common.registry.TaskRegistry;
import com.ingot.framework.tss.common.result.TaskResult;
import com.ingot.framework.tss.spring.scheduler.SpringTaskScheduler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * <p>Description  : Spring 任务管理实现.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@RequiredArgsConstructor
public class SpringTaskManagement implements TaskManagement {
    
    private static final Logger log = LoggerFactory.getLogger(SpringTaskManagement.class);
    
    private final SpringTaskScheduler taskScheduler;
    private final TaskRegistry taskRegistry;
    
    @Override
    public TaskResult triggerTask(String taskName, String params) {
        TaskHandler handler = taskRegistry.getHandler(taskName);
        if (handler == null) {
            return TaskResult.failure("任务不存在: " + taskName);
        }
        
        // 构建上下文
        TaskContext context = TaskContext.builder()
            .taskName(taskName)
            .params(params)
            .executeTime(System.currentTimeMillis())
            .build();
        
        // 手动执行
        long startTime = System.currentTimeMillis();
        try {
            TaskResult result = handler.execute(context);
            long duration = System.currentTimeMillis() - startTime;
            
            // 记录执行历史
            taskScheduler.recordExecution(taskName, result, duration);
            
            log.info("手动触发任务: {}, 耗时: {}ms", taskName, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("手动触发任务失败: {}, 耗时: {}ms", taskName, duration, e);
            
            TaskResult result = TaskResult.failure("执行失败: " + e.getMessage(), e);
            taskScheduler.recordExecution(taskName, result, duration);
            return result;
        }
    }
    
    @Override
    public boolean stopTask(String taskName) {
        taskScheduler.unregisterTask(taskName);
        return true;
    }
    
    @Override
    public boolean pauseTask(String taskName) {
        taskScheduler.unregisterTask(taskName);
        log.info("暂停任务: {}", taskName);
        return true;
    }
    
    @Override
    public boolean resumeTask(String taskName) {
        // 从任务定义中重新注册
        List<TaskDefinition> allTasks = taskScheduler.getAllTasks();
        TaskDefinition taskDef = allTasks.stream()
            .filter(t -> t.getTaskName().equals(taskName))
            .findFirst()
            .orElse(null);
        
        if (taskDef != null) {
            taskScheduler.registerTask(taskDef);
            log.info("恢复任务: {}", taskName);
            return true;
        }
        
        log.warn("恢复任务失败，任务定义不存在: {}", taskName);
        return false;
    }
    
    @Override
    public boolean updateTaskCron(String taskName, String cron) {
        // 先获取任务定义
        List<TaskDefinition> allTasks = taskScheduler.getAllTasks();
        TaskDefinition taskDef = allTasks.stream()
            .filter(t -> t.getTaskName().equals(taskName))
            .findFirst()
            .orElse(null);
        
        if (taskDef == null) {
            log.warn("更新Cron失败，任务不存在: {}", taskName);
            return false;
        }
        
        // 取消旧任务
        taskScheduler.unregisterTask(taskName);
        
        // 重新注册新任务
        TaskDefinition newDefinition = TaskDefinition.builder()
            .taskId(taskDef.getTaskId())
            .taskName(taskName)
            .description(taskDef.getDescription())
            .cron(cron)
            .group(taskDef.getGroup())
            .enabled(taskDef.isEnabled())
            .build();
        
        taskScheduler.registerTask(newDefinition);
        
        log.info("更新任务Cron: {}, 新Cron: {}", taskName, cron);
        return true;
    }
    
    @Override
    public List<TaskExecutionRecord> getExecutionHistory(String taskName, int page, int size) {
        return taskScheduler.getExecutionHistory(taskName, page, size);
    }
    
    @Override
    public TaskStatusInfo getTaskStatus(String taskName) {
        List<TaskDefinition> allTasks = taskScheduler.getAllTasks();
        boolean exists = allTasks.stream()
            .anyMatch(t -> t.getTaskName().equals(taskName));
        
        TaskStatus status = exists ? TaskStatus.RUNNING : TaskStatus.STOPPED;
        
        return TaskStatusInfo.builder()
            .taskName(taskName)
            .status(status)
            .description(status == TaskStatus.RUNNING ? "运行中" : "已停止")
            .build();
    }
}
