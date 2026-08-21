package com.ingot.framework.security.account.domain.port.outbound.noop;

import com.ingot.framework.commons.model.security.SessionRevokeReason;
import com.ingot.framework.security.account.domain.port.outbound.SessionRevocationPort;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>未接入 Auth Inner RPC 时的空实现，账号状态变更仍然生效但不联动下线。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
public class NoOpSessionRevocationPort implements SessionRevocationPort {

    @Override
    public int revokeUserSessions(Long userId, SessionRevokeReason reason, Long actorId) {
        log.debug("[NoOpSessionRevocation] 未接入会话撤销端口，跳过联动: userId={}, reason={}", userId, reason);
        return 0;
    }
}
