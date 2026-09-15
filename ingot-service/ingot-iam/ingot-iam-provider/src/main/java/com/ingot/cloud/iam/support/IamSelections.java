package com.ingot.cloud.iam.support;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.Selection;

/**
 * <p>把选择器域约束转成稳定业务错误，避免平台路径携带部门。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class IamSelections {
    private IamSelections() {
    }

    /**
     * 校验选择器与管理域兼容。
     *
     * @param domain 接口管理域
     * @param selection 成员与部门选择
     */
    public static void requireCompatible(AuthorizationDomain domain, Selection selection) {
        if (selection == null) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        try {
            selection.requireCompatibleDomain(domain);
        } catch (IllegalArgumentException exception) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
    }
}
