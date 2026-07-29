package com.ingot.framework.security.account.adapter.task;

import com.ingot.framework.security.account.domain.port.inbound.UnlockAccountUseCase;
import com.ingot.framework.tss.common.annotation.ScheduledTask;
import com.ingot.framework.tss.common.context.TaskContext;
import com.ingot.framework.tss.common.result.TaskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AccountLockTask
 *
 * @author jy
 * @since 2026/3/18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountLockTask {
    private final UnlockAccountUseCase unlockAccountUseCase;

    @ScheduledTask(
            name = "UnlockAccountTask",
            description = "自动解锁过期的临时锁定",
            cron = "0 0/1 * * * ?",  // 每分钟执行一次
            group = "AccountState"
    )
    public TaskResult unlockExpired(TaskContext context) {
        log.info("[定时任务] - 开始自动解锁过期的临时锁定");
        unlockAccountUseCase.unlockExpired();
        return TaskResult.success("自动解锁过期的临时锁定完成");
    }

}
