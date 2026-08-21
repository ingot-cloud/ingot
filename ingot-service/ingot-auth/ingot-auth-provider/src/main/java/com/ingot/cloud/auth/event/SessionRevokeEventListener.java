package com.ingot.cloud.auth.event;

import java.time.Instant;

import cn.hutool.core.util.IdUtil;
import com.ingot.cloud.security.api.model.enums.SecurityEventType;
import com.ingot.framework.commons.model.security.SessionRevokeReason;
import com.ingot.framework.security.account.domain.model.enums.EventSource;
import com.ingot.framework.security.oauth2.server.authorization.OnlineToken;
import com.ingot.framework.security.oauth2.server.authorization.session.SessionRevocationListener;
import com.ingot.framework.security.oauth2.server.authorization.session.SessionRevokedEvent;
import com.ingot.framework.security.recording.config.SecurityEventProperties;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.spi.SecurityEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * <p>把会话撤销结果转写为安全事件，是「谁在何时被下线」的唯一证据来源。</p>
 *
 * <p>事件类型按撤销原因区分：用户自助登出记 {@link SecurityEventType#LOGOUT}，
 * 并发策略踢人记 {@link SecurityEventType#SESSION_CONCURRENT_KICKOUT}，
 * 其余被动下线（管理员强制、改密、锁定、禁用联动）统一记
 * {@link SecurityEventType#SESSION_REVOKED}。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see SessionRevocationListener
 * @implNote recording 框架仅在 {@code ingot.security.event.enabled=true} 时装配，
 * 因此这里按需解析 Publisher：未启用时静默跳过，撤销结果不受影响。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionRevokeEventListener implements SessionRevocationListener {

    /**
     * 会话撤销一旦产生事件即代表执行成功，失败路径不会走到这里。
     */
    private static final String RESULT_SUCCESS = "SUCCESS";

    private final ObjectProvider<SecurityEventPublisher> publisherProvider;
    private final ObjectProvider<SecurityEventProperties> propertiesProvider;

    @Override
    public void onRevoked(SessionRevokedEvent event) {
        SecurityEventPublisher publisher = publisherProvider.getIfAvailable();
        SecurityEventProperties properties = propertiesProvider.getIfAvailable();
        if (publisher == null || properties == null) {
            log.debug("[SessionRevokeEvent] 安全事件上报未启用，跳过: sid={}", event.sid());
            return;
        }
        publisher.publish(toRecord(event, properties.getSourceModule()));
    }

    private SecurityEventRecord toRecord(SessionRevokedEvent event, String sourceModule) {
        OnlineToken session = event.session();
        SecurityEventType eventType = resolveEventType(event.reason());
        return SecurityEventRecord.builder()
                .eventId(IdUtil.simpleUUID())
                .eventType(eventType.getCode())
                .eventCategory(eventType.getCategory().getCode())
                .occurredAt(Instant.now())
                .tenantId(session.getTenantId())
                .userId(session.getUserId())
                .userType(session.getUserType())
                .account(session.getPrincipalName())
                .clientId(session.getClientId())
                .sessionId(event.sid())
                .clientIp(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .result(RESULT_SUCCESS)
                .reasonCode(event.reason() == null ? null : event.reason().getValue())
                .sourceModule(sourceModule)
                .source(EventSource.AUTH.getValue())
                .operatorId(event.actorId())
                .build();
    }

    private static SecurityEventType resolveEventType(SessionRevokeReason reason) {
        if (reason == null) {
            return SecurityEventType.SESSION_REVOKED;
        }
        return switch (reason) {
            case USER_LOGOUT -> SecurityEventType.LOGOUT;
            case CONCURRENT_KICKOUT -> SecurityEventType.SESSION_CONCURRENT_KICKOUT;
            default -> SecurityEventType.SESSION_REVOKED;
        };
    }
}
