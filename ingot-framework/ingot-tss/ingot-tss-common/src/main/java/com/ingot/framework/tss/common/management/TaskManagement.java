package com.ingot.framework.tss.common.management;

import com.ingot.framework.tss.common.model.TaskExecutionRecord;
import com.ingot.framework.tss.common.model.TaskStatusInfo;
import com.ingot.framework.tss.common.result.TaskResult;

import java.util.List;

/**
 * <p>Description  : 任务管理接口.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
public interface TaskManagement {
    
    /**
     * 手动触发任务
     *
     * @param taskName 任务名称
     * @param params   任务参数
     * @return 任务执行结果
     */
    TaskResult triggerTask(String taskName, String params);
    
    /**
     * 停止任务
     *
     * @param taskName 任务名称
     * @return 是否成功
     */
    boolean stopTask(String taskName);
    
    /**
     * 暂停任务
     *
     * @param taskName 任务名称
     * @return 是否成功
     */
    boolean pauseTask(String taskName);
    
    /**
     * 恢复任务
     *
     * @param taskName 任务名称
     * @return 是否成功
     */
    boolean resumeTask(String taskName);
    
    /**
     * 更新任务 Cron 表达式
     *
     * @param taskName 任务名称
     * @param cron     新的 Cron 表达式
     * @return 是否成功
     */
    boolean updateTaskCron(String taskName, String cron);
    
    /**
     * 查询任务执行历史
     *
     * @param taskName 任务名称
     * @param page     页码（从1开始）
     * @param size     每页大小
     * @return 执行记录列表
     */
    List<TaskExecutionRecord> getExecutionHistory(String taskName, int page, int size);
    
    /**
     * 查询任务状态
     *
     * @param taskName 任务名称
     * @return 任务状态信息
     */
    TaskStatusInfo getTaskStatus(String taskName);
}
