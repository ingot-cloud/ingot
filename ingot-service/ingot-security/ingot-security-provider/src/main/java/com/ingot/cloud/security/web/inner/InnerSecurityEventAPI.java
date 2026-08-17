package com.ingot.cloud.security.web.inner;

import com.ingot.cloud.security.api.model.dto.SecurityEventReportDTO;
import com.ingot.cloud.security.api.model.enums.SecurityEventAdmissionCode;
import com.ingot.cloud.security.service.admission.SecurityEventAdmissionService;
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

    private final SecurityEventAdmissionService admissionService;

    @PostMapping("/report")
    public R<Void> report(@RequestBody SecurityEventReportDTO dto) {
        return admitSafely(() -> admissionService.admit(dto));
    }

    @PostMapping("/report/batch")
    public R<Void> reportBatch(@RequestBody List<SecurityEventReportDTO> dtos) {
        return admitSafely(() -> admissionService.admitBatch(dtos));
    }

    private static R<Void> admitSafely(java.util.function.Supplier<SecurityEventAdmissionService.AdmissionResult> action) {
        try {
            return toResponse(action.get());
        } catch (IllegalArgumentException e) {
            return R.error(
                    SecurityEventAdmissionCode.ADMISSION_INVALID.getCode(),
                    e.getMessage());
        }
    }

    private static R<Void> toResponse(SecurityEventAdmissionService.AdmissionResult result) {
        if (result.ok()) {
            return R.ok();
        }
        if (result.code() == SecurityEventAdmissionCode.ADMISSION_INVALID) {
            return R.error(result.code());
        }
        return R.error(result.code().getCode(), result.message());
    }
}
