package com.ingot.framework.data.mybatis.scope.error;

import com.ingot.framework.commons.model.status.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : DataScopeErrorCode.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/4/1.</p>
 * <p>Time         : 15:11.</p>
 */
@Getter
@RequiredArgsConstructor
public enum DataScopeErrorCode implements ErrorCode {
    DS_401("ds_unauthorized", "未授权"),
    DS_403("ds_forbidden", "无权访问"),
    DS_COMMON("ds_common", "公共异常");

    private final String code;
    private final String text;
}
