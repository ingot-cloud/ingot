package com.ingot.cloud.security.web.inner;

import java.util.List;

import com.ingot.cloud.security.api.model.vo.policy.AccountLockoutPolicyVO;
import com.ingot.cloud.security.model.domain.AccountLockoutPolicyConfig;
import com.ingot.cloud.security.service.account.AccountLockoutPolicyAdminService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>账号锁定策略 Inner API，供 PMS / Member Feign 拉取全量快照。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Permit(mode = PermitMode.INNER)
@RestController
@RequestMapping("/inner/security/account")
@RequiredArgsConstructor
public class InnerAccountLockoutPolicyAPI implements RShortcuts {

    private final AccountLockoutPolicyAdminService adminService;

    /**
     * 返回全部锁定策略，供远程 loader 写入分层缓存。
     *
     * @return 策略 VO 列表
     */
    @GetMapping("/lockout-policies")
    public R<List<AccountLockoutPolicyVO>> listPolicies() {
        return ok(adminService.list().stream().map(this::toVo).toList());
    }

    private AccountLockoutPolicyVO toVo(AccountLockoutPolicyConfig entity) {
        AccountLockoutPolicyVO vo = new AccountLockoutPolicyVO();
        vo.setId(entity.getId());
        vo.setUserType(entity.getUserType());
        vo.setEnabled(entity.getEnabled());
        vo.setMaxAttempts(entity.getMaxAttempts());
        vo.setLockDurationMinutes(entity.getLockDurationMinutes());
        vo.setAttemptWindowMinutes(entity.getAttemptWindowMinutes());
        vo.setHintAfterAttempts(entity.getHintAfterAttempts());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
