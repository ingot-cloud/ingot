package com.ingot.framework.data.mybatis.scope.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>嵌套数据范围上下文栈必须 push/pop 恢复外层，且空范围不解释为 ALL。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class DataScopeContextHolderTest {

    @AfterEach
    void tearDown() {
        DataScopeContextHolder.clear();
    }

    @Test
    void nestedFramesRestoreOuter() {
        DataScopeContextHolder.push(DataScopeContextHolder.Frame.of("order", "demo:order:query", false, List.of(1L), null));
        DataScopeContextHolder.push(DataScopeContextHolder.Frame.of("notice", "demo:notice:query", true, List.of(), null));
        assertTrue(DataScopeContextHolder.isSkip());
        assertEquals("notice", DataScopeContextHolder.getResource());
        DataScopeContextHolder.pop();
        assertFalse(DataScopeContextHolder.isSkip());
        assertEquals(List.of(1L), DataScopeContextHolder.getScopes());
        assertEquals("order", DataScopeContextHolder.getResource());
        DataScopeContextHolder.pop();
        assertNull(DataScopeContextHolder.current());
    }

    @Test
    void emptyFrameIsNotSkip() {
        DataScopeContextHolder.push(DataScopeContextHolder.Frame.of("order", "demo:order:query", false, List.of(), null));
        assertTrue(DataScopeContextHolder.isEmpty());
        assertFalse(DataScopeContextHolder.isSkip());
        DataScopeContextHolder.pop();
    }
}
