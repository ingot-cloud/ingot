package com.ingot.framework.security.oauth2.server.authorization.session;

import com.ingot.framework.commons.model.security.SessionRevokeReason;

/**
 * <p>会话撤销领域服务，是「彻底撤销」的唯一入口。</p>
 *
 * <p>一次撤销必须同时抹掉 OAuth2 Authorization（连带 Access / Refresh Token 索引）与在线会话
 * 主数据，否则会出现「Access Token 失效但 Refresh Token 仍可换新」或「会话已删但 Token 索引残留」
 * 的半撤销状态。调用方不应绕过本服务直接操作
 * {@link org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService} 或
 * {@link com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService}。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see SessionRevocationListener
 * @apiNote 实现须保证幂等：对已撤销或已过期的会话重复调用不报错，仅返回未命中。
 */
public interface SessionRevocationService {

    /**
     * 撤销单个会话，操作者未知。
     *
     * @param sid    会话 ID
     * @param reason 撤销原因，决定审计与安全事件口径
     * @return {@code true} 表示本次调用确实撤销了一个存活会话
     */
    default boolean revokeBySid(String sid, SessionRevokeReason reason) {
        return revokeBySid(sid, reason, null);
    }

    /**
     * 撤销单个会话。
     *
     * @param actorId 操作者用户 ID，写入安全事件 {@code operatorId}；系统自动触发时传 {@code null}
     * @return {@code true} 表示本次调用确实撤销了一个存活会话
     */
    boolean revokeBySid(String sid, SessionRevokeReason reason, Long actorId);

    /**
     * 撤销用户会话，操作者未知。
     *
     * @return 实际撤销的会话数
     */
    default int revokeByUser(Long tenantId, String clientId, Long userId, SessionRevokeReason reason) {
        return revokeByUser(tenantId, clientId, userId, reason, null);
    }

    /**
     * 撤销用户会话。
     *
     * @param clientId 限定 Client；传 {@code null} 表示该用户在当前租户下的全部 Client
     * @param actorId  操作者用户 ID，系统自动触发时传 {@code null}
     * @return 实际撤销的会话数
     */
    int revokeByUser(Long tenantId, String clientId, Long userId, SessionRevokeReason reason, Long actorId);
}
