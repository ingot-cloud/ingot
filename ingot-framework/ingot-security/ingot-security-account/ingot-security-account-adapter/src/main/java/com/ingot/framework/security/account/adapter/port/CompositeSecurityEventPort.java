package com.ingot.framework.security.account.adapter.port;

import com.ingot.framework.security.account.adapter.support.AccountSecurityEventRecordMapper;
import com.ingot.framework.security.account.domain.model.AccountSecurityEvent;
import com.ingot.framework.security.account.domain.port.outbound.SecurityEventPort;
import com.ingot.framework.security.recording.config.SecurityEventProperties;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.spi.SecurityEventPublisher;

import java.util.List;

/**
 * <p>账号域 {@link SecurityEventPort}：经统一 Publisher 写入 canonical {@code security_event}。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public class CompositeSecurityEventPort implements SecurityEventPort {

    private final SecurityEventPublisher publisher;
    private final AccountSecurityEventRecordMapper recordMapper;
    private final SecurityEventProperties properties;

    public CompositeSecurityEventPort(
            SecurityEventPublisher publisher,
            AccountSecurityEventRecordMapper recordMapper,
            SecurityEventProperties properties) {
        this.publisher = publisher;
        this.recordMapper = recordMapper;
        this.properties = properties;
    }

    @Override
    public void publishEvent(AccountSecurityEvent event) {
        if (!properties.isEnabled()) {
            return;
        }
        SecurityEventRecord record = recordMapper.toRecord(event);
        if (record != null) {
            publisher.publish(record);
        }
    }

    @Override
    public void publishBatch(List<AccountSecurityEvent> events) {
        if (events == null || events.isEmpty() || !properties.isEnabled()) {
            return;
        }
        for (AccountSecurityEvent event : events) {
            publishEvent(event);
        }
    }
}
