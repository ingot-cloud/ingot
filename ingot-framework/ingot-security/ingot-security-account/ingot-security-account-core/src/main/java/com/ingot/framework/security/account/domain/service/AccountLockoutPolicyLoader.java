package com.ingot.framework.security.account.domain.service;

import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.model.LockoutPolicy;

/**
 * <p>账号登录失败锁定策略加载器，把策略来源收敛到单一入口。</p>
 *
 * <p>消费侧（{@code RecordLoginUseCaseService}、{@code AuthContextSupport}）一律经此取生效策略，
 * 不直读 {@code @ConfigurationProperties}。{@code mode=local} 读 Nacos；{@code mode=remote}
 * 走安全中心分层缓存链。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface AccountLockoutPolicyLoader {

    /**
     * 返回指定用户类型当前生效的锁定策略。
     *
     * @param userType 用户类型；{@code null} 时 remote 回落快照第一行，local 仍返回本进程 Nacos
     * @return 生效锁定策略（不可变）
     */
    LockoutPolicy getLockoutPolicy(UserTypeEnum userType);

    /**
     * 清除 remote 模式的 L1/L2；local 实现为空操作。
     * <p>不清除 LKG。</p>
     */
    default void evictAll() {
        // local 无缓存
    }
}
