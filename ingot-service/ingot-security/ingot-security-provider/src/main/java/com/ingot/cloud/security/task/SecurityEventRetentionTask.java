package com.ingot.cloud.security.task;

import com.ingot.cloud.security.service.SecurityEventRetentionService;
import com.ingot.framework.tss.common.annotation.ScheduledTask;
import com.ingot.framework.tss.common.context.TaskContext;
import com.ingot.framework.tss.common.result.TaskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 中心统一安全事件过期清理任务。
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityEventRetentionTask {

    private final SecurityEventRetentionService retentionService;

    @ScheduledTask(
            name = "PurgeSecurityEventTask",
            description = "清理过期 security_event 记录",
            cron = "0 30 3 * * ?", // 每天凌晨 3:30:00 执行
            group = "SecurityEvent"
    )
    public TaskResult purgeExpired(TaskContext context) {
        log.info("[定时任务] - 开始清理过期 security_event");
        int count = retentionService.purgeExpired();
        return TaskResult.success("purged security_event rows=" + count);
    }
}
