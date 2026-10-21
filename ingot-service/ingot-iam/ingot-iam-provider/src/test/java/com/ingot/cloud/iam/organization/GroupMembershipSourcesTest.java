package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ingot.framework.commons.model.iam.GroupMembershipDepartment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>验证有效成员来源标记：直接绑定、含下级与不可见部门名称省略。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class GroupMembershipSourcesTest {
    @Test
    void marksDirectAndDepartmentSourcesAndOmitsHiddenNames() {
        var parent = id(11);
        var child = id(12);
        var member = id(101);
        var extra = id(102);
        var result = GroupMembershipSources.resolve(List.of(member, extra), Set.of(member),
                List.of(new GroupMembershipSources.Rule(parent, true, Set.of(parent, child))),
                Map.of(member, List.of(child), extra, List.of(parent)), Map.of());
        assertTrue(result.get(member).direct());
        assertEquals(List.of(new GroupMembershipDepartment("11", null, true)), result.get(member).departments());
        assertFalse(result.get(extra).direct());
        assertEquals("11", result.get(extra).departments().getFirst().id());
        assertNull(result.get(extra).departments().getFirst().name());
    }

    @Test
    void keepsRuleNameWhenVisibleAndDoesNotMatchUnrelatedDepartment() {
        var research = id(11);
        var other = id(21);
        var member = id(101);
        var result = GroupMembershipSources.resolve(List.of(member), Set.of(),
                List.of(new GroupMembershipSources.Rule(research, false, Set.of(research)),
                        new GroupMembershipSources.Rule(other, false, Set.of(other))),
                Map.of(member, List.of(research)), Map.of(research, "研发"));
        assertEquals(List.of(new GroupMembershipDepartment("11", "研发", false)), result.get(member).departments());
    }

    private static BigInteger id(long value) {
        return BigInteger.valueOf(value);
    }
}
