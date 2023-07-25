package com.ingot.cloud.pms.api.model.status;

import com.ingot.framework.core.model.status.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : PmsStatusCode.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/1/21.</p>
 * <p>Time         : 3:39 PM.</p>
 */
@Getter
@RequiredArgsConstructor
public enum PmsErrorCode implements ErrorCode {
    ExistUsername("PUE01", "用户名已存在"),
    ExistPhone("PUE02", "手机号已存在"),
    ExistEmail("PUE03", "Email已存在");

    private final String code;
    private final String text;
}
