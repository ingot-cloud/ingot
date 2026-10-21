package com.ingot.cloud.iam.organization;

import java.util.List;
import java.util.Map;

import com.ingot.cloud.iam.organization.DepartmentWorkbookExcel.DepartmentWorkbookSheet;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.DepartmentWorkbookColumns;
import com.ingot.framework.commons.model.iam.DepartmentWorkbookLimits;
import com.ingot.framework.commons.model.iam.DepartmentWorkbookReasons;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>验证部门表格读写能跳过须知行，并按部门名称定位表头。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class DepartmentWorkbookExcelTest {
    @Test
    void writeThenReadKeepsDataRows() {
        byte[] content = DepartmentWorkbookExcel.writeDepartments(
                List.of(DepartmentWorkbookColumns.NAME, DepartmentWorkbookColumns.PARENT,
                        DepartmentWorkbookColumns.SORT_ORDER),
                List.of(Map.of(DepartmentWorkbookColumns.NAME, "产品部",
                        DepartmentWorkbookColumns.PARENT, "英格特云",
                        DepartmentWorkbookColumns.SORT_ORDER, "1")),
                false);
        DepartmentWorkbookSheet sheet = DepartmentWorkbookExcel.read(content);
        assertTrue(sheet.has(DepartmentWorkbookColumns.NAME));
        assertFalse(sheet.has(DepartmentWorkbookColumns.DEPARTMENT_ID));
        assertEquals(1, sheet.rows().size());
        assertEquals("产品部", sheet.rows().getFirst().get(DepartmentWorkbookColumns.NAME));
        assertEquals("英格特云", sheet.rows().getFirst().get(DepartmentWorkbookColumns.PARENT));
        assertEquals(6, sheet.excelRow(0));
    }

    @Test
    void updateTemplateKeepsDepartmentId() {
        byte[] content = DepartmentWorkbookExcel.writeDepartments(
                List.of(DepartmentWorkbookColumns.DEPARTMENT_ID, DepartmentWorkbookColumns.NAME,
                        DepartmentWorkbookColumns.PARENT, DepartmentWorkbookColumns.SORT_ORDER),
                List.of(Map.of(DepartmentWorkbookColumns.DEPARTMENT_ID, "9",
                        DepartmentWorkbookColumns.NAME, "研发部",
                        DepartmentWorkbookColumns.PARENT, "英格特云")),
                true);
        DepartmentWorkbookSheet sheet = DepartmentWorkbookExcel.read(content);
        assertTrue(sheet.has(DepartmentWorkbookColumns.DEPARTMENT_ID));
        assertEquals("9", sheet.rows().getFirst().get(DepartmentWorkbookColumns.DEPARTMENT_ID));
    }

    @Test
    void missingHeaderFails() {
        byte[] content = DepartmentWorkbookExcel.write(List.of(List.of("不是表头")));
        BizException exception = assertThrows(BizException.class, () -> DepartmentWorkbookExcel.read(content));
        assertEquals(DepartmentWorkbookReasons.HEADER_MISSING, exception.getMessage());
    }

    @Test
    void rowLimitUsesNamedConstant() {
        assertEquals(1000, DepartmentWorkbookLimits.MAX_DATA_ROWS);
    }
}
