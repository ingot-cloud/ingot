package com.ingot.framework.data.mybatis.scope.error;

import com.ingot.framework.commons.error.BizException;

/**
 * <p>授权快照不可用：过期刷新失败或授权服务故障，对应 HTTP 503。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public class AuthorizationSnapshotException extends BizException {

    /**
     * 使用约定错误码构造。
     */
    public AuthorizationSnapshotException() {
        super(DataScopeErrorCode.DS_503);
    }

    /**
     * 包装远端不可用原因。
     *
     * @param cause 根因
     */
    public AuthorizationSnapshotException(Throwable cause) {
        super(DataScopeErrorCode.DS_503);
        if (cause != null) {
            initCause(cause);
        }
    }
}
