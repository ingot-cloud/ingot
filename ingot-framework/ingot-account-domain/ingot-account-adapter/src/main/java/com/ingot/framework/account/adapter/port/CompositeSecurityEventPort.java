package com.ingot.framework.account.adapter.port;

import com.ingot.cloud.security.api.config.SecurityEventProperties;
import com.ingot.framework.account.domain.model.AccountSecurityEvent;
import com.ingot.framework.account.domain.port.outbound.SecurityEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * 组合 {@link SecurityEventPort}：按 {@link SecurityEventProperties} 决定本地 / 中心上报。
 *
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class CompositeSecurityEventPort implements SecurityEventPort {

    private final DefaultSecurityEventPortAdapter localPort;
    @Nullable
    private final RemoteSecurityEventPortAdapter remotePort;
    private final SecurityEventProperties properties;

    @Override
    public void publishEvent(AccountSecurityEvent event) {
        if (!properties.isReportingEnabled()) {
            return;
        }
        if (properties.isLocalMode() || properties.isRemoteMode()) {
            localPort.publishEvent(event);
        }
        if (properties.isRemoteMode() && remotePort != null) {
            remotePort.publish(event);
        }
    }

    @Override
    public void publishBatch(List<AccountSecurityEvent> events) {
        if (events == null || events.isEmpty() || !properties.isReportingEnabled()) {
            return;
        }
        for (AccountSecurityEvent event : events) {
            publishEvent(event);
        }
    }
}
