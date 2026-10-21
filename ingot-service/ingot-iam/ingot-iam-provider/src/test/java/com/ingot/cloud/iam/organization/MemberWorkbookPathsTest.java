package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.util.List;

import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.MemberDepartmentBinding;
import com.ingot.framework.commons.model.iam.MemberDepartmentView;
import com.ingot.framework.commons.model.iam.MemberWorkbookReasons;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>验证部门路径从根部门名开始分层，并拒绝未知或歧义路径。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class MemberWorkbookPathsTest {
    @Test
    void formatAndParseKeepPrimaryFirst() {
        MemberWorkbookPaths paths = new MemberWorkbookPaths(List.of(
                department(1, null, "英格特云"),
                department(2, 1L, "研发部"),
                department(3, 1L, "产品部")));
        String cell = paths.format(List.of(
                new MemberDepartmentView("3", "产品部", false),
                new MemberDepartmentView("2", "研发部", true)));
        assertEquals("英格特云/研发部,英格特云/产品部", cell);
        List<MemberDepartmentBinding> parsed = paths.parse(cell);
        assertEquals("2", parsed.getFirst().id());
        assertTrue(parsed.getFirst().primary());
        assertEquals("3", parsed.get(1).id());
        assertEquals("1", paths.parse("英格特云").getFirst().id());
    }

    @Test
    void unknownAndAmbiguousPathsFail() {
        MemberWorkbookPaths paths = new MemberWorkbookPaths(List.of(
                department(1, null, "集团"),
                department(2, 1L, "研发"),
                department(3, 1L, "研发")));
        BizException missing = assertThrows(BizException.class, () -> paths.parse("集团/不存在"));
        assertEquals(MemberWorkbookReasons.DEPARTMENT_NOT_FOUND, missing.getMessage());
        BizException ambiguous = assertThrows(BizException.class, () -> paths.parse("集团/研发"));
        assertEquals(MemberWorkbookReasons.DEPARTMENT_AMBIGUOUS, ambiguous.getMessage());
    }

    private static IamDepartmentEntity department(long id, Long parentId, String name) {
        IamDepartmentEntity row = new IamDepartmentEntity();
        row.setId(BigInteger.valueOf(id));
        row.setParentId(parentId == null ? null : BigInteger.valueOf(parentId));
        row.setName(name);
        return row;
    }
}
