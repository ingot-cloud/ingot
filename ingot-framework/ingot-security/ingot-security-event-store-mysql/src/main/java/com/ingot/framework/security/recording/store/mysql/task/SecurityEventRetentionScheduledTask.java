package com.ingot.framework.security.recording.store.mysql.task;

import com.ingot.framework.security.recording.spi.SecurityEventRetentionHandler;
import com.ingot.framework.tss.common.annotation.ScheduledTask;
import com.ingot.framework.tss.common.context.TaskContext;
import com.ingot.framework.tss.common.result.TaskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>canonical {@code security_event} retention 定时任务，委托 {@link SecurityEventRetentionHandler}。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class SecurityEventRetentionScheduledTask {

    private final SecurityEventRetentionHandler retentionHandler;

    @ScheduledTask(
            name = "PurgeCanonicalSecurityEventTask",
            description = "清理过期 canonical security_event 记录",
            cron = "0 30 3 * * ?",
            group = "SecurityEvent")
    public TaskResult purgeExpired(TaskContext context) {
        log.info("[SecurityEventRetention] start canonical security_event purge");
        int count = retentionHandler.runRetentionRound();
        return TaskResult.success("purged canonical security_event rows=" + count);
    }
}
