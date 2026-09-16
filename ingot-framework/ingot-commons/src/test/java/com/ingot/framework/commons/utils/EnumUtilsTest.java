package com.ingot.framework.commons.utils;

import java.util.Map;

import com.ingot.framework.commons.model.iam.MemberStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>验证枚举字面量索引拒绝重复值，并按未知/空入参区分查找与解析。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class EnumUtilsTest {

    @Test
    void indexesUniqueValuesAndLooksUp() {
        Map<String, MemberStatus> index = EnumUtils.index(MemberStatus.values(), MemberStatus::getValue);
        assertEquals(MemberStatus.ACTIVE, EnumUtils.get(index, "ACTIVE"));
        assertNull(EnumUtils.get(index, null));
        assertNull(EnumUtils.get(index, "MISSING"));
        assertEquals(MemberStatus.SUSPENDED, EnumUtils.require(index, "SUSPENDED"));
        assertNull(EnumUtils.require(index, null));
        assertThrows(IllegalArgumentException.class, () -> EnumUtils.require(index, "MISSING"));
    }
}
