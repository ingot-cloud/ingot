package com.ingot.cloud.security.api.support;

import com.ingot.cloud.security.api.model.dto.SecurityEventReportDTO;
import com.ingot.cloud.security.api.model.enums.SecurityEventCategory;
import com.ingot.cloud.security.api.model.enums.SecurityEventType;
import org.springframework.util.StringUtils;

/**
 * 统一安全事件上报 DTO 校验（中心 admission 与旧入库路径共用）。
 *
 * @author jy
 * @since 1.0.0
 */
public final class SecurityEventReportValidator {

    public static final int MAX_BATCH_SIZE = 100;

    private SecurityEventReportValidator() {
    }

    public static void validate(SecurityEventReportDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("security event payload is required");
        }
        if (!StringUtils.hasText(dto.getEventType())) {
            throw new IllegalArgumentException("eventType is required");
        }
        if (!StringUtils.hasText(dto.getEventCategory())) {
            throw new IllegalArgumentException("eventCategory is required");
        }
        if (!StringUtils.hasText(dto.getSourceModule())) {
            throw new IllegalArgumentException("sourceModule is required");
        }
        if (SecurityEventType.fromCode(dto.getEventType()) == null) {
            throw new IllegalArgumentException("unknown eventType: " + dto.getEventType());
        }
        if (SecurityEventCategory.fromCode(dto.getEventCategory()) == null) {
            throw new IllegalArgumentException("unknown eventCategory: " + dto.getEventCategory());
        }
        if (dto.getEventId() != null && !dto.getEventId().isBlank() && dto.getEventId().length() != 32) {
            throw new IllegalArgumentException("eventId must be 32 characters when present");
        }
    }

    public static void validateBatchSize(int size) {
        if (size > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("batch size exceeds " + MAX_BATCH_SIZE);
        }
    }
}
