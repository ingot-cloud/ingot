package com.ingot.framework.tss.common.registry;

import com.ingot.framework.tss.common.handler.TaskHandler;

import java.util.Set;

/**
 * <p>Description  : 任务注册中心接口.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
public interface TaskRegistry {
    
    /**
     * 注册任务处理器
     *
     * @param taskName 任务名称
     * @param handler  任务处理器
     */
    void register(String taskName, TaskHandler handler);
    
    /**
     * 获取任务处理器
     *
     * @param taskName 任务名称
     * @return 任务处理器，不存在则返回 null
     */
    TaskHandler getHandler(String taskName);
    
    /**
     * 移除任务处理器
     *
     * @param taskName 任务名称
     */
    void remove(String taskName);
    
    /**
     * 获取所有任务名称
     *
     * @return 任务名称集合
     */
    Set<String> getAllTaskNames();
    
    /**
     * 检查任务是否已注册
     *
     * @param taskName 任务名称
     * @return 是否已注册
     */
    boolean contains(String taskName);
}
