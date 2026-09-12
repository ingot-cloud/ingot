package com.ingot.cloud.pms.api.model.constants;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeptBindingConstantsTest {

    @Test
    void nullDeptShouldMapToSentinelAndStayTenantLevel() {
        assertEquals(DeptBindingConstants.NULL_DEPT_SENTINEL, DeptBindingConstants.normalizeDeptId(null));
        assertTrue(DeptBindingConstants.isTenantLevel(DeptBindingConstants.NULL_DEPT_SENTINEL));
    }

    @Test
    void realDeptShouldKeepIdentity() {
        assertEquals(1171517787697836033L, DeptBindingConstants.normalizeDeptId(1171517787697836033L));
        assertFalse(DeptBindingConstants.isTenantLevel(1171517787697836033L));
    }
}
