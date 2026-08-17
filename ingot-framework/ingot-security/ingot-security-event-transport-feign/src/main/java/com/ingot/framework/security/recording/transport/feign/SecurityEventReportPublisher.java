package com.ingot.framework.security.recording.transport.feign;

import com.ingot.cloud.security.api.model.dto.SecurityEventReportDTO;
import com.ingot.framework.security.recording.spi.SecurityEventPublisher;
import lombok.RequiredArgsConstructor;

/**
 * <p>将 {@link SecurityEventReportDTO} 经统一 Publisher 发布，供网关与 access 适配器使用。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public final class SecurityEventReportPublisher {

    private final SecurityEventPublisher publisher;

    public void publish(SecurityEventReportDTO dto) {
        if (dto == null) {
            return;
        }
        publisher.publish(SecurityEventReportMapper.toRecord(dto));
    }
}
