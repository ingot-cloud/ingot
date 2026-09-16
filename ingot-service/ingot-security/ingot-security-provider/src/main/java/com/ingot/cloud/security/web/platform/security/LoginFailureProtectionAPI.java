package com.ingot.cloud.security.web.platform.security;

import com.ingot.cloud.security.api.model.enums.LoginFailureDimension;
import com.ingot.cloud.security.model.domain.LoginFailureProtectionPolicy;
import com.ingot.cloud.security.service.access.LoginFailureProtectionAdminService;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.access.HasAnyAuthority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 登录失败保护策略 Platform API。
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@RequestMapping("/platform/security/access/login-failure-policies")
@RequiredArgsConstructor
@Tag(name = "安全中心")
public class LoginFailureProtectionAPI implements RShortcuts {

    private final LoginFailureProtectionAdminService adminService;

    @GetMapping
    @Operation(summary = "查询登录失败保护策略列表")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_LOGIN_FAILURE_POLICY_READ})
    public R<List<LoginFailureProtectionPolicy>> list() {
        return ok(adminService.list());
    }

    @GetMapping("/{dimension}")
    @Operation(summary = "按维度查询登录失败保护策略")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_LOGIN_FAILURE_POLICY_READ})
    public R<LoginFailureProtectionPolicy> getByDimension(@PathVariable LoginFailureDimension dimension) {
        return ok(adminService.getByDimension(dimension));
    }

    @PutMapping
    @Operation(summary = "更新登录失败保护策略")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_LOGIN_FAILURE_POLICY_UPDATE})
    public R<LoginFailureProtectionPolicy> upsert(@RequestBody LoginFailureProtectionPolicy policy) {
        return ok(adminService.upsert(policy));
    }
}
