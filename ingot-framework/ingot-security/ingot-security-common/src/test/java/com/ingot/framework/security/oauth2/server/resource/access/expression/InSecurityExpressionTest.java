package com.ingot.framework.security.oauth2.server.resource.access.expression;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.server.resource.authentication.InJwtAuthenticationConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>接口注解必须使用在线授权，旧令牌角色、租户超管标记和依赖故障不能放行。</p>
 * @author jy
 * @since 1.0.0
 */
class InSecurityExpressionTest {
    private static final String ACTION = "iam-platform:member:read";

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void onlineRevocationWinsOverJwtAdminAndBusinessAuthorities() {
        authenticate(AuthorizationDomain.PLATFORM);
        var values = new AtomicReference<Set<String>>(Set.of(RoleConstants.ROLE_ADMIN_CODE));
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("authorities", (TrustedAuthoritySource) values::get);
        var expression = new InSecurityExpression(beans.getBeanProvider(TrustedAuthoritySource.class));
        assertTrue(expression.adminOrHasAnyAuthority(ACTION));
        values.set(Set.of(ACTION));
        assertFalse(expression.requiredAdmin());
        assertTrue(expression.hasAuthority(ACTION));
        values.set(Set.of());
        assertFalse(expression.adminOrHasAnyAuthority(ACTION));
        assertFalse(expression.hasAuthority(ACTION));
    }

    @Test
    void missingProviderAndProviderFailureNeverFallBackToJwt() {
        authenticate(AuthorizationDomain.PLATFORM);
        assertThrows(AuthorizationDeniedException.class, () -> new InSecurityExpression().requiredAdmin());
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("authorities", (TrustedAuthoritySource) () -> { throw new IllegalStateException("offline"); });
        assertThrows(IllegalStateException.class, () -> new InSecurityExpression(
                beans.getBeanProvider(TrustedAuthoritySource.class)).adminOrHasAnyAuthority(ACTION));
    }

    @Test
    void tenantContextCannotUsePlatformAdministratorEvenIfSnapshotContainsMarker() {
        authenticate(AuthorizationDomain.TENANT);
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("authorities", (TrustedAuthoritySource) () -> Set.of(RoleConstants.ROLE_ADMIN_CODE));
        assertFalse(new InSecurityExpression(beans.getBeanProvider(TrustedAuthoritySource.class)).requiredAdmin());
    }

    private void authenticate(AuthorizationDomain domain) {
        Long tenant = domain == AuthorizationDomain.TENANT ? 10L : null;
        var context = new AuthorizationContext(domain, tenant == null ? null : tenant.toString(), "1", "2");
        var user = InUser.stateless(1L, tenant, "web", "standard", "0", "user", List.of(), List.of(), Map.of())
                .toBuilder().authorizationContext(context).build();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,
                "unused", List.of(new SimpleGrantedAuthority(InJwtAuthenticationConverter.AUTHORITY_PREFIX + RoleConstants.ROLE_ADMIN_CODE),
                        new SimpleGrantedAuthority(InJwtAuthenticationConverter.AUTHORITY_PREFIX + ACTION))));
    }
}
