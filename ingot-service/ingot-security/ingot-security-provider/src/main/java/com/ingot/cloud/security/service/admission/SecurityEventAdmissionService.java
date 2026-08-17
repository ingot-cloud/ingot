package com.ingot.cloud.security.service.admission;

import com.ingot.cloud.security.api.model.dto.BlacklistReportDTO;
import com.ingot.cloud.security.api.model.dto.SecurityEventReportDTO;
import com.ingot.cloud.security.api.support.BlacklistReportEventMapper;
import com.ingot.cloud.security.api.model.enums.SecurityEventAdmissionCode;
import com.ingot.cloud.security.api.support.SecurityEventReportValidator;
import com.ingot.framework.security.recording.model.PublishOutcome;
import com.ingot.framework.security.recording.model.RecordPriority;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.runtime.PriorityClassifier;
import com.ingot.framework.security.recording.spi.SecurityEventEnqueue;
import com.ingot.framework.security.recording.support.SecurityEventRecordValidator;
import com.ingot.framework.security.recording.transport.feign.SecurityEventReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全中心 admission：HTTP 校验与入队，Store 写入由 dispatcher 异步完成。
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SecurityEventAdmissionService {

    private final SecurityEventEnqueue enqueue;
    private final SecurityEventRecordValidator recordValidator;
    private final PriorityClassifier priorityClassifier;

    public AdmissionResult admit(SecurityEventReportDTO dto) {
        return admitBatch(List.of(dto));
    }

    public AdmissionResult admitFromBlacklist(BlacklistReportDTO dto) {
        SecurityEventReportDTO mapped = BlacklistReportEventMapper.toSecurityEvent(dto);
        if (mapped == null) {
            return AdmissionResult.ok(0);
        }
        return admit(mapped);
    }

    public AdmissionResult admitBatch(List<SecurityEventReportDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return AdmissionResult.ok(0);
        }
        SecurityEventReportValidator.validateBatchSize(dtos.size());
        List<SecurityEventRecord> records = new ArrayList<>(dtos.size());
        for (SecurityEventReportDTO dto : dtos) {
            SecurityEventReportValidator.validate(dto);
            SecurityEventRecord record = SecurityEventReportMapper.toRecord(dto);
            recordValidator.validate(record);
            records.add(record);
        }
        for (SecurityEventRecord record : records) {
            PublishOutcome outcome = enqueue.enqueue(record);
            AdmissionResult mapped = mapOutcome(outcome, priorityClassifier.classify(record));
            if (mapped != AdmissionResult.OK) {
                return mapped;
            }
        }
        return AdmissionResult.ok(records.size());
    }

    static AdmissionResult mapOutcome(PublishOutcome outcome, RecordPriority priority) {
        return switch (outcome) {
            case ACCEPTED, DISABLED -> AdmissionResult.OK;
            case DROPPED -> AdmissionResult.rejected(
                    SecurityEventAdmissionCode.ADMISSION_REJECTED,
                    priority == RecordPriority.BEST_EFFORT);
            case FAILED -> AdmissionResult.rejected(
                    priority == RecordPriority.DURABLE
                            ? SecurityEventAdmissionCode.ADMISSION_RETRYABLE
                            : SecurityEventAdmissionCode.ADMISSION_REJECTED,
                    priority != RecordPriority.DURABLE);
        };
    }

    /**
     * admission 批次结果。
     *
     * @param ok           是否允许 producer ack
     * @param retryable    producer 是否应保留 claim 并重试
     * @param code         失败时的响应码
     * @param message      失败消息
     * @param acceptedCount 接纳条数
     */
    public record AdmissionResult(
            boolean ok,
            boolean retryable,
            SecurityEventAdmissionCode code,
            String message,
            int acceptedCount) {

        static final AdmissionResult OK = new AdmissionResult(true, false, null, null, 0);

        static AdmissionResult ok(int count) {
            return new AdmissionResult(true, false, null, null, count);
        }

        static AdmissionResult rejected(SecurityEventAdmissionCode code, boolean nonRetryable) {
            return new AdmissionResult(false, !nonRetryable, code, code.getText(), 0);
        }
    }
}
