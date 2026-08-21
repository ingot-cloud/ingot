package com.ingot.framework.security.oauth2.server.authorization.session.concurrency;

import java.util.List;

import com.ingot.framework.commons.model.security.SessionRevokeReason;
import com.ingot.framework.commons.model.security.TokenAuthTypeEnum;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorCodesExtension;
import com.ingot.framework.security.oauth2.server.authorization.OnlineToken;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import com.ingot.framework.security.oauth2.server.authorization.session.SessionRevocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

/**
 * <p>并发会话约束的执行面：登录落库前把在线会话数压到策略允许的范围内。</p>
 *
 * <p>只在新登录时调用，refresh 换发不触发 —— 换发沿用同一 sid，会话数不增加，且续期不应因
 * 策略来源故障而失败。超限时按策略踢除旧会话（完整撤销，含 Refresh Token）或拒绝本次登录。</p>
 *
 * <p>三条约束叠加后得到实际上限：策略显式的 {@code maxSessions} 优先；策略为无限时，
 * Client 配置为 {@link TokenAuthTypeEnum#UNIQUE} 缺省按单会话处理；
 * {@code adminForbidConcurrent} 对管理用户强制单会话，优先级最高。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see SessionConcurrencyPolicyResolver
 * @apiNote 拒绝登录与策略不可用都以 {@link OAuth2AuthenticationException} 上抛，
 *          由 Token 端点转成标准 OAuth2 错误响应，分别对应
 *          {@code concurrent_session_limit} 与 {@code session_policy_unavailable}。
 */
@Slf4j
@RequiredArgsConstructor
public class SessionConcurrencyEnforcer {

    private final OnlineTokenService onlineTokenService;
    private final SessionRevocationService sessionRevocationService;
    private final SessionConcurrencyPolicyResolver policyResolver;

    /**
     * 执行并发约束。
     *
     * @param user       已切片到目标租户的登录用户
     * @param currentSid 本次登录的会话 ID
     * @throws OAuth2AuthenticationException 策略要求拒绝本次登录，或策略不可用需 fail-closed
     */
    public void enforce(InUser user, String currentSid) {
        SessionConcurrencyRule rule = resolveRule(user);
        int maxSessions = effectiveMaxSessions(user, rule);
        if (maxSessions <= 0) {
            return;
        }

        List<OnlineToken> others = otherSessions(user, currentSid);
        if (others.size() < maxSessions) {
            return;
        }

        switch (rule.overflow()) {
            case REJECT -> {
                log.warn("[SessionConcurrency] 会话数已达上限，拒绝新登录: userId={}, clientId={}, "
                                + "online={}, maxSessions={}",
                        user.getId(), user.getClientId(), others.size(), maxSessions);
                throw authenticationException(OAuth2ErrorCodesExtension.CONCURRENT_SESSION_LIMIT);
            }
            case KICK_ALL -> kick(user, others, maxSessions);
            case KICK_OLDEST -> kick(user, oldest(others, others.size() - maxSessions + 1), maxSessions);
        }
    }

    private SessionConcurrencyRule resolveRule(InUser user) {
        try {
            return policyResolver.resolve(user);
        } catch (SessionPolicyUnavailableException e) {
            log.error("[SessionConcurrency] 并发策略不可用，拒绝新登录: userId={}, clientId={}",
                    user.getId(), user.getClientId(), e);
            throw authenticationException(OAuth2ErrorCodesExtension.SESSION_POLICY_UNAVAILABLE);
        }
    }

    /**
     * 合成实际生效的会话数上限；返回 {@code 0} 表示无限。
     */
    private int effectiveMaxSessions(InUser user, SessionConcurrencyRule rule) {
        if (rule.adminForbidConcurrent()
                && UserTypeEnum.getEnum(user.getUserType()) == UserTypeEnum.ADMIN) {
            return 1;
        }
        int maxSessions = Math.max(rule.maxSessions(), 0);
        if (maxSessions == 0
                && TokenAuthTypeEnum.getEnum(user.getTokenAuthType()) == TokenAuthTypeEnum.UNIQUE) {
            return 1;
        }
        return maxSessions;
    }

    /**
     * 该维度下除本次登录之外仍然在线的会话，按创建时间倒序。
     */
    private List<OnlineToken> otherSessions(InUser user, String currentSid) {
        return onlineTokenService.listUserSessions(user.getTenantId(), user.getClientId(), user.getId())
                .stream()
                .filter(session -> session != null && !currentSid.equals(session.getSid()))
                .toList();
    }

    /**
     * 取最旧的若干会话；入参列表按创建时间倒序，最旧在尾部。
     */
    private List<OnlineToken> oldest(List<OnlineToken> sessions, int count) {
        if (count <= 0) {
            return List.of();
        }
        int size = sessions.size();
        return sessions.subList(Math.max(size - count, 0), size);
    }

    private void kick(InUser user, List<OnlineToken> victims, int maxSessions) {
        for (OnlineToken victim : victims) {
            log.info("[SessionConcurrency] 会话数超限，踢除旧会话: userId={}, clientId={}, "
                            + "victimSid={}, maxSessions={}",
                    user.getId(), user.getClientId(), victim.getSid(), maxSessions);
            sessionRevocationService.revokeBySid(victim.getSid(), SessionRevokeReason.CONCURRENT_KICKOUT);
        }
    }

    private OAuth2AuthenticationException authenticationException(OAuth2ErrorCodesExtension code) {
        return new OAuth2AuthenticationException(new OAuth2Error(code.getCode(), code.getText(), null));
    }
}
