package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.model.status.ErrorCode;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>统一 IAM 对外的稳定错误码、中文说明及 HTTP 状态，不把拒绝与授权不可用混为一类。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum IamReasonCode implements ErrorCode {
    INVALID_ARGUMENT("InvalidArgument", "请求参数不合法", 400),
    IDENTITY_INVALID("IdentityInvalid", "当前身份无效，请重新认证", 401),
    ACTION_DENIED("ActionDenied", "没有执行该操作的权限", 403),
    DATA_SCOPE_DENIED("DataScopeDenied", "目标不在允许的操作范围内", 403),
    DELEGATION_EXCEEDED("DelegationExceeded", "超出来源委派的允许范围", 403),
    ROLE_REVISION_UNAVAILABLE("RoleRevisionUnavailable", "角色版本不可用", 403),
    APPLICATION_UNAVAILABLE("ApplicationUnavailable", "当前身份不可访问该应用", 403),
    OBJECT_NOT_FOUND("ObjectNotFound", "对象不存在或不可访问", 404),
    POLICY_CONFLICT("PolicyConflict", "策略或角色差异存在未解决的冲突", 400),
    REVISION_CONFLICT("RevisionConflict", "配置已变化，请重新预览后提交", 409),
    OBJECT_IN_USE("ObjectInUse", "对象仍被引用，无法删除", 400),
    AUTHORIZATION_UNAVAILABLE("AuthorizationUnavailable", "暂时无法加载有效授权，请稍后重试", 503);

    /**
     * JSON 与对外契约使用的稳定错误码。
     */
    @JsonValue
    @EnumValue
    private final String code;
    /**
     * 不包含目标敏感信息的默认中文说明。
     */
    private final String text;
    /**
     * 该业务错误应使用的 HTTP 状态。
     */
    private final int httpStatus;

    private static final Map<String, IamReasonCode> BY_CODE = EnumUtils.index(values(), IamReasonCode::getCode);

    /**
     * 按稳定错误码解析。
     *
     * @param code 稳定错误码；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static IamReasonCode getEnum(String code) {
        return EnumUtils.require(BY_CODE, code);
    }

    /**
     * 按稳定错误码查找，供 HTTP 状态映射区分 IAM 业务错误与其他服务错误码。
     *
     * @param code 稳定错误码，可空
     * @return 对应枚举；{@code null} 或非 IAM 错误码返回 {@code null}
     */
    public static IamReasonCode find(String code) {
        return EnumUtils.get(BY_CODE, code);
    }
}
