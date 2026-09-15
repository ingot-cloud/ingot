package com.ingot.cloud.iam.web.v1.platform;

import com.ingot.cloud.iam.diagnose.JdbcDiagnoseAuditService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.AuditEntry;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.Decision;
import com.ingot.framework.commons.model.iam.DiagnoseInput;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>提供平台受限授权诊断与脱敏审计。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 平台授权诊断")
@RequestMapping("/v1/platform/authorization")
@RequiredArgsConstructor
public class PlatformAuthorizationAPI implements RShortcuts {
    private final JdbcDiagnoseAuditService diagnoses;

    /**
     * 对平台成员做只读诊断。
     *
     * @param input 诊断目标
     * @return 受限解释
     */
    @Operation(summary = "受限授权诊断")
    @PostMapping("/diagnose")
    public R<Decision> diagnose(@Valid @RequestBody DiagnoseInput input) {
        return ok(diagnoses.diagnose(AuthorizationDomain.PLATFORM, input));
    }

    /**
     * 分页列出平台脱敏审计。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @return 审计页
     */
    @Operation(summary = "平台审计")
    @GetMapping("/audits")
    public R<PageResponse<ResourceDetail<AuditEntry>>> audits(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(diagnoses.listAudits(AuthorizationDomain.PLATFORM, page, pageSize));
    }
}
