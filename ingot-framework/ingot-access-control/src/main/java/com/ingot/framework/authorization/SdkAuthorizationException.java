package com.ingot.framework.authorization;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;

/**
 * <p>SDK 执行拒绝与基础设施故障的稳定异常，不改变其他业务异常的 HTTP 协议。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class SdkAuthorizationException extends BizException {

    /**
     * 根据 IAM 稳定错误码构造异常。
     * @param reason 拒绝或故障原因
     */
    public SdkAuthorizationException(IamReasonCode reason) {
        super(reason);
    }

}
