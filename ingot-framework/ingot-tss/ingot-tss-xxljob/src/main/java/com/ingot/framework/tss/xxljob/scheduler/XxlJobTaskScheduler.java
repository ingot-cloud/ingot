package com.ingot.framework.tss.xxljob.scheduler;

import com.ingot.framework.tss.common.enums.SchedulerType;
import com.ingot.framework.tss.common.handler.TaskHandler;
import com.ingot.framework.tss.common.model.TaskDefinition;
import com.ingot.framework.tss.common.registry.TaskRegistry;
import com.ingot.framework.tss.common.scheduler.TaskScheduler;
import com.ingot.framework.tss.xxljob.handler.ScheduledTaskJobHandler;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.handler.IJobHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <p>Description  : XXL-Job 任务调度器实现.</p>
 * <p>说明：每个 @ScheduledTask 任务会注册为独立的 XXL-Job Handler</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@RequiredArgsConstructor
public class XxlJobTaskScheduler implements TaskScheduler, InitializingBean {
    
    private static final Logger log = LoggerFactory.getLogger(XxlJobTaskScheduler.class);
    
    private final TaskRegistry taskRegistry;
    
    /**
     * 已注册的 Handler
     */
    private final Map<String, IJobHandler> registeredHandlers = new ConcurrentHashMap<>();
    
    /**
     * 任务定义
     */
    private final Map<String, TaskDefinition> taskDefinitions = new ConcurrentHashMap<>();
    
    /**
     * XXL-Job 的 Handler 仓库（通过反射获取）
     */
    private ConcurrentMap<String, IJobHandler> jobHandlerRepository;
    
    @Override
    public void afterPropertiesSet() throws Exception {
        // 通过反射获取 XxlJobExecutor 的 jobHandlerRepository
        try {
            Field field = XxlJobExecutor.class.getDeclaredField("jobHandlerRepository");
            field.setAccessible(true);
            jobHandlerRepository = (ConcurrentMap<String, IJobHandler>) field.get(null);
            log.info("成功获取 XXL-Job Handler 仓库，支持动态注册");
        } catch (Exception e) {
            log.warn("无法获取 XXL-Job Handler 仓库，将无法动态注册 Handler: {}", e.getMessage());
        }
    }
    
    @Override
    public void registerTask(TaskDefinition taskDefinition) {
        String taskName = taskDefinition.getTaskName();
        
        // 从注册中心获取任务处理器
        TaskHandler handler = taskRegistry.getHandler(taskName);
        if (handler == null) {
            log.warn("任务处理器不存在: {}", taskName);
            return;
        }
        
        // 为每个任务创建独立的 XXL-Job Handler
        IJobHandler jobHandler = new ScheduledTaskJobHandler(handler, taskName);
        
        // 动态注册到 XXL-Job（每个任务独立的 Handler）
        if (jobHandlerRepository != null) {
            IJobHandler existing = jobHandlerRepository.putIfAbsent(taskName, jobHandler);
            if (existing != null) {
                log.warn("Handler 已存在，将被覆盖: {}", taskName);
                jobHandlerRepository.put(taskName, jobHandler);
            }
        } else {
            log.warn("无法动态注册 Handler: {}，请使用原生 @XxlJob 注解", taskName);
        }
        
        registeredHandlers.put(taskName, jobHandler);
        taskDefinitions.put(taskName, taskDefinition);
        
        log.info("注册 XXL-Job Handler: {} ({})", taskName, taskDefinition.getDescription());
        log.info("请在 XXL-Job Admin 中配置任务:");
        log.info("  - JobHandler: {}", taskName);
        log.info("  - 任务参数: 可选参数（通过 context.getParams() 获取）");
        log.info("  - Cron: 在 Admin 中配置");
        log.info("  - 运行模式: BEAN");
    }
    
    @Override
    public void unregisterTask(String taskName) {
        registeredHandlers.remove(taskName);
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
