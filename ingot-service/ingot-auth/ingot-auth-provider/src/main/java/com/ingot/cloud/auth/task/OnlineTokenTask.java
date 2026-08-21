package com.ingot.cloud.auth.task;

import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import com.ingot.framework.tss.common.annotation.ScheduledTask;
import com.ingot.framework.tss.common.context.TaskContext;
import com.ingot.framework.tss.common.result.TaskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <p>在线会话巡检任务：清理在线用户排序集中已过期的条目。</p>
 *
 * <p>会话主数据依赖 Redis TTL 自然过期，但在线用户 ZSet 无法按成员过期，需要定期按 score 收敛，
 * 否则在线数统计会持续虚高。过期成员对应的 {@code token:user:set} 一并删除。</p>
 *
 * @author jy
 * @since 1.0.0
 * @implNote 清理范围由在线用户注册表枚举，不扫描 key 空间。
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
