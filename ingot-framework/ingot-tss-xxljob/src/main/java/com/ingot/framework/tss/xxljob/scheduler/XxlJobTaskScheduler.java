package com.ingot.framework.tss.xxljob.scheduler;

import com.ingot.framework.tss.common.enums.SchedulerType;
import com.ingot.framework.tss.common.handler.TaskHandler;
import com.ingot.framework.tss.common.model.TaskDefinition;
import com.ingot.framework.tss.common.registry.TaskRegistry;
import com.ingot.framework.tss.common.scheduler.TaskScheduler;
import com.ingot.framework.tss.xxljob.handler.ScheduledTaskJobHandler;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import com.xxl.job.core.handler.IJobHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Description  : XXL-Job 任务调度器实现.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@RequiredArgsConstructor
public class XxlJobTaskScheduler implements TaskScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(XxlJobTaskScheduler.class);
    
    private final TaskRegistry taskRegistry;
    
    /**
     * 任务定义
     */
    private final Map<String, TaskDefinition> taskDefinitions = new ConcurrentHashMap<>();
    
    @Override
    public void registerTask(TaskDefinition taskDefinition) {
        String taskName = taskDefinition.getTaskName();
        
        // 从注册中心获取任务处理器
        TaskHandler handler = taskRegistry.getHandler(taskName);
        if (handler == null) {
            log.warn("任务处理器不存在: {}", taskName);
            return;
        }
        
        // 保存任务定义
        taskDefinitions.put(taskName, taskDefinition);
        
        log.info("注册任务到 TaskRegistry: {} ({})", taskName, taskDefinition.getDescription());
        log.info("请在 XXL-Job Admin 中配置任务:");
        log.info("  - JobHandler: ingotTaskHandler");
        log.info("  - 任务参数: {}", taskName);
    }
    
    @Override
    public void unregisterTask(String taskName) {
        taskDefinitions.remove(taskName);
        
        // XXL-Job 不支持动态取消注册
        log.warn("XXL-Job 不支持动态取消注册，请在 Admin 后台停止任务: {}", taskName);
    }
    
    @Override
    public List<TaskDefinition> getAllTasks() {
        return new ArrayList<>(taskDefinitions.values());
    }
    
    @Override
    public SchedulerType getSchedulerType() {
        return SchedulerType.XXLJOB;
    }
}
