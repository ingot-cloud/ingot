package com.ingot.cloud.iam.catalog;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>验证目录删除说明会列出全部挡住删除的引用。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class CatalogInUseTest {
    @Test
    void requireApplicationUnusedAllowsEmptyCatalog() {
        assertDoesNotThrow(() -> CatalogInUse.requireApplicationUnused(0, 0, 0, 0));
    }

    @Test
    void requireApplicationUnusedListsOwnedAndReferenced() {
        BizException inUse = assertThrows(BizException.class,
                () -> CatalogInUse.requireApplicationUnused(1, 1, 1, 1));
        assertEquals(IamReasonCode.OBJECT_IN_USE.getCode(), inUse.getCode());
        assertEquals("无法删除应用：下仍有资源、菜单，且仍被组织开通、套餐引用", inUse.getMessage());
    }
}
