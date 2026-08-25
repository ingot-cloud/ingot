package com.ingot.cloud.security.service.account;

import java.util.List;

import com.ingot.cloud.security.model.domain.AccountLockoutPolicyConfig;
import com.ingot.framework.commons.model.security.UserTypeEnum;

/**
 * <p>账号锁定策略管理面：列表、按用户类型查询与 upsert。</p>
 *
 * <p>不允许删除行。APP 行禁止 {@code lockDurationMinutes = 0}。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface AccountLockoutPolicyAdminService {

    /**
     * 返回全部用户类型的锁定策略，按 userType 排序。
     *
     * @return 策略列表，种子保证非空
     */
    List<AccountLockoutPolicyConfig> list();

    /**
     * 按用户类型查询一行。
     *
     * @param userType 用户类型，不可空
     * @return 对应策略；不存在时返回 {@code null}
     */
    AccountLockoutPolicyConfig getByUserType(UserTypeEnum userType);

    /**
     * 按 {@code userType} upsert，并在事务提交后广播 {@code ACCOUNT_LOCKOUT} 失效。
     *
     * @param policy 待写入策略，{@code userType} 必填
     * @return 持久化后的策略
     */
    AccountLockoutPolicyConfig upsert(AccountLockoutPolicyConfig policy);
}
