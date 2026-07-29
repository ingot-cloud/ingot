package com.ingot.cloud.security.web.inner;

import com.ingot.cloud.security.api.model.dto.SecurityEventReportDTO;
import com.ingot.cloud.security.service.SecurityEventService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 统一安全事件内网上报接口。
 *
 * @author jy
 * @since 1.0.0
 */
@Permit(mode = PermitMode.INNER)
@RestController
@RequestMapping("/inner/security/event")
@RequiredArgsConstructor
public class InnerSecurityEventAPI implements RShortcuts {

    private final SecurityEventService securityEventService;

    @PostMapping("/report")
    public R<Void> report(@RequestBody SecurityEventReportDTO dto) {
        securityEventService.save(dto);
        return ok();
    }

    @PostMapping("/report/batch")
    public R<Void> reportBatch(@RequestBody List<SecurityEventReportDTO> dtos) {
        securityEventService.saveBatch(dtos);
        return ok();
    }
}
