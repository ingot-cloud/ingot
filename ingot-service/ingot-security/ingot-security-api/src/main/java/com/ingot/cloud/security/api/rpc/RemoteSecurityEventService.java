package com.ingot.cloud.security.api.rpc;

import com.ingot.cloud.security.api.model.dto.SecurityEventReportDTO;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 统一安全事件上报 Feign 接口（内网）。
 *
 * @author jy
 * @since 1.0.0
 */
@FeignClient(contextId = "RemoteSecurityEventService", value = ServiceNameConstants.SECURITY_SERVICE)
public interface RemoteSecurityEventService {

    @PostMapping("/inner/security/event/report")
    R<Void> report(@RequestBody SecurityEventReportDTO dto);

    @PostMapping("/inner/security/event/report/batch")
    R<Void> reportBatch(@RequestBody List<SecurityEventReportDTO> dtos);
}
