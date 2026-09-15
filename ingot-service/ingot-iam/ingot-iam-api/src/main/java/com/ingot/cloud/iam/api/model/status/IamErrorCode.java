package com.ingot.cloud.iam.api.model.status;

import com.ingot.framework.commons.model.status.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>声明 IAM 保留业务接口的错误代码。</p>
 *
 * @author wangchao
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum IamErrorCode implements ErrorCode {
    ExistUsername("PUE01", "用户名已存在"),
    ExistPhone("PUE02", "手机号已存在"),
    ExistEmail("PUE03", "Email已存在");

    private final String code;
    private final String text;
}
