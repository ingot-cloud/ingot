package com.ingot.framework.security.oauth2.server.authorization.session;

import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.server.authorization.OnlineSessionRegistration;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import com.ingot.framework.security.oauth2.server.authorization.session.concurrency.SessionConcurrencyEnforcer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>会话注册入口，负责在签发链上落地会话并执行并发会话约束。</p>
 *
 * <p>把「并发约束 + 落库新会话」收在此处，使
 * {@link com.ingot.framework.security.oauth2.server.authorization.token.JwtOAuth2TokenCustomizer}
 * 只关心 JWT 声明本身。约束判定与超限处置全部委托
 * {@link SessionConcurrencyEnforcer}，本类只决定「是否需要判定」。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see SessionConcurrencyEnforcer
 */
@Slf4j
@RequiredArgsConstructor
public class SessionRegistrar {

    private final OnlineTokenService onlineTokenService;
    private final SessionConcurrencyEnforcer concurrencyEnforcer;

    /**
     * 注册会话；新登录先执行并发约束，refresh 换发直接续期。
     *
     * <p>refresh 沿用同一 sid，会话数不增加，因此跳过约束：既省掉一次策略读取，也避免策略来源
     * 短暂故障时把在线用户连带踢下线 —— fail-closed 的代价只应由新登录承担。</p>
     *
     * @param user         已绑定目标租户或成员上下文的登录用户
     * @param registration 本次签发的会话标识与时效
     * @throws org.springframework.security.oauth2.core.OAuth2AuthenticationException
     *         并发策略要求拒绝本次登录，或策略不可用需 fail-closed
     */
    public void register(InUser user, OnlineSessionRegistration registration) {
        boolean renewal = onlineTokenService.isOnlineSid(registration.sid());
        if (!renewal) {
            concurrencyEnforcer.enforce(user, registration.sid());
        }
        onlineTokenService.save(user, registration);
        log.debug("[SessionRegistrar] 会话已注册: sid={}, userId={}, renewal={}",
                registration.sid(), user.getId(), renewal);
    }
}
