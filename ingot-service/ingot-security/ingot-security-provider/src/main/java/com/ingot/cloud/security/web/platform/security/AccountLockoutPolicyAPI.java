package com.ingot.cloud.security.web.platform.security;

import java.util.List;

import com.ingot.cloud.security.model.domain.AccountLockoutPolicyConfig;
import com.ingot.cloud.security.service.account.AccountLockoutPolicyAdminService;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>账号锁定策略 Platform API，供安全中心管理台维护 B/C 两端登录失败锁定参数。</p>
 *
 * <p>种子固定两行，只提供查询与更新；不允许删除。保存后广播失效，消费节点无需重启。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@RequestMapping("/platform/security/account/lockout-policies")
@RequiredArgsConstructor
@Tag(name = "安全中心-账号锁定策略")
public class AccountLockoutPolicyAPI implements RShortcuts {

    private final AccountLockoutPolicyAdminService adminService;

    /**
     * 查询全部用户类型的锁定策略。
     *
     * @return 固定两行（ADMIN / APP）
     */
    @GetMapping
    @Operation(summary = "查询账号锁定策略列表")
    @AdminOrHasAnyAuthority({"platform:security:account:lockout:query"})
    public R<List<AccountLockoutPolicyConfig>> list() {
        return ok(adminService.list());
    }

    /**
     * 按用户类型查询一行。
     *
     * @param userType {@code 0}（ADMIN）或 {@code 1}（APP）
     * @return 对应策略；不存在时 data 为 null
     */
    @GetMapping("/{userType}")
    @Operation(summary = "按用户类型查询账号锁定策略")
    @AdminOrHasAnyAuthority({"platform:security:account:lockout:query"})
    public R<AccountLockoutPolicyConfig> getByUserType(
            @Parameter(description = "用户类型：0=B端管理员，1=C端用户")
            @PathVariable String userType) {
        return ok(adminService.getByUserType(resolveUserType(userType)));
    }

    /**
     * 按 userType upsert 一行策略。
     *
     * @param policy 待写入内容，userType 必填
     * @return 持久化后的策略
     */
    @PutMapping
    @Operation(summary = "更新账号锁定策略")
    @AdminOrHasAnyAuthority({"platform:security:account:lockout:update"})
    public R<AccountLockoutPolicyConfig> upsert(@RequestBody AccountLockoutPolicyConfig policy) {
        return ok(adminService.upsert(policy));
    }

    private static UserTypeEnum resolveUserType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        UserTypeEnum byValue = UserTypeEnum.getEnum(trimmed);
        if (byValue != null) {
            return byValue;
        }
        try {
            return UserTypeEnum.valueOf(trimmed);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
