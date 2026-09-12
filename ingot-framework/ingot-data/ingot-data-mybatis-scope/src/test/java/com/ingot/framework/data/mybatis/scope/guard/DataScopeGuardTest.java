package com.ingot.framework.data.mybatis.scope.guard;

import java.util.List;

import com.ingot.cloud.pms.api.model.dto.authorization.AuthorizationResourceRuleDTO;
import com.ingot.cloud.pms.api.model.dto.authorization.AuthorizationSnapshotDTO;
import com.ingot.framework.data.mybatis.common.model.DataScopeTypeEnum;
import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationSnapshotHolder;
import com.ingot.framework.data.mybatis.scope.error.DataScopeException;
import com.ingot.framework.security.core.userdetails.InUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>写归属校验：ALL 放行，SELF 与部门并集，无规则拒绝。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class DataScopeGuardTest {

    @AfterEach
    void tearDown() {
        AuthorizationSnapshotHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void allAllowsAnyTarget() {
        bindUser(10L, 1L);
        AuthorizationSnapshotHolder.set(snapshot("order", "demo:order:update", DataScopeTypeEnum.ALL, List.of(), false));
        assertDoesNotThrow(() -> DataScopeGuard.assertWritable("order", "demo:order:update",
                DataScopeTarget.builder().deptId(99L).userId(2L).build()));
    }

    @Test
    void selfOrDeptUnion() {
        bindUser(10L, 1L);
        AuthorizationSnapshotHolder.set(snapshot("order", "demo:order:update", DataScopeTypeEnum.CUSTOM, List.of(3L), true));
        assertDoesNotThrow(() -> DataScopeGuard.assertWritable("order", "demo:order:update",
                DataScopeTarget.builder().userId(10L).deptId(8L).build()));
        assertDoesNotThrow(() -> DataScopeGuard.assertWritable("order", "demo:order:update",
                DataScopeTarget.builder().userId(2L).deptId(3L).build()));
        assertThrows(AuthorizationDeniedException.class, () -> DataScopeGuard.assertWritable("order", "demo:order:update",
                DataScopeTarget.builder().userId(2L).deptId(8L).build()));
    }

    @Test
    void missingRuleForbidden() {
        bindUser(10L, 1L);
        AuthorizationSnapshotDTO snapshot = new AuthorizationSnapshotDTO();
        snapshot.setPermissionCodes(java.util.Set.of("demo:order:update"));
        AuthorizationSnapshotHolder.set(snapshot);
        assertThrows(DataScopeException.class, () -> DataScopeGuard.assertWritable("order", "demo:order:update",
                DataScopeTarget.builder().userId(10L).build()));
    }

    @Test
    void missingPermissionCodesDoesNotDenyWhenRuleMatches() {
        bindUser(10L, 1L);
        AuthorizationSnapshotDTO snapshot = snapshot("order", "demo:order:update", DataScopeTypeEnum.ALL, List.of(), false);
        snapshot.setPermissionCodes(java.util.Set.of());
        AuthorizationSnapshotHolder.set(snapshot);
        assertDoesNotThrow(() -> DataScopeGuard.assertWritable("order", "demo:order:update",
                DataScopeTarget.builder().deptId(99L).userId(2L).build()));
    }

    private static void bindUser(long userId, long tenantId) {
        InUser user = InUser.stateless(userId, tenantId, "client", "standard", "ADMIN", "u",
                List.of(), List.of(), java.util.Map.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, "N/A", user.getAuthorities()));
    }

    private static AuthorizationSnapshotDTO snapshot(String resource,
                                                     String permission,
                                                     DataScopeTypeEnum type,
                                                     List<Long> deptIds,
                                                     boolean self) {
        AuthorizationSnapshotDTO snapshot = new AuthorizationSnapshotDTO();
        snapshot.setPermissionCodes(java.util.Set.of(permission));
        AuthorizationResourceRuleDTO rule = new AuthorizationResourceRuleDTO();
        rule.setResourceCode(resource);
        rule.setPermissionCode(permission);
        rule.setScopeType(type);
        rule.setDeptIds(deptIds);
        rule.setSelf(self);
        snapshot.setResourceRules(List.of(rule));
        return snapshot;
    }
}
