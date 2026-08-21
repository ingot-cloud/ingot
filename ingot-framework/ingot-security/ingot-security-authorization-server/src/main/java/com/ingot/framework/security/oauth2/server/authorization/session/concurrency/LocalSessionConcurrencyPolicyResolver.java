package com.ingot.framework.security.oauth2.server.authorization.session.concurrency;

import com.ingot.framework.commons.model.security.SessionConcurrencyDimension;
import com.ingot.framework.commons.model.security.SessionOverflowStrategy;
import com.ingot.framework.security.core.InSecurityProperties;
import com.ingot.framework.security.core.userdetails.InUser;
import lombok.RequiredArgsConstructor;

/**
 * <p>本地并发策略来源，直接读 Nacos 下发的 {@code ingot.security.session.concurrency.*}。</p>
 *
 * <p>未部署安全中心的环境用它承担全部策略能力，因此不区分租户与 Client —— 一套参数全局生效。
 * 每次解析都重新读配置对象，配合 Spring Cloud 的 {@code @ConfigurationProperties} 重绑定，
 * 改 Nacos 后下一笔登录即按新值执行，无需重启。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see InSecurityProperties.Concurrency
 */
@RequiredArgsConstructor
public class LocalSessionConcurrencyPolicyResolver implements SessionConcurrencyPolicyResolver {

    private final InSecurityProperties properties;

    @Override
    public SessionConcurrencyRule resolve(InUser user) {
        InSecurityProperties.Concurrency concurrency = properties.getSession().getConcurrency();
        return new SessionConcurrencyRule(
                Math.max(concurrency.getMaxSessions(), 0),
                concurrency.getDimension() != null
                        ? concurrency.getDimension() : SessionConcurrencyDimension.USER_CLIENT,
                concurrency.getOverflow() != null
                        ? concurrency.getOverflow() : SessionOverflowStrategy.KICK_OLDEST,
                concurrency.isAdminForbidConcurrent());
    }
}
