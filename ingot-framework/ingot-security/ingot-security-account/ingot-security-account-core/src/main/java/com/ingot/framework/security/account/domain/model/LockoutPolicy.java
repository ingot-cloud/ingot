package com.ingot.framework.security.account.domain.model;

import com.ingot.framework.commons.model.security.UserTypeEnum;

/**
 * <p>账号登录失败锁定策略的不可变值对象，由 {@code AccountLockoutPolicyLoader} 产出。</p>
 *
 * <p>与 {@code AccountDomainProperties.LockoutPolicy} 解耦，使 local / remote 两种来源返回同一模型。
 * {@code userType} 用于 remote 快照按类型取行；local 实现会带上调用方传入的类型。</p>
 *
 * @param userType             策略所属用户类型；地板单元素场景可能为 {@code null}
 * @param enabled              是否启用自动锁定
 * @param maxAttempts          失败次数阈值
 * @param lockDurationMinutes  锁定时长（分钟），{@code 0}=永久
 * @param attemptWindowMinutes 失败计数窗口（分钟）
 * @param hintAfterAttempts    从第几次失败开始给出剩余次数提示
 * @author jy
 * @since 1.0.0
 */
public record LockoutPolicy(
        UserTypeEnum userType,
        boolean enabled,
        int maxAttempts,
        int lockDurationMinutes,
        int attemptWindowMinutes,
        int hintAfterAttempts
) {
}
