package com.ingot.framework.tss.common.registry;

import com.ingot.framework.tss.common.handler.TaskHandler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * <p>Description  : 默认任务注册中心实现.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
public class DefaultTaskRegistry implements TaskRegistry {
    
    private static final Logger log = Logger.getLogger(DefaultTaskRegistry.class.getName());
    
    private final ConcurrentMap<String, TaskHandler> handlerMap = new ConcurrentHashMap<>();
    
    @Override
    public void register(String taskName, TaskHandler handler) {
        if (taskName == null || taskName.trim().isEmpty()) {
            throw new IllegalArgumentException("任务名称不能为空");
        }
        if (handler == null) {
            throw new IllegalArgumentException("任务处理器不能为空");
        }
        
        TaskHandler existingHandler = handlerMap.putIfAbsent(taskName, handler);
        if (existingHandler != null) {
            log.warning("任务已存在，将被覆盖: " + taskName);
            handlerMap.put(taskName, handler);
        }
        
        log.info("注册任务处理器: " + taskName);
    }
    
    @Override
    public TaskHandler getHandler(String taskName) {
        return handlerMap.get(taskName);
    }
    
    @Override
    public void remove(String taskName) {
        TaskHandler removed = handlerMap.remove(taskName);
        if (removed != null) {
            log.info("移除任务处理器: " + taskName);
        }
    }
    
    @Override
    public Set<String> getAllTaskNames() {
        return handlerMap.keySet();
    }
    
    @Override
    public boolean contains(String taskName) {
        return handlerMap.containsKey(taskName);
    }
}
