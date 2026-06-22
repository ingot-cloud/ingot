package com.ingot.cloud.pms.web.v1.platform.dev;

import com.ingot.cloud.pms.api.model.vo.authorization.AuthorizationAuditReportVO;
import com.ingot.cloud.pms.audit.AuthorizationDataAuditService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.access.RequiredAdmin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>授权开发工具 API（只读），提供授权相关数据完整性审计能力。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(description = "DevAuthorization", name = "授权开发工具")
@RequestMapping("/v1/platform/dev/authorization")
@RequiredArgsConstructor
public class DevAuthorizationAuditAPI implements RShortcuts {

    private final AuthorizationDataAuditService authorizationDataAuditService;

    @Operation(summary = "执行授权数据审计", description = "只读审计，不修改业务数据")
    @RequiredAdmin
    @GetMapping("/audit")
    public R<AuthorizationAuditReportVO> audit(@RequestParam(required = false) Long tenantId) {
        return ok(authorizationDataAuditService.audit(tenantId));
    }
}
