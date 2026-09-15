package com.ingot.cloud.iam.role;

import java.util.List;

import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangedSpringEvent;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.RoleDelta;
import com.ingot.framework.commons.model.iam.RoleDeltaOperation;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.ScopeKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * <p>验证角色合成派生缓存按来源加修订指纹复用，失效后重编译。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class RoleSynthesisCacheTest {
    @Test
    void reusesCompiledGrantsUntilFingerprintOrInvalidationChanges() {
        RoleSynthesisCache cache = new RoleSynthesisCache();
        RoleRevisionSnapshot first = new RoleRevisionSnapshot(50L, 31L,
                List.of(new ActionGrant("11", List.of(new ScopeExpression(ScopeKind.ALL, null, null)))),
                List.of());
        List<ActionGrant> compiled = cache.grants(first);
        assertEquals(1, compiled.size());
        assertSame(compiled, cache.grants(first));
        RoleRevisionSnapshot changed = new RoleRevisionSnapshot(50L, 31L,
                List.of(new ActionGrant("11", List.of(new ScopeExpression(ScopeKind.ALL, null, null)))),
                List.of(new RoleDelta("11", RoleDeltaOperation.REMOVE, List.of())));
        List<ActionGrant> next = cache.grants(changed);
        assertNotSame(compiled, next);
        assertEquals(0, next.size());
        cache.onAuthorizationChanged(AuthorizationChangedSpringEvent.all(this));
        List<ActionGrant> rebuilt = cache.grants(first);
        assertNotSame(compiled, rebuilt);
        assertEquals(1, rebuilt.size());
    }
}
