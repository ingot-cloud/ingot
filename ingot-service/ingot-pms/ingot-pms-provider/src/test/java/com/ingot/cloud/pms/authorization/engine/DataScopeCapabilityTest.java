package com.ingot.cloud.pms.authorization.engine;

import java.util.List;

import com.ingot.framework.data.mybatis.common.model.DataScopeTypeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataScopeCapabilityTest {

    @Test
    void selfOfGrantorDoesNotCoverAnotherUser() {
        DataScopeCapability capability = new DataScopeCapability();
        capability.mergeSelf();
        assertTrue(capability.covers(DataScopeTypeEnum.SELF, List.of(), null, List.of(), 1L, List.of(1L)));
        assertFalse(capability.covers(DataScopeTypeEnum.SELF, List.of(), 10L, List.of(10L), 1L, List.of(2L)));
        assertFalse(capability.covers(DataScopeTypeEnum.SELF, List.of(), null, List.of(), 1L, List.of()));
    }

    @Test
    void deptAndChildMustCoverEntireTree() {
        DataScopeCapability capability = new DataScopeCapability();
        capability.mergeDepts(List.of(10L, 11L));
        assertTrue(capability.covers(DataScopeTypeEnum.DEPT, List.of(), 10L, List.of(), 1L, List.of(2L)));
        assertTrue(capability.covers(DataScopeTypeEnum.DEPT_AND_CHILD, List.of(), 10L, List.of(10L, 11L), 1L, List.of(2L)));
        assertFalse(capability.covers(DataScopeTypeEnum.DEPT_AND_CHILD, List.of(), 10L, List.of(10L, 11L, 12L), 1L, List.of(2L)));
        assertFalse(capability.covers(DataScopeTypeEnum.ALL, List.of(), null, List.of(), 1L, List.of(2L)));
    }
}
