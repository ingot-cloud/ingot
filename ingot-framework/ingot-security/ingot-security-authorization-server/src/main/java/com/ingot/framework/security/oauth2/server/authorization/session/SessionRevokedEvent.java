package com.ingot.framework.security.oauth2.server.authorization.session;

import com.ingot.framework.commons.model.security.SessionRevokeReason;
import com.ingot.framework.security.oauth2.server.authorization.OnlineToken;

/**
 * <p>会话撤销结果快照，仅在确实撤销了一个存活会话时产生。</p>
 *
 * <p>{@link #session()} 是删除前读到的会话主数据，撤销完成后 Redis 已无处可查，
 * 因此审计所需的租户、用户、Client、登录环境等事实必须随事件一并传递。</p>
 *
 * @param sid     被撤销的会话 ID
 * @param reason  撤销原因
 * @param actorId 操作者用户 ID；系统自动触发（如并发踢人）时为 {@code null}
 * @param session 撤销前的会话快照
 * @author jy
 * @since 1.0.0
 * @see SessionRevocationListener
 */
public record SessionRevokedEvent(String sid,
                                  SessionRevokeReason reason,
                                  Long actorId,
                                  OnlineToken session) {
}
