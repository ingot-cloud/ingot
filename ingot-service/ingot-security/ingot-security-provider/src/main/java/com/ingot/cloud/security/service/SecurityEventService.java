package com.ingot.cloud.security.service;

import com.ingot.cloud.security.api.model.dto.BlacklistReportDTO;
import com.ingot.cloud.security.api.model.dto.SecurityEventReportDTO;

import java.util.List;

/**
 * 统一安全事件入库服务。
 *
 * @author jy
 * @since 1.0.0
 */
public interface SecurityEventService {

    void save(SecurityEventReportDTO dto);

    void saveBatch(List<SecurityEventReportDTO> dtos);

    /**
     * 兼容旧网关 {@code reportBlacklist} 入口，映射后写入 {@code security_event}。
     */
    void saveFromBlacklistReport(BlacklistReportDTO dto);
}
