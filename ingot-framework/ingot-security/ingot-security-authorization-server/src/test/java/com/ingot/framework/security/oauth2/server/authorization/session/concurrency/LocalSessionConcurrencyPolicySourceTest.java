package com.ingot.framework.security.oauth2.server.authorization.session.concurrency;

import java.util.List;

import com.ingot.cloud.security.api.model.vo.policy.SessionConcurrencyPolicyVO;
import com.ingot.framework.commons.model.security.SessionOverflowStrategy;
import com.ingot.framework.commons.model.security.SessionPolicyScope;
import com.ingot.framework.commons.model.security.TokenAuthTypeEnum;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.InSecurityProperties;
import com.ingot.framework.security.core.userdetails.InUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 本地并发策略来源与 Nacos 地板的行为：地板不为空，且改配置后无需重启即生效。
 *
 * @author jy
 * @since 1.0.0
 */
class LocalSessionConcurrencyPolicySourceTest {

    private final InSecurityProperties properties = new InSecurityProperties();

    @Test
    void localResolver_defaultsToUnlimitedKickOldest() {
        SessionConcurrencyRule rule = new LocalSessionConcurrencyPolicyResolver(properties).resolve(user());

        assertEquals(0, rule.maxSessions());
        assertEquals(SessionOverflowStrategy.KICK_OLDEST, rule.overflow());
        assertFalse(rule.adminForbidConcurrent());
    }

    @Test
    void localResolver_readsPropertiesOnEveryResolve() {
        LocalSessionConcurrencyPolicyResolver resolver = new LocalSessionConcurrencyPolicyResolver(properties);
        assertEquals(0, resolver.resolve(user()).maxSessions());

        // 模拟 Nacos 刷新后配置对象被重新绑定
        properties.getSession().getConcurrency().setMaxSessions(1);
        properties.getSession().getConcurrency().setOverflow(SessionOverflowStrategy.REJECT);

        SessionConcurrencyRule rule = resolver.resolve(user());
        assertEquals(1, rule.maxSessions());
        assertEquals(SessionOverflowStrategy.REJECT, rule.overflow());
    }

    @Test
    void floorSupplier_alwaysReturnsOneGlobalPolicy() {
        properties.getSession().getConcurrency().setMaxSessions(2);
        properties.getSession().getConcurrency().setAdminForbidConcurrent(true);

        List<SessionConcurrencyPolicyVO> floor =
                new SessionConcurrencyPolicyFloorSupplier(properties).get("all");

        assertEquals(1, floor.size());
        SessionConcurrencyPolicyVO vo = floor.getFirst();
        assertEquals(SessionPolicyScope.GLOBAL, vo.getScope());
        assertEquals(2, vo.getMaxSessions());
        assertTrue(vo.getAdminForbidConcurrent());
        assertTrue(vo.getEnabled());
    }

    @Test
    void clientOnlyResolver_ignoresConfiguredPolicy() {
        properties.getSession().getConcurrency().setMaxSessions(1);

        SessionConcurrencyRule rule = new ClientOnlySessionConcurrencyPolicyResolver().resolve(user());

        assertEquals(0, rule.maxSessions());
    }

    private static InUser user() {
        return InUser.stateless(9L, 1L, "web", TokenAuthTypeEnum.STANDARD.getValue(),
                UserTypeEnum.ADMIN.getValue(), "admin", List.of(), List.of(), null);
    }
}
