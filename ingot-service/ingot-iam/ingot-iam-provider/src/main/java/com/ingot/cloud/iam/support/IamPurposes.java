package com.ingot.cloud.iam.support;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.SelectionPurpose;

/**
 * <p>校验候选接口的用途白名单，未知或与入口不匹配的 purpose 不能换成无权全集。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class IamPurposes {
    private IamPurposes() {
    }

    /**
     * 确认请求用途恰好等于该入口允许的值。
     *
     * @param actual 请求用途
     * @param expected 本入口唯一允许的用途
     * @throws BizException 缺失或不匹配时为 InvalidArgument
     */
    public static void require(SelectionPurpose actual, SelectionPurpose expected) {
        if (actual == null || actual != expected) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
    }
}
