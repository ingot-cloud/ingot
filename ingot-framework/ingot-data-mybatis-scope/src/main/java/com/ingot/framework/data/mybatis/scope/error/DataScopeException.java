package com.ingot.framework.data.mybatis.scope.error;

import com.ingot.framework.commons.error.BizException;

/**
 * <p>Description  : DataScopeException.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/4/1.</p>
 * <p>Time         : 15:10.</p>
 */
public class DataScopeException extends BizException {

    public DataScopeException(DataScopeErrorCode code) {
        super(code);
    }

    public DataScopeException(String message) {
        super(DataScopeErrorCode.DS_COMMON, message);
    }
}
