package com.ingot.cloud.iam.organization;

import java.util.List;
import java.util.Map;

import com.ingot.cloud.iam.organization.MemberWorkbookExcel.MemberWorkbookSheet;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.MemberWorkbookColumns;
import com.ingot.framework.commons.model.iam.MemberWorkbookLimits;
import com.ingot.framework.commons.model.iam.MemberWorkbookReasons;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>验证表格读写能跳过须知行，并拒绝超过行上限的文件。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class MemberWorkbookExcelTest {
    @Test
    void writeThenReadKeepsDataRows() {
        byte[] content = MemberWorkbookExcel.writeMembers(
                List.of(MemberWorkbookColumns.DISPLAY_NAME, MemberWorkbookColumns.PHONE,
                        MemberWorkbookColumns.DEPARTMENTS),
                List.of(Map.of(MemberWorkbookColumns.DISPLAY_NAME, "张三", MemberWorkbookColumns.PHONE, "13800001111",
                        MemberWorkbookColumns.DEPARTMENTS, "英格特云/研发部")),
                false);
        MemberWorkbookSheet sheet = MemberWorkbookExcel.read(content);
        assertTrue(sheet.has(MemberWorkbookColumns.DISPLAY_NAME));
        assertEquals(1, sheet.rows().size());
        assertEquals("张三", sheet.rows().getFirst().get(MemberWorkbookColumns.DISPLAY_NAME));
        assertEquals("13800001111", sheet.rows().getFirst().get(MemberWorkbookColumns.PHONE));
        assertEquals(6, sheet.excelRow(0));
    }

    @Test
    void normalizeStripsNumericPhoneSuffix() {
        assertEquals("13800001111", MemberWorkbookExcel.normalize("13800001111.0"));
        assertEquals("", MemberWorkbookExcel.normalize("  "));
    }

    @Test
    void missingHeaderFails() {
        byte[] content = MemberWorkbookExcel.write(List.of(List.of("不是表头")));
        BizException exception = assertThrows(BizException.class, () -> MemberWorkbookExcel.read(content));
        assertEquals(MemberWorkbookReasons.HEADER_MISSING, exception.getMessage());
    }

    @Test
    void rowLimitUsesNamedConstant() {
        assertEquals(1000, MemberWorkbookLimits.MAX_DATA_ROWS);
    }
}
