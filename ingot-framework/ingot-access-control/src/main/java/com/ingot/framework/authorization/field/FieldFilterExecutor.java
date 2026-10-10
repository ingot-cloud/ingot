package com.ingot.framework.authorization.field;

import java.util.Map;
import com.ingot.framework.authorization.SdkAuthorizationException;
import com.ingot.framework.commons.model.iam.FieldOperations;
import com.ingot.framework.commons.model.iam.IamReasonCode;

/**
 * <p>在 SQL/count 前校验实际筛选键；能力应已包含完整查询范围的 FULL 证明。</p>
 * @author jy
 * @since 1.0.0
 */
public final class FieldFilterExecutor {
    private FieldFilterExecutor() { }

    /** 拒绝所有未明确授权的实际条件，显式空值也不能绕过门禁。 */
    public static void require(Map<String, ?> submitted, Map<String, FieldOperations> operations) {
        for (var key : submitted.keySet())
            if (!operations.getOrDefault(key, FieldOperations.NONE).filterable())
                throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
    }
}
