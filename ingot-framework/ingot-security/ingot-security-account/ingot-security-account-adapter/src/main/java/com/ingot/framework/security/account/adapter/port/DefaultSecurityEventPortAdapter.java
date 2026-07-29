package com.ingot.framework.security.account.adapter.port;

import com.ingot.framework.security.account.adapter.entity.AccountSecurityEventEntity;
import com.ingot.framework.security.account.adapter.mapper.AccountSecurityEventMapper;
import com.ingot.framework.security.account.domain.model.AccountSecurityEvent;
import com.ingot.framework.security.account.domain.port.outbound.SecurityEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 安全事件端口默认实现（基于 account_security_event 表）
 * <p>
 * 由 {@code AccountAdapterAutoConfiguration} 以 {@code @ConditionalOnMissingBean} 方式注册，
 * 不使用 {@code @Component}，避免与业务服务中的自定义实现产生冲突。
 * </p>
 *
 * @author jymot
 * @since 2026-02-13
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultSecurityEventPortAdapter implements SecurityEventPort {

    private final AccountSecurityEventMapper eventMapper;

    @Override
    public void publishEvent(AccountSecurityEvent event) {
        log.debug("发布安全事件: userId={}, eventType={}", event.getUserId(), event.getEventType());
        
        AccountSecurityEventEntity entity = toEntity(event);
        eventMapper.insert(entity);
    }

    @Override
    public void publishBatch(List<AccountSecurityEvent> events) {
        for (AccountSecurityEvent event : events) {
            publishEvent(event);
        }
    }

    // ========== 转换方法 ==========

    private AccountSecurityEventEntity toEntity(AccountSecurityEvent event) {
        AccountSecurityEventEntity entity = new AccountSecurityEventEntity();
        entity.setUserId(event.getUserId());
        entity.setUserType(event.getUserType() != null ? event.getUserType().name() : null);
        entity.setEventType(event.getEventType() != null ? event.getEventType().getCode() : null);
        entity.setEventCategory(event.getEventCategory());
        entity.setReasonCode(event.getReasonCode());
        entity.setReasonDetail(event.getReasonDetail());
        entity.setResult(event.getResult() == null ? null : (event.getResult() ? "SUCCESS" : "FAILURE"));
        entity.setSource(event.getSource() != null ? event.getSource().getValue() : null);
        entity.setOperatorId(event.getOperatorId());
        entity.setOperatorName(event.getOperatorName());
        entity.setClientIp(event.getClientIp());
        entity.setUserAgent(event.getUserAgent());
        entity.setTenantId(event.getTenantId());
        entity.setExtraData(event.getExtraData());
        entity.setCreatedAt(event.getCreatedAt());
        return entity;
    }
}
