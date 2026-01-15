package com.ingot.framework.tss.spring.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;

import com.ingot.framework.tss.common.context.TaskContext;
import com.ingot.framework.tss.common.enums.SchedulerType;
import com.ingot.framework.tss.common.handler.TaskHandler;
import com.ingot.framework.tss.common.model.TaskDefinition;
import com.ingot.framework.tss.common.model.TaskExecutionRecord;
import com.ingot.framework.tss.common.registry.TaskRegistry;
import com.ingot.framework.tss.common.result.TaskResult;
import com.ingot.framework.tss.common.scheduler.TaskScheduler;
import com.ingot.framework.tss.spring.properties.SpringTaskProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.support.CronTrigger;

/**
 * <p>Description  : Spring 任务调度器实现.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@RequiredArgsConstructor
public class SpringTaskScheduler implements TaskScheduler, DisposableBean {
    
    private static final Logger log = LoggerFactory.getLogger(SpringTaskScheduler.class);
    
    private final TaskRegistry taskRegistry;
    private final org.springframework.scheduling.TaskScheduler springScheduler;
    private final SpringTaskProperties properties;
    /**
     * 已调度的任务
     */
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    
    /**
     * 任务定义
     */
    private final Map<String, TaskDefinition> taskDefinitions = new ConcurrentHashMap<>();
    
    /**
     * 执行历史（内存存储，最多保留1000条）
     */
    private final ConcurrentLinkedQueue<TaskExecutionRecord> executionHistory = new ConcurrentLinkedQueue<>();

    @Override
    public void registerTask(TaskDefinition taskDefinition) {
        String taskName = taskDefinition.getTaskName();
        
        if (scheduledTasks.containsKey(taskName)) {
            log.warn("任务已存在，将被覆盖: {}", taskName);
            unregisterTask(taskName);
        }
        
        // 使用 Spring 的 TaskScheduler 调度任务
        CronTrigger trigger = new CronTrigger(taskDefinition.getCron());
        ScheduledFuture<?> future = springScheduler.schedule(
            () -> executeTask(taskDefinition),
            trigger
        );
        
        scheduledTasks.put(taskName, future);
        taskDefinitions.put(taskName, taskDefinition);
        
        log.info("注册 Spring 定时任务: {}, cron: {}", taskName, taskDefinition.getCron());
    }
    
    @Override
    public void unregisterTask(String taskName) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskName);
        taskDefinitions.remove(taskName);
        
        if (future != null) {
            future.cancel(false);
            log.info("取消 Spring 定时任务: {}", taskName);
        }
    }
    
    @Override
    public List<TaskDefinition> getAllTasks() {
        return new ArrayList<>(taskDefinitions.values());
    }
    
    @Override
    public SchedulerType getSchedulerType() {
        return SchedulerType.SPRING;
    }
    
    /**
     * 执行任务
     */
    private void executeTask(TaskDefinition taskDefinition) {
        String taskName = taskDefinition.getTaskName();
        TaskHandler handler = taskRegistry.getHandler(taskName);
        
        if (handler == null) {
            log.error("找不到任务处理器: {}", taskName);
            return;
        }
        
        // 构建任务上下文
        TaskContext context = TaskContext.builder()
            .taskName(taskName)
            .scheduleTime(System.currentTimeMillis())
            .executeTime(System.currentTimeMillis())
            .build();
        
        // 执行任务
        long startTime = System.currentTimeMillis();
        TaskResult result;
        
        try {
            result = handler.execute(context);
            long duration = System.currentTimeMillis() - startTime;
            
            // 记录执行历史
            recordExecution(taskName, result, duration);
            
            log.info("任务执行完成: {}, 耗时: {}ms, 结果: {}", 
                taskName, duration, result.getMessage());
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("任务执行失败: {}, 耗时: {}ms", taskName, duration, e);
            
            result = TaskResult.failure(e.getMessage(), e);
            recordExecution(taskName, result, duration);
        }
    }
    
    /**
     * 记录执行历史
     */
    public void recordExecution(String taskName, TaskResult result, long duration) {
        TaskExecutionRecord record = TaskExecutionRecord.builder()
            .id(UUID.randomUUID().toString())
            .taskName(taskName)
            .executeTime(System.currentTimeMillis())
            .duration(duration)
            .success(result.isSuccess())
            .message(result.getMessage())
            .exceptionMessage(result.getThrowable() != null ? 
                result.getThrowable().getMessage() : null)
            .build();
        
        executionHistory.offer(record);

        final int MAX_HISTORY_SIZE = properties.getMaxHistorySize();
        // 保持队列大小在限制内
        while (executionHistory.size() > MAX_HISTORY_SIZE) {
            executionHistory.poll();
        }
    }
    
    /**
     * 获取执行历史
     */
    public List<TaskExecutionRecord> getExecutionHistory(String taskName, int page, int size) {
        return executionHistory.stream()
            .filter(record -> record.getTaskName().equals(taskName))
            .skip((long) (page - 1) * size)
            .limit(size)
            .toList();
    }
    
    @Override
    public void destroy() {
        // 取消所有任务
        scheduledTasks.values().forEach(future -> future.cancel(false));
        scheduledTasks.clear();
        log.info("Spring TaskScheduler 已销毁");
    }
}
