package com.ingot.framework.commons.model.bff;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.model.status.ErrorCode;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * BFF 登录与会话交接的稳定错误码及 HTTP 状态。
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum BffErrorCode implements ErrorCode {
    INVALID_REQUEST("BFF_INVALID_REQUEST", "请求不合法", 400),
    RETURN_TO_INVALID("BFF_RETURN_TO_INVALID", "返回路径不合法", 400),
    TICKET_INVALID("BFF_TICKET_INVALID", "交接凭证无效或已使用", 400),
    TRANSACTION_EXPIRED("BFF_TRANSACTION_EXPIRED", "登录事务已过期", 410),
    TRANSACTION_CONFLICT("BFF_TRANSACTION_CONFLICT", "登录事务状态冲突", 409),
    ENTRY_MISMATCH("BFF_ENTRY_MISMATCH", "请求入口与注册应用不匹配", 403),
    BINDING_MISMATCH("BFF_BINDING_MISMATCH", "浏览器绑定不匹配", 403),
    IDENTITY_UNAVAILABLE("BFF_IDENTITY_UNAVAILABLE", "当前入口无法建立该身份", 403);

    @JsonValue
    private final String code;
    private final String text;
    private final int httpStatus;

    private static final Map<String, BffErrorCode> BY_CODE = EnumUtils.index(values(), BffErrorCode::getCode);

    @JsonCreator
    public static BffErrorCode getEnum(String code) {
        return EnumUtils.require(BY_CODE, code);
    }

    public static BffErrorCode find(String code) {
        return EnumUtils.get(BY_CODE, code);
    }
}
