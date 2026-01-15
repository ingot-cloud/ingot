package com.ingot.framework.tss.spring.handler;

import com.ingot.framework.tss.common.context.TaskContext;
import com.ingot.framework.tss.common.handler.TaskHandler;
import com.ingot.framework.tss.common.result.TaskResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * <p>Description  : 方法调用任务处理器.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@RequiredArgsConstructor
public class MethodInvokerTaskHandler implements TaskHandler {
    
    private static final Logger log = LoggerFactory.getLogger(MethodInvokerTaskHandler.class);
    
    private final Object targetBean;
    private final Method targetMethod;
    
    @Override
    public TaskResult execute(TaskContext context) {
        try {
            // 使方法可访问
            targetMethod.setAccessible(true);
            
            // 调用方法
            Object result;
            Class<?>[] paramTypes = targetMethod.getParameterTypes();
            
            if (paramTypes.length == 0) {
                // 无参方法
                result = targetMethod.invoke(targetBean);
            } else if (paramTypes.length == 1 && TaskContext.class.isAssignableFrom(paramTypes[0])) {
                // 有 TaskContext 参数
                result = targetMethod.invoke(targetBean, context);
            } else {
                log.warn("任务方法参数不支持: {}", targetMethod.getName());
                return TaskResult.failure("任务方法参数不支持");
            }
            
            // 处理返回值
            if (result instanceof TaskResult) {
                return (TaskResult) result;
            } else if (result == null || result instanceof Void) {
                return TaskResult.success("执行完成");
            } else {
                return TaskResult.success("执行完成", result);
            }
            
        } catch (Exception e) {
            log.error("任务方法执行失败: {}.{}", 
                targetBean.getClass().getSimpleName(), 
                targetMethod.getName(), e);
            return TaskResult.failure("执行失败: " + e.getMessage(), e);
        }
    }
}
