package com.ingot.framework.security.oauth2.server.authorization.session.concurrency;

import java.util.List;

import com.ingot.cloud.security.api.model.vo.policy.SessionConcurrencyPolicyVO;
import com.ingot.framework.cache.spi.CacheFloorSupplier;
import com.ingot.framework.commons.model.security.SessionConcurrencyDimension;
import com.ingot.framework.commons.model.security.SessionOverflowStrategy;
import com.ingot.framework.commons.model.security.SessionPolicyScope;
import com.ingot.framework.security.core.InSecurityProperties;
import lombok.RequiredArgsConstructor;

/**
 * <p>降级末级的地板供给器，把 Nacos 的并发参数包装成一条 {@code GLOBAL} 策略。</p>
 *
 * <p>返回值形态与远端一致，使解析逻辑对「数据来自安全中心还是地板」完全无感。
 * 始终返回恰好一条记录，不会为空 —— 地板存在的意义就是保证远端与 LKG 都失效时仍有可执行策略。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see InSecurityProperties.Concurrency
 */
@RequiredArgsConstructor
public class SessionConcurrencyPolicyFloorSupplier
        implements CacheFloorSupplier<String, List<SessionConcurrencyPolicyVO>> {

    private final InSecurityProperties properties;

    @Override
    public List<SessionConcurrencyPolicyVO> get(String key) {
        InSecurityProperties.Concurrency concurrency = properties.getSession().getConcurrency();
        SessionConcurrencyPolicyVO vo = new SessionConcurrencyPolicyVO();
        vo.setScope(SessionPolicyScope.GLOBAL);
        vo.setClientId("");
        vo.setUserType("");
        vo.setMaxSessions(Math.max(concurrency.getMaxSessions(), 0));
        vo.setDimension(concurrency.getDimension() != null
                ? concurrency.getDimension() : SessionConcurrencyDimension.USER_CLIENT);
        vo.setOverflow(concurrency.getOverflow() != null
                ? concurrency.getOverflow() : SessionOverflowStrategy.KICK_OLDEST);
        vo.setAdminForbidConcurrent(concurrency.isAdminForbidConcurrent());
        vo.setEnabled(Boolean.TRUE);
        vo.setRemark("Nacos 地板策略");
        return List.of(vo);
    }
}
