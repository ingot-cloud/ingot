package com.ingot.framework.data.mybatis.scope.config;

import java.util.List;
import java.util.Set;

import com.ingot.cloud.pms.api.model.dto.authorization.AuthorizationResourceRuleDTO;
import com.ingot.cloud.pms.api.model.dto.authorization.AuthorizationSnapshotDTO;
import com.ingot.framework.data.mybatis.common.model.DataScopeTypeEnum;
import com.ingot.framework.data.mybatis.scope.context.DataScopeContextHolder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataScopeAOPTest {

    @Test
    void missingRuleIsEmptyFrameNotDenied() {
        AuthorizationSnapshotDTO snapshot = new AuthorizationSnapshotDTO();
        snapshot.setPermissionCodes(Set.of("demo:order:query"));
        DataScopeContextHolder.Frame frame = DataScopeAOP.buildFrame(
                "order", "demo:order:query", snapshot, 10L);
        assertFalse(frame.skip());
        assertTrue(frame.deptIds().isEmpty());
        assertNull(frame.userId());
    }

    @Test
    void missingPermissionCodesStillUsesMatchingRule() {
        AuthorizationSnapshotDTO snapshot = snapshot("order", "demo:order:query",
                DataScopeTypeEnum.ALL, List.of(), false);
        snapshot.setPermissionCodes(Set.of());
        DataScopeContextHolder.Frame frame = DataScopeAOP.buildFrame(
                "order", "demo:order:query", snapshot, 10L);
        assertTrue(frame.skip());
    }

    @Test
    void selfAndDeptMerge() {
        AuthorizationSnapshotDTO snapshot = snapshot("order", "demo:order:query",
                DataScopeTypeEnum.CUSTOM, List.of(3L, 5L), true);
        DataScopeContextHolder.Frame frame = DataScopeAOP.buildFrame(
                "order", "demo:order:query", snapshot, 10L);
        assertFalse(frame.skip());
        assertEquals(List.of(3L, 5L), frame.deptIds());
        assertEquals(10L, frame.userId());
    }

    private static AuthorizationSnapshotDTO snapshot(String resource,
                                                     String permission,
                                                     DataScopeTypeEnum type,
                                                     List<Long> deptIds,
                                                     boolean self) {
        AuthorizationSnapshotDTO snapshot = new AuthorizationSnapshotDTO();
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
