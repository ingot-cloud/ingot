package com.ingot.framework.commons.model.bff;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
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

    INVALID_REQUEST("BFF_INVALID_REQUEST", "请求不合法", 400), RETURN_TO_INVALID("BFF_RETURN_TO_INVALID", "返回路径不合法", 400),
    TRANSACTION_EXPIRED("BFF_TRANSACTION_EXPIRED", "登录事务已过期", 410),
    TRANSACTION_CONFLICT("BFF_TRANSACTION_CONFLICT", "登录事务状态冲突", 409),
    ENTRY_MISMATCH("BFF_ENTRY_MISMATCH", "请求入口与注册应用不匹配", 403),
    BINDING_MISMATCH("BFF_BINDING_MISMATCH", "浏览器绑定不匹配", 403), CSRF_INVALID("BFF_CSRF_INVALID", "登录安全凭证已失效，请重试", 403),
    TRANSACTION_BUSY("BFF_TRANSACTION_BUSY", "本次登录正在处理中", 409),
    RESULT_UNAVAILABLE("BFF_RESULT_UNAVAILABLE", "本次登录结果无法恢复，请重新认证", 409),
    LOGIN_CAPACITY("BFF_LOGIN_CAPACITY", "登录流程过多，请稍后重试", 429),
    AUTH_UNAVAILABLE("BFF_AUTH_UNAVAILABLE", "认证服务暂不可用，请稍后重试", 503),
    IDENTITY_UNAVAILABLE("BFF_IDENTITY_UNAVAILABLE", "当前入口无法建立该身份", 403);

    /** 公开稳定错误码。 */
    @JsonValue
    @EnumValue
    private final String code;

    /** 用户可读的安全提示。 */
    private final String text;

    /** 对应HTTP状态。 */
    private final int httpStatus;

    private static final Map<String, BffErrorCode> BY_CODE = EnumUtils.index(values(), BffErrorCode::getCode);

    /** 严格解析错误码，未知非空值拒绝。 */
    @JsonCreator
    public static BffErrorCode getEnum(String code) {
        return EnumUtils.require(BY_CODE, code);
    }

    /** 查询已知错误码，不识别时返回空。 */
    public static BffErrorCode find(String code) {
        return EnumUtils.get(BY_CODE, code);
    }

}
