package com.ingot.cloud.iam.role;

import java.util.List;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.RoleDelta;
import com.ingot.framework.commons.model.iam.RoleDeltaOperation;
import com.ingot.framework.commons.model.iam.RoleOrigin;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.ScopeKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>验证角色合成固定顺序：先 REMOVE 再 REPLACE_SCOPE 最后 ADD，冲突失败关闭。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class RoleSynthesisTest {
    private static final ScopeExpression ALL = new ScopeExpression(ScopeKind.ALL, null, null);

    @Test
    void removeThenAddKeepsOnlyAddedAction() {
        RoleSynthesis.Result result = RoleSynthesis.synthesize(
                List.of(new ActionGrant("1", List.of(ALL)), new ActionGrant("2", List.of(ALL))),
                List.of(new RoleDelta("1", RoleDeltaOperation.REMOVE, List.of()),
                        new RoleDelta("3", RoleDeltaOperation.ADD, List.of(ALL))));
        assertEquals(List.of("2", "3"), result.grants().stream().map(ActionGrant::actionId).toList());
        assertTrue(result.origins().stream().anyMatch(origin -> origin.actionId().equals("1")
                && origin.origin() == RoleOrigin.REMOVED));
    }

    @Test
    void removeMissingActionIsConflict() {
        BizException failure = assertThrows(BizException.class, () -> RoleSynthesis.synthesize(
                List.of(new ActionGrant("1", List.of(ALL))),
                List.of(new RoleDelta("9", RoleDeltaOperation.REMOVE, List.of()))));
        assertEquals(IamReasonCode.POLICY_CONFLICT.getCode(), failure.getCode());
    }

    @Test
    void addExistingActionIsConflict() {
        BizException failure = assertThrows(BizException.class, () -> RoleSynthesis.synthesize(
                List.of(new ActionGrant("1", List.of(ALL))),
                List.of(new RoleDelta("1", RoleDeltaOperation.ADD, List.of(ALL)))));
        assertEquals(IamReasonCode.POLICY_CONFLICT.getCode(), failure.getCode());
    }
}
