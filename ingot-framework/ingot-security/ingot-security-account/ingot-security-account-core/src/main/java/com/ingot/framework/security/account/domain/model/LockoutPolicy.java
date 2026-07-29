package com.ingot.framework.security.account.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * 账号登录失败锁定策略（不可变值对象）。
 *
 * <p>作为账号保护策略消费侧与来源之间的统一载体，由 {@code AccountLockoutPolicyLoader} 产出。
 * 与 {@code AccountDomainProperties.LockoutPolicy} 解耦，使 {@code local} / 将来 {@code remote}
 * 两种来源可返回同一模型，消费侧无需感知来源差异。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Value
@Builder
public class LockoutPolicy {

    /**
     * 是否启用自动锁定
     */
    boolean enabled;

    /**
     * 失败次数阈值
     */
    int maxAttempts;

    /**
     * 锁定时长（分钟），0=永久锁定
     */
    int lockDurationMinutes;

    /**
     * 失败计数窗口期（分钟，滑动窗口尚未实现，预留）
     */
    int attemptWindowMinutes;

    /**
     * 从第几次失败开始给出「还剩几次将锁定」的详细提示
     */
    int hintAfterAttempts;
}
