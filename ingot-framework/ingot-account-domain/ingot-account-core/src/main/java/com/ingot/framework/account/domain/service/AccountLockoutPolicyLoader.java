package com.ingot.framework.account.domain.service;

import com.ingot.framework.account.domain.model.LockoutPolicy;

/**
 * 账号登录失败锁定策略加载器（策略来源 seam）。
 *
 * <p>把「锁定策略来源」收敛到单一入口，消费侧（如 {@code RecordLoginUseCaseService}、
 * {@code AuthContextSupport}）一律经此取生效策略，不直读 {@code @ConfigurationProperties}。
 * 本期仅提供 {@code local} 实现；将来 {@code remote} 弹性阶梯（安全中心 + LKG + Nacos 地板）
 * 只需新增实现并按 {@code ingot.security.account.mode} 装配，消费侧零改动。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface AccountLockoutPolicyLoader {

    /**
     * 返回当前生效的锁定策略。
     *
     * @return 生效锁定策略（不可变）
     */
    LockoutPolicy getLockoutPolicy();
}
