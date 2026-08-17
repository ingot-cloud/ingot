package com.ingot.framework.security.recording.transport.feign;

import com.ingot.cloud.security.api.model.dto.SecurityEventReportDTO;
import com.ingot.cloud.security.api.model.enums.SecurityEventAdmissionCode;
import com.ingot.cloud.security.api.rpc.RemoteSecurityEventService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.recording.model.DeliveryResult;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.spi.SecurityEventTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * <p>经 Feign 批量投递安全事件至中心 admission 接口。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public final class FeignSecurityEventTransport implements SecurityEventTransport {

    private final RemoteSecurityEventService remoteService;

    @Override
    public String transportId() {
        return "feign";
    }

    @Override
    public DeliveryResult deliverBatch(List<SecurityEventRecord> records) {
        if (records == null || records.isEmpty()) {
            return DeliveryResult.success(0);
        }
        List<SecurityEventReportDTO> dtos = SecurityEventReportMapper.toDtos(records);
        try {
            R<Void> response = dtos.size() == 1
                    ? remoteService.report(dtos.get(0))
                    : remoteService.reportBatch(dtos);
            if (response != null && response.isSuccess()) {
                return DeliveryResult.success(records.size());
            }
            String code = response == null ? null : response.getCode();
            boolean retryable = SecurityEventAdmissionCode.ADMISSION_RETRYABLE.getCode().equals(code);
            String message = response == null ? "empty response" : response.getMessage();
            log.debug("Feign security event transport failed: code={}, retryable={}, size={}",
                    code, retryable, records.size());
            return DeliveryResult.failure(records.size(), retryable, message);
        } catch (Exception e) {
            log.warn("Feign security event transport error (batchSize={}): {}", records.size(), e.getMessage());
            return DeliveryResult.failure(records.size(), true, e.getMessage());
        }
    }
}
