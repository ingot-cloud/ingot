package com.ingot.framework.commons.model.iam;

import com.ingot.framework.commons.error.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>验证操作码按应用与资源编码拼接，完整码不重复加前缀。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class ActionCodesTest {
    @Test
    void composeJoinsSegmentsAndStripsExistingPrefix() {
        assertEquals("iam-platform:account:read",
                ActionCodes.compose("iam-platform", "account", "read"));
        assertEquals("iam-platform:account:read",
                ActionCodes.compose("iam-platform", "account", "iam-platform:account:read"));
    }

    @Test
    void composeRejectsWildcardOrExtraSeparator() {
        assertThrows(BizException.class, () -> ActionCodes.compose("iam-platform", "account", "*"));
        assertThrows(BizException.class, () -> ActionCodes.compose("iam-platform", "account", "read:extra"));
        assertThrows(BizException.class, () -> ActionCodes.compose("iam-platform", "account", "   "));
    }
}
