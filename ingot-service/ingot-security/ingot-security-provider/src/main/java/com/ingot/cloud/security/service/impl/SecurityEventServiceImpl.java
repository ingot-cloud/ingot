package com.ingot.cloud.security.service.impl;

import com.ingot.cloud.security.api.model.dto.BlacklistReportDTO;
import com.ingot.cloud.security.api.model.dto.SecurityEventReportDTO;
import com.ingot.cloud.security.api.model.enums.SecurityEventCategory;
import com.ingot.cloud.security.api.model.enums.SecurityEventType;
import com.ingot.cloud.security.api.support.BlacklistReportEventMapper;
import com.ingot.cloud.security.mapper.SecurityEventMapper;
import com.ingot.cloud.security.model.domain.SecurityEvent;
import com.ingot.cloud.security.service.SecurityEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一安全事件入库实现。
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SecurityEventServiceImpl implements SecurityEventService {

    private final SecurityEventMapper securityEventMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(SecurityEventReportDTO dto) {
        validate(dto);
        securityEventMapper.insert(toEntity(dto));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(List<SecurityEventReportDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        for (SecurityEventReportDTO dto : dtos) {
            save(dto);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveFromBlacklistReport(BlacklistReportDTO dto) {
        SecurityEventReportDTO mapped = BlacklistReportEventMapper.toSecurityEvent(dto);
        if (mapped == null) {
            return;
        }
        save(mapped);
    }

    static void validate(SecurityEventReportDTO dto) {
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
    }

    private static SecurityEvent toEntity(SecurityEventReportDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime occurredAt = dto.getOccurredAt() != null ? dto.getOccurredAt() : now;
        SecurityEvent entity = new SecurityEvent();
        entity.setEventType(dto.getEventType());
        entity.setEventCategory(dto.getEventCategory());
        entity.setOccurredAt(occurredAt);
        entity.setReceivedAt(now);
        entity.setTenantId(dto.getTenantId());
        entity.setUserId(dto.getUserId());
        entity.setUserType(dto.getUserType());
        entity.setAccount(dto.getAccount());
        entity.setClientId(dto.getClientId());
        entity.setAppId(dto.getAppId());
        entity.setSessionId(dto.getSessionId());
        entity.setDeviceId(dto.getDeviceId());
        entity.setClientIp(dto.getClientIp());
        entity.setRequestUri(dto.getRequestUri());
        entity.setUserAgent(dto.getUserAgent());
        entity.setResult(dto.getResult());
        entity.setReasonCode(dto.getReasonCode());
        entity.setReasonDetail(dto.getReasonDetail());
        entity.setSourceModule(dto.getSourceModule());
        entity.setSource(dto.getSource());
        entity.setOperatorId(dto.getOperatorId());
        entity.setOperatorName(dto.getOperatorName());
        entity.setTraceId(dto.getTraceId());
        entity.setExtension(dto.getExtension());
        return entity;
    }
}
