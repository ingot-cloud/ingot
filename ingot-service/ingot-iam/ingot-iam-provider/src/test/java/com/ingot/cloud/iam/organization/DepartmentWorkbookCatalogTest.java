package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.util.List;

import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.DepartmentWorkbookReasons;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>验证部门路径解析、同级重名、环检测与导出树前序。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class DepartmentWorkbookCatalogTest {
    @Test
    void formatAndParseKeepRootFirst() {
        DepartmentWorkbookCatalog catalog = new DepartmentWorkbookCatalog(List.of(
                department(1, null, "英格特云", 0),
                department(2, 1L, "研发部", 1),
                department(3, 1L, "产品部", 2)));
        assertEquals("英格特云", catalog.format("1"));
        assertEquals("英格特云/研发部", catalog.format("2"));
        assertEquals("1", catalog.requireParent("英格特云"));
        assertEquals("2", catalog.requireParent("英格特云/研发部"));
    }

    @Test
    void unknownAndAmbiguousPathsFail() {
        DepartmentWorkbookCatalog catalog = new DepartmentWorkbookCatalog(List.of(
                department(1, null, "集团", 0),
                department(2, 1L, "研发", 1),
                department(3, 1L, "研发", 2)));
        BizException missing = assertThrows(BizException.class, () -> catalog.requireParent("集团/不存在"));
        assertEquals(DepartmentWorkbookReasons.PARENT_NOT_FOUND, missing.getMessage());
        BizException ambiguous = assertThrows(BizException.class, () -> catalog.requireParent("集团/研发"));
        assertEquals(DepartmentWorkbookReasons.PARENT_AMBIGUOUS, ambiguous.getMessage());
    }

    @Test
    void siblingTakenIncludesRegisteredRow() {
        DepartmentWorkbookCatalog catalog = new DepartmentWorkbookCatalog(List.of(
                department(1, null, "英格特云", 0),
                department(2, 1L, "研发部", 1)));
        assertTrue(catalog.siblingTaken("1", "研发部", null));
        assertFalse(catalog.siblingTaken("1", "研发部", "2"));
        catalog.register(department(9, 1L, "新产品", 3));
        assertTrue(catalog.siblingTaken("1", "新产品", null));
        assertEquals("9", catalog.requireParent("英格特云/新产品"));
    }

    @Test
    void wouldCycleWhenParentIsDescendant() {
        DepartmentWorkbookCatalog catalog = new DepartmentWorkbookCatalog(List.of(
                department(1, null, "英格特云", 0),
                department(2, 1L, "研发部", 1),
                department(3, 2L, "后端组", 1)));
        assertTrue(catalog.wouldCycle("1", "3"));
        assertTrue(catalog.wouldCycle("2", "2"));
        assertFalse(catalog.wouldCycle("3", "1"));
    }

    @Test
    void exportRowsWalkPreorderAndExpandSelection() {
        DepartmentWorkbookCatalog catalog = new DepartmentWorkbookCatalog(List.of(
                department(1, null, "英格特云", 0),
                department(3, 1L, "产品部", 2),
                department(2, 1L, "研发部", 1),
                department(4, 2L, "后端组", 1)));
        List<IamDepartmentEntity> all = catalog.exportRows(List.of());
        assertEquals(List.of("1", "2", "4", "3"), all.stream().map(row -> row.getId().toString()).toList());
        List<IamDepartmentEntity> partial = catalog.exportRows(List.of("2"));
        assertEquals(List.of("2", "4"), partial.stream().map(row -> row.getId().toString()).toList());
    }

    private static IamDepartmentEntity department(long id, Long parentId, String name, int sortOrder) {
        IamDepartmentEntity row = new IamDepartmentEntity();
        row.setId(BigInteger.valueOf(id));
        row.setParentId(parentId == null ? null : BigInteger.valueOf(parentId));
        row.setName(name);
        row.setSortOrder(sortOrder);
        return row;
    }
}
