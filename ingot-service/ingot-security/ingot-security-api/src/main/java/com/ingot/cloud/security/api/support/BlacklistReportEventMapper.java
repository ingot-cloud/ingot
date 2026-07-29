package com.ingot.cloud.security.api.support;

import com.ingot.cloud.security.api.model.dto.BlacklistReportDTO;
import com.ingot.cloud.security.api.model.dto.SecurityEventReportDTO;
import com.ingot.cloud.security.api.model.enums.BlacklistEventAction;
import com.ingot.cloud.security.api.model.enums.BlacklistTriggerSource;
import com.ingot.cloud.security.api.model.enums.SecurityEventCategory;
import com.ingot.cloud.security.api.model.enums.SecurityEventType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 网关封禁审计 DTO → 统一安全事件 DTO 映射。
 *
 * @author jy
 * @since 1.0.0
 */
public final class BlacklistReportEventMapper {

    private static final String SOURCE_MODULE_GATEWAY = "ingot-gateway";
    private static final String SOURCE_GATEWAY = "GATEWAY";

    private BlacklistReportEventMapper() {
    }

    public static SecurityEventReportDTO toSecurityEvent(BlacklistReportDTO dto) {
        if (dto == null) {
            return null;
        }
        Map<String, Object> extension = new LinkedHashMap<>();
        extension.put("keyType", dto.getKeyType());
        extension.put("keyValue", dto.getKeyValue());
        extension.put("action", dto.getAction());
        extension.put("triggerSource", dto.getTriggerSource());
        extension.put("ruleCode", dto.getRuleCode());
        extension.put("countInWindow", dto.getCountInWindow());
        extension.put("ttlSec", dto.getTtlSec());
        extension.put("remark", dto.getRemark());

        SecurityEventType eventType = resolveEventType(dto);

        return SecurityEventReportDTO.builder()
                .eventType(eventType.getCode())
                .eventCategory(SecurityEventCategory.ACCESS.getCode())
                .clientIp(dto.getRealIp())
                .requestUri(dto.getRequestPath())
                .userAgent(dto.getUserAgent())
                .traceId(dto.getTraceId())
                .sourceModule(SOURCE_MODULE_GATEWAY)
                .source(SOURCE_GATEWAY)
                .extension(extension)
                .build();
    }

    private static SecurityEventType resolveEventType(BlacklistReportDTO dto) {
        BlacklistTriggerSource trigger = BlacklistTriggerSource.fromCode(dto.getTriggerSource());
        if (trigger == BlacklistTriggerSource.AUTO
                && dto.getRuleCode() != null
                && !dto.getRuleCode().isBlank()
                && BlacklistEventAction.BLOCK.getCode().equals(dto.getAction())) {
            return SecurityEventType.RATE_LIMIT_VIOLATION;
        }
        return SecurityEventType.BLACKLIST_BLOCK;
    }
}
