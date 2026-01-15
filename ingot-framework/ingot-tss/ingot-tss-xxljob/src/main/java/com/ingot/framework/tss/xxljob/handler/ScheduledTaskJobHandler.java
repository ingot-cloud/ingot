package com.ingot.framework.tss.xxljob.handler;

import com.ingot.framework.tss.common.context.TaskContext;
import com.ingot.framework.tss.common.handler.TaskHandler;
import com.ingot.framework.tss.common.result.TaskResult;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.IJobHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Description  : 为每个 @ScheduledTask 创建独立的 XXL-Job Handler.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@RequiredArgsConstructor
public class ScheduledTaskJobHandler extends IJobHandler {
    
    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskJobHandler.class);
    
    private final TaskHandler taskHandler;
    private final String taskName;
    
    @Override
    public void execute() throws Exception {
        // 从 XXL-Job 上下文构建任务上下文
        TaskContext context = TaskContext.builder()
            .taskName(taskName)
            .params(XxlJobHelper.getJobParam())
            .scheduleTime(System.currentTimeMillis())
            .executeTime(System.currentTimeMillis())
            .shardIndex(XxlJobHelper.getShardIndex())
            .shardTotal(XxlJobHelper.getShardTotal())
            .build();
        
        long startTime = System.currentTimeMillis();
        try {
            // 执行任务
            TaskResult result = taskHandler.execute(context);
            long duration = System.currentTimeMillis() - startTime;
            
            // 返回结果给 XXL-Job
            if (result.isSuccess()) {
                String msg = String.format("%s (耗时: %dms)", 
                    result.getMessage(), duration);
                XxlJobHelper.handleSuccess(msg);
                
                log.info("XXL-Job 任务执行成功: {}, 耗时: {}ms", taskName, duration);
            } else {
                XxlJobHelper.handleFail(result.getMessage());
                
                log.error("XXL-Job 任务执行失败: {}, 耗时: {}ms, 错误: {}", 
                    taskName, duration, result.getMessage());
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String msg = String.format("执行异常: %s (耗时: %dms)", 
                e.getMessage(), duration);
            
            XxlJobHelper.handleFail(msg);
            
            log.error("XXL-Job 任务执行异常: {}, 耗时: {}ms", taskName, duration, e);
            throw e;
        }
    }
}
