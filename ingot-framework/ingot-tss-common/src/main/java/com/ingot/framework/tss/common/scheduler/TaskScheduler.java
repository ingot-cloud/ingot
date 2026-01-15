package com.ingot.framework.tss.common.scheduler;

import com.ingot.framework.tss.common.enums.SchedulerType;
import com.ingot.framework.tss.common.model.TaskDefinition;

import java.util.List;

/**
 * <p>Description  : 任务调度器接口.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
public interface TaskScheduler {
    
    /**
     * 注册任务
     *
     * @param taskDefinition 任务定义
     */
    void registerTask(TaskDefinition taskDefinition);
    
    /**
     * 取消注册任务
     *
     * @param taskName 任务名称
     */
    void unregisterTask(String taskName);
    
    /**
     * 获取所有已注册任务
     *
     * @return 任务定义列表
     */
    List<TaskDefinition> getAllTasks();
    
    /**
     * 获取调度器类型
     *
     * @return 调度器类型
     */
    SchedulerType getSchedulerType();
}
