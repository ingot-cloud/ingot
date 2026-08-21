package com.ingot.framework.security.oauth2.server.authorization.session;

import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.model.security.SessionRevokeReason;
import com.ingot.framework.security.oauth2.server.authorization.OnlineToken;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;

/**
 * <p>{@link SessionRevocationService} 的默认实现，按 sid 同时撤销 Authorization 与在线会话。</p>
 *
 * @author jy
 * @since 1.0.0
 * @implNote sid 等于 {@code OAuth2Authorization.id}，因此撤销无需先解析 Token：
 * 先删 Authorization（其 remove 会级联删除会话主数据与全部 Token 索引），再补删会话，
 * 覆盖「Authorization 已过期而会话残留」的不一致场景。删除前先读一次会话主数据，
 * 既用于判定是否真的撤销了存活会话，也为撤销事件保留审计快照。
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultSessionRevocationService implements SessionRevocationService {

    private final OAuth2AuthorizationService authorizationService;
    private final OnlineTokenService onlineTokenService;
    private final List<SessionRevocationListener> listeners;

    @Override
    public boolean revokeBySid(String sid, SessionRevokeReason reason, Long actorId) {
        if (StrUtil.isEmpty(sid)) {
            return false;
        }

        OnlineToken session = onlineTokenService.getBySid(sid).orElse(null);
        OAuth2Authorization authorization = authorizationService.findById(sid);
        if (authorization != null) {
            authorizationService.remove(authorization);
        }
        // Authorization 缺失（已过期或从未落库）时仍需确保会话主数据与索引被清理
        onlineTokenService.removeBySid(sid);

        boolean online = session != null;
        log.info("[SessionRevocation] 撤销会话: sid={}, reason={}, actorId={}, online={}, authorizationFound={}",
                sid, reason, actorId, online, authorization != null);

        if (online) {
            notifyListeners(new SessionRevokedEvent(sid, reason, actorId, session));
        }
        return online;
    }

    @Override
    public int revokeByUser(Long tenantId, String clientId, Long userId,
                            SessionRevokeReason reason, Long actorId) {
        List<String> sids = StrUtil.isEmpty(clientId)
                ? onlineTokenService.listSids(tenantId, userId)
                : onlineTokenService.listSids(tenantId, clientId, userId);
        int revoked = 0;
        for (String sid : sids) {
            if (revokeBySid(sid, reason, actorId)) {
                revoked++;
            }
        }

        log.info("[SessionRevocation] 撤销用户全部会话: tenantId={}, clientId={}, userId={}, "
                        + "reason={}, actorId={}, count={}",
                tenantId, clientId, userId, reason, actorId, revoked);
        return revoked;
    }

    /**
     * 通知撤销回调；回调异常不影响撤销结果，会话已经删除，回滚只会带来更差的半撤销状态。
     */
    private void notifyListeners(SessionRevokedEvent event) {
        for (SessionRevocationListener listener : listeners) {
            try {
                listener.onRevoked(event);
            } catch (Exception e) {
                log.error("[SessionRevocation] 撤销回调执行失败: sid={}, listener={}",
                        event.sid(), listener.getClass().getName(), e);
            }
        }
    }
}
