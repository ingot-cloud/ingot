package com.ingot.framework.security.account.adapter.port;

import com.ingot.cloud.security.api.config.SecurityEventProperties;
import com.ingot.cloud.security.api.model.dto.SecurityEventReportDTO;
import com.ingot.cloud.security.api.rpc.RemoteSecurityEventService;
import com.ingot.cloud.security.api.support.AsyncSecurityEventReporter;
import com.ingot.framework.security.account.adapter.support.AccountSecurityEventReportMapper;
import com.ingot.framework.security.account.domain.model.AccountSecurityEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

/**
 * 异步向 ingot-security 上报账号域安全事件。
 *
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class RemoteSecurityEventPortAdapter {

    private final ObjectProvider<RemoteSecurityEventService> remoteProvider;
    private final SecurityEventProperties properties;
    private final AccountSecurityEventReportMapper reportMapper;
    private AsyncSecurityEventReporter reporter;

    @PostConstruct
    void init() {
        reporter = new AsyncSecurityEventReporter(
                remoteProvider::getIfAvailable,
                properties,
                "in-acct-security-event-reporter",
                "RemoteSecurityEventPort");
    }

    @PreDestroy
    void destroy() {
        if (reporter != null) {
            reporter.close();
        }
    }

    public void publish(AccountSecurityEvent event) {
        if (!properties.isRemoteCategoryEnabled(event.getEventCategory())) {
            return;
        }
        SecurityEventReportDTO dto = reportMapper.toReportDto(event);
        if (dto == null) {
            return;
        }
        reporter.offer(dto);
    }

    public void publishBatch(List<AccountSecurityEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (AccountSecurityEvent event : events) {
            publish(event);
        }
    }
}
