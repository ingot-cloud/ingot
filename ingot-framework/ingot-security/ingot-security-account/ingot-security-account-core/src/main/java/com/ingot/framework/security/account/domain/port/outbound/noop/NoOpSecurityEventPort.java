package com.ingot.framework.security.account.domain.port.outbound.noop;

import com.ingot.framework.security.account.domain.model.AccountSecurityEvent;
import com.ingot.framework.security.account.domain.port.outbound.SecurityEventPort;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * SecurityEventPort 空实现
 * <p>
 * 当不需要安全事件持久化时，使用此空实现作为默认值。
 * 事件仅打印日志，不写入数据库。
 * 各业务服务可引入 ingot-security-account-adapter 模块来获得完整的事件持久化实现，
 * 或者替换为消息队列等其他实现。
 * </p>
 *
 * @author jymot
 * @since 2026-02-13
 */
@Slf4j
public class NoOpSecurityEventPort implements SecurityEventPort {

    @Override
    public void publishEvent(AccountSecurityEvent event) {
        log.info("[SecurityEvent] userId={}, type={}, result={}",
                event.getUserId(),
                event.getEventType() != null ? event.getEventType().getCode() : "UNKNOWN",
                event.getResult());
    }

    @Override
    public void publishBatch(List<AccountSecurityEvent> events) {
        events.forEach(this::publishEvent);
    }
}
