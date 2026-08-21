package com.ingot.framework.security.oauth2.server.authorization.session.concurrency;

import com.ingot.framework.security.core.userdetails.InUser;

/**
 * <p>并发会话策略来源，把登录用户解析成一条可执行的并发约束。</p>
 *
 * <p>实现按部署形态选择：本地地板配置、安全中心远端策略，或总开关关闭时的 Client 语义兜底。
 * 无法给出可信结论时必须抛 {@link SessionPolicyUnavailableException} 而不是返回
 * {@link SessionConcurrencyRule#unlimited()} —— 静默放开为无限并发会把一次配置中心故障
 * 变成一次安全策略失效。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see SessionConcurrencyEnforcer
 */
@FunctionalInterface
public interface SessionConcurrencyPolicyResolver {

    /**
     * 解析该用户本次登录适用的并发约束。
     *
     * @param user 已切片到目标租户的登录用户
     * @return 并发约束，不为 {@code null}
     * @throws SessionPolicyUnavailableException 策略来源不可用且无任何兜底
     */
    SessionConcurrencyRule resolve(InUser user);
}
