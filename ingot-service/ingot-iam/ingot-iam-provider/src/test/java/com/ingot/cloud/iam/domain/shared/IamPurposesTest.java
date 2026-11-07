package com.ingot.cloud.iam.domain.shared;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>验证候选入口拒绝缺失或错配的 purpose。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class IamPurposesTest {
    @Test
    void matchingPurposeIsAccepted() {
        IamPurposes.require(SelectionPurpose.MANAGED_DEPARTMENT, SelectionPurpose.MANAGED_DEPARTMENT);
    }

    @Test
    void missingOrMismatchedPurposeIsInvalidArgument() {
        BizException missing = assertThrows(BizException.class,
                () -> IamPurposes.require(null, SelectionPurpose.MANAGED_DEPARTMENT));
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), missing.getCode());
        BizException mismatched = assertThrows(BizException.class,
                () -> IamPurposes.require(SelectionPurpose.ASSIGN_RECIPIENT, SelectionPurpose.MANAGED_DEPARTMENT));
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), mismatched.getCode());
    }
}
