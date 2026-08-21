package com.ingot.framework.security.oauth2.server.authorization.session.concurrency;

import com.ingot.framework.commons.model.security.SessionConcurrencyDimension;
import com.ingot.framework.commons.model.security.SessionOverflowStrategy;

/**
 * <p>某次登录已解析出的并发会话约束，是策略来源与执行面之间唯一的传递形态。</p>
 *
 * <p>解析发生在登录时：无论策略来自安全中心、Nacos 地板还是 Client 的 {@code UNIQUE} 配置，
 * 到达执行面时都已收敛成这一个不可变结论，执行面不再关心它从哪来。
 * {@code maxSessions} 为 {@code 0} 表示无限，此时 {@link #overflow} 与
 * {@link #adminForbidConcurrent} 仍可能生效 —— 前者在 Client 配置为单会话时决定踢谁，
 * 后者对管理用户强制单会话。</p>
 *
 * @param maxSessions           允许的最大并发会话数，{@code 0} 表示无限
 * @param dimension             会话数统计维度
 * @param overflow              超限处置方式
 * @param adminForbidConcurrent 管理用户是否强制单会话
 * @author jy
 * @since 1.0.0
 * @see SessionConcurrencyEnforcer
 */
public record SessionConcurrencyRule(int maxSessions,
                                     SessionConcurrencyDimension dimension,
                                     SessionOverflowStrategy overflow,
                                     boolean adminForbidConcurrent) {

    /**
     * 无限并发的默认约束：不限会话数，Client 配置为单会话时踢除最旧会话。
     *
     * <p>也是并发策略总开关关闭时的行为，与本闭环上线前的现网语义一致。</p>
     */
    public static SessionConcurrencyRule unlimited() {
        return new SessionConcurrencyRule(0, SessionConcurrencyDimension.USER_CLIENT,
                SessionOverflowStrategy.KICK_OLDEST, false);
    }
}
