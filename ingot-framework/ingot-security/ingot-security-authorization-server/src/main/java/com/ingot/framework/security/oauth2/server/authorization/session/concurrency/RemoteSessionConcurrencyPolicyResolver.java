package com.ingot.framework.security.oauth2.server.authorization.session.concurrency;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.security.api.model.vo.policy.SessionConcurrencyPolicyVO;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.cache.spi.RemoteUnavailableException;
import com.ingot.framework.commons.model.security.SessionConcurrencyDimension;
import com.ingot.framework.commons.model.security.SessionOverflowStrategy;
import com.ingot.framework.commons.model.security.SessionPolicyScope;
import com.ingot.framework.security.core.userdetails.InUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>远端并发策略来源，从分层缓存读取安全中心策略并按范围由窄到宽选取命中记录。</p>
 *
 * <p>缓存链已包含 {@code remote → LKG → Nacos 地板}，因此本类只做「选哪一条」：
 * {@link SessionPolicyScope#CLIENT} 优于 {@link SessionPolicyScope#USER_TYPE} 优于
 * {@link SessionPolicyScope#GLOBAL}，命中即止，不做字段级合并 —— 合并会让管理员难以从页面
 * 推断某个 Client 的实际生效值。</p>
 *
 * <p>策略表为空是合法状态，按无限并发处理；只有缓存链耗尽全部兜底才抛
 * {@link SessionPolicyUnavailableException} 让登录 fail-closed。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see SessionConcurrencyPolicyFloorSupplier
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteSessionConcurrencyPolicyResolver implements SessionConcurrencyPolicyResolver {

    /**
     * 策略为单 key 全量快照，缓存键固定。
     */
    public static final String CACHE_KEY = "all";

    private final LayeredCache<String, List<SessionConcurrencyPolicyVO>> policyCache;

    @Override
    public SessionConcurrencyRule resolve(InUser user) {
        List<SessionConcurrencyPolicyVO> policies;
        try {
            policies = policyCache.get(CACHE_KEY);
        } catch (RemoteUnavailableException e) {
            throw new SessionPolicyUnavailableException(
                    "[SessionConcurrency] 策略不可用且无 LKG / 地板兜底", e);
        }
        return select(policies, user)
                .map(RemoteSessionConcurrencyPolicyResolver::toRule)
                .orElseGet(SessionConcurrencyRule::unlimited);
    }

    /**
     * 按 scope 由窄到宽选取命中策略；同 scope 多条时取 ID 最小者，保证结果稳定。
     */
    private Optional<SessionConcurrencyPolicyVO> select(List<SessionConcurrencyPolicyVO> policies, InUser user) {
        if (policies == null || policies.isEmpty()) {
            return Optional.empty();
        }
        List<SessionConcurrencyPolicyVO> enabled = policies.stream()
                .filter(p -> p != null && !Boolean.FALSE.equals(p.getEnabled()))
                .toList();

        return firstMatch(enabled, p -> p.getScope() == SessionPolicyScope.CLIENT
                && StrUtil.equals(p.getClientId(), user.getClientId()))
                .or(() -> firstMatch(enabled, p -> p.getScope() == SessionPolicyScope.USER_TYPE
                        && StrUtil.equals(p.getUserType(), user.getUserType())))
                .or(() -> firstMatch(enabled, p -> p.getScope() == SessionPolicyScope.GLOBAL));
    }

    private Optional<SessionConcurrencyPolicyVO> firstMatch(
            List<SessionConcurrencyPolicyVO> policies,
            Predicate<SessionConcurrencyPolicyVO> predicate) {
        return policies.stream()
                .filter(predicate)
                .min(Comparator.comparing(p -> p.getId() != null ? p.getId() : Long.MAX_VALUE));
    }

    private static SessionConcurrencyRule toRule(SessionConcurrencyPolicyVO vo) {
        return new SessionConcurrencyRule(
                vo.getMaxSessions() != null ? Math.max(vo.getMaxSessions(), 0) : 0,
                vo.getDimension() != null ? vo.getDimension() : SessionConcurrencyDimension.USER_CLIENT,
                vo.getOverflow() != null ? vo.getOverflow() : SessionOverflowStrategy.KICK_OLDEST,
                Boolean.TRUE.equals(vo.getAdminForbidConcurrent()));
    }
}
