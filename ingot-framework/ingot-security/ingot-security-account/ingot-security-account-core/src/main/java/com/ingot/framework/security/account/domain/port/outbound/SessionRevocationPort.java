package com.ingot.framework.security.account.domain.port.outbound;

import com.ingot.framework.commons.model.security.SessionRevokeReason;

/**
 * <p>会话撤销出站端口：账号状态或凭证发生变化后，联动下线该用户已签发的会话。</p>
 *
 * <p>改密、锁定、禁用只改数据库状态，已签发的 Access / Refresh Token 不会自动失效，
 * 必须由本端口把会话从 Auth 侧撤销掉，否则「已锁定的账号仍能继续访问」。
 * 账号域不感知会话存储与 Auth 服务，实现由 adapter 经 Inner RPC 完成。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote 实现须 fail-open：撤销失败只记录日志与指标，不得抛出异常，
 * 以免账号本身的状态变更被联动失败连带回滚。
 */
public interface SessionRevocationPort {

    /**
     * 撤销用户在当前租户下全部 Client 的会话。
     *
     * @param userId  目标用户 ID
     * @param reason  撤销原因，决定安全事件口径
     * @param actorId 操作者用户 ID；系统自动触发时为 {@code null}
     * @return 实际撤销的会话数；端口未接入或撤销失败时为 0
     */
    int revokeUserSessions(Long userId, SessionRevokeReason reason, Long actorId);
}
