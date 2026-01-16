package com.ingot.cloud.auth.task;

import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import com.ingot.framework.tss.common.annotation.ScheduledTask;
import com.ingot.framework.tss.common.context.TaskContext;
import com.ingot.framework.tss.common.result.TaskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <p>Description  : OnlineTokenTask.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/16.</p>
 * <p>Time         : 14:17.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnlineTokenTask {
    private final OnlineTokenService onlineTokenService;

    @ScheduledTask(
            name = "CleanAllExpiredOnlineUsers",
            description = "清理所有过期用户",
            cron = "0 0 0/1 * * ?",  // 每小时执行一次
            group = "OnlineToken"
    )
    public TaskResult cleanAllExpiredOnlineUsers(TaskContext context) {
        log.info("[定时任务] - 开始清理过期在线用户");
        long count = onlineTokenService.cleanAllExpiredOnlineUsers();
        return TaskResult.success("Cleaned all expired online users: total=" + count);
    }

}
