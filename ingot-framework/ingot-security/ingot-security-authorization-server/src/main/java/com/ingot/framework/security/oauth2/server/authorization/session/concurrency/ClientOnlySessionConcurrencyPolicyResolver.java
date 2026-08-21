package com.ingot.framework.security.oauth2.server.authorization.session.concurrency;

import com.ingot.framework.security.core.userdetails.InUser;

/**
 * <p>并发策略总开关关闭时的兜底来源，只保留 Client 的 {@code UNIQUE / STANDARD} 语义。</p>
 *
 * <p>返回无限约束，因此执行面只会应用「Client 配置为单会话则缺省 N=1」这一条规则，
 * 与本闭环上线前的现网行为完全一致。它既不读配置也不发远端调用，是紧急回退时最不可能失败的实现。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see SessionConcurrencyRule#unlimited()
 */
public class ClientOnlySessionConcurrencyPolicyResolver implements SessionConcurrencyPolicyResolver {

    @Override
    public SessionConcurrencyRule resolve(InUser user) {
        return SessionConcurrencyRule.unlimited();
    }
}
