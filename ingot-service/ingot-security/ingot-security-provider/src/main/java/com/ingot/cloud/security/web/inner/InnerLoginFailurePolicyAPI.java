package com.ingot.cloud.security.web.inner;

import com.ingot.cloud.security.api.model.vo.policy.LoginFailureProtectionPolicyVO;
import com.ingot.cloud.security.model.domain.LoginFailureProtectionPolicy;
import com.ingot.cloud.security.service.access.LoginFailureProtectionAdminService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 登录失败保护策略 Inner API。
 *
 * @author jy
 * @since 1.0.0
 */
@Permit(mode = PermitMode.INNER)
@RestController
@RequestMapping("/inner/security/access")
@RequiredArgsConstructor
public class InnerLoginFailurePolicyAPI implements RShortcuts {

    private final LoginFailureProtectionAdminService adminService;

    @GetMapping("/login-failure-policies")
    public R<List<LoginFailureProtectionPolicyVO>> listPolicies() {
        return ok(adminService.list().stream().map(this::toVo).toList());
    }

    private LoginFailureProtectionPolicyVO toVo(LoginFailureProtectionPolicy entity) {
        LoginFailureProtectionPolicyVO vo = new LoginFailureProtectionPolicyVO();
        vo.setId(entity.getId());
        vo.setDimension(entity.getDimension());
        vo.setEnabled(entity.getEnabled());
        vo.setMaxAttempts(entity.getMaxAttempts());
        vo.setWindowMinutes(entity.getWindowMinutes());
        vo.setBlockTtlSec(entity.getBlockTtlSec());
        vo.setBlockKeyType(entity.getBlockKeyType());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
