package com.ingot.framework.security.account.adapter.task;

import com.ingot.framework.security.account.adapter.service.AccountSecurityEventRetentionService;
import com.ingot.framework.tss.common.annotation.ScheduledTask;
import com.ingot.framework.tss.common.context.TaskContext;
import com.ingot.framework.tss.common.result.TaskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 本地账号安全事件过期清理任务。
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountSecurityEventRetentionTask {

    private final AccountSecurityEventRetentionService retentionService;

    @ScheduledTask(
            name = "PurgeAccountSecurityEventTask",
            description = "清理过期 account_security_event 记录",
            cron = "0 0 3 * * ?", // 每天凌晨 3:00:00 执行
            group = "SecurityEvent"
    )
    public TaskResult purgeExpired(TaskContext context) {
        log.info("[定时任务] - 开始清理过期 account_security_event");
        int count = retentionService.purgeExpired();
        return TaskResult.success("purged account_security_event rows=" + count);
    }
}
