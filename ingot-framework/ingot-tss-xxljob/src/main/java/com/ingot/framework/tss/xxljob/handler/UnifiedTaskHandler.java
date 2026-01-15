package com.ingot.framework.tss.xxljob.handler;

import com.ingot.framework.tss.common.context.TaskContext;
import com.ingot.framework.tss.common.handler.TaskHandler;
import com.ingot.framework.tss.common.registry.TaskRegistry;
import com.ingot.framework.tss.common.result.TaskResult;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * <p>Description  : XXL-Job 统一任务处理器.</p>
 * <p>说明：此 Handler 提供统一入口，通过任务名称路由到具体的任务处理器</p>
 * <p>使用方式：在 XXL-Job Admin 配置任务时</p>
 * <p>  - JobHandler: ingotTaskHandler</p>
 * <p>  - 任务参数: {"taskName": "your-task-name", "params": "your-params"}</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@Component
@RequiredArgsConstructor
public class UnifiedTaskHandler {
    
    private static final Logger log = LoggerFactory.getLogger(UnifiedTaskHandler.class);
    
    private final TaskRegistry taskRegistry;
    
    /**
     * 统一任务处理入口
     * 通过任务名称路由到具体的任务处理器
     */
    @XxlJob("ingotTaskHandler")
    public void execute() {
        String fullParam = XxlJobHelper.getJobParam();
        
        try {
            // 解析参数获取任务名称
            // 支持两种格式：
            // 1. 直接传任务名：task-name
            // 2. JSON格式：{"taskName": "task-name", "params": "..."}
            String taskName;
            String params = null;
            
            if (fullParam != null && fullParam.trim().startsWith("{")) {
                // JSON 格式
                // 简单解析（生产环境建议使用 JSON 库）
                int taskNameStart = fullParam.indexOf("\"taskName\"");
                if (taskNameStart != -1) {
                    int valueStart = fullParam.indexOf(":", taskNameStart) + 1;
                    int valueEnd = fullParam.indexOf(",", valueStart);
                    if (valueEnd == -1) {
                        valueEnd = fullParam.indexOf("}", valueStart);
                    }
                    taskName = fullParam.substring(valueStart, valueEnd)
                        .trim()
                        .replace("\"", "")
                        .trim();
                    
                    // 提取 params
                    int paramsStart = fullParam.indexOf("\"params\"");
                    if (paramsStart != -1) {
                        int paramsValueStart = fullParam.indexOf(":", paramsStart) + 1;
                        int paramsValueEnd = fullParam.indexOf("}", paramsValueStart);
                        params = fullParam.substring(paramsValueStart, paramsValueEnd)
                            .trim()
                            .replace("\"", "")
                            .trim();
                    }
                } else {
                    XxlJobHelper.handleFail("无效的参数格式，需要包含 taskName");
                    return;
                }
            } else {
                // 直接传任务名
                taskName = fullParam;
            }
            
            // 从注册中心获取任务处理器
            TaskHandler handler = taskRegistry.getHandler(taskName);
            if (handler == null) {
                XxlJobHelper.handleFail("任务不存在: " + taskName);
                return;
            }
            
            // 构建任务上下文
            TaskContext context = TaskContext.builder()
                .taskName(taskName)
                .params(params)
                .scheduleTime(System.currentTimeMillis())
                .executeTime(System.currentTimeMillis())
                .shardIndex(XxlJobHelper.getShardIndex())
                .shardTotal(XxlJobHelper.getShardTotal())
                .build();
            
            long startTime = System.currentTimeMillis();
            
            // 执行任务
            TaskResult result = handler.execute(context);
            long duration = System.currentTimeMillis() - startTime;
            
            // 返回结果
            if (result.isSuccess()) {
                String msg = String.format("[%s] %s (耗时: %dms)", 
                    taskName, result.getMessage(), duration);
                XxlJobHelper.handleSuccess(msg);
                log.info("任务执行成功: {}, 耗时: {}ms", taskName, duration);
            } else {
                String msg = String.format("[%s] %s", taskName, result.getMessage());
                XxlJobHelper.handleFail(msg);
                log.error("任务执行失败: {}, 错误: {}", taskName, result.getMessage());
            }
            
        } catch (Exception e) {
            log.error("任务执行异常", e);
            XxlJobHelper.handleFail("执行异常: " + e.getMessage());
        }
    }
}
