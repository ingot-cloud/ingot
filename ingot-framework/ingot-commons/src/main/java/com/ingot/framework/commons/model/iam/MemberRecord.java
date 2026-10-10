package com.ingot.framework.commons.model.iam;

import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import com.ingot.framework.commons.annotation.field.FieldBinding;
import com.ingot.framework.commons.annotation.field.PublicField;
import jakarta.validation.constraints.*;

/**
 * <p>返回经字段策略处理的成员资料，隐藏字段以空引用省略，脱敏字段只携带脱敏值。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 成员 ID，不是账号 ID
 * @param displayName 可见显示名称；隐藏时省略
 * @param avatar 可见头像时效链接；隐藏时省略
 * @param phone 当前域联系手机号；可能已脱敏，隐藏时省略，不回退到全局账号
 * @param email 当前域联系邮箱；可能已脱敏，隐藏时省略，不回退到全局账号
 * @param username 已关联全局账号的登录名；平台成员填写，租户成员省略
 * @param status 该域的成员资格
 * @param departments 仅包含可见部门关系；平台成员为空数组
 * @param joinedAt 平台详情的首次加入时间，UTC；列表与租户成员省略
 * @param lastLoginAt 平台详情的账号最近成功登录时间，UTC；包含平台及组织身份，无记录时省略
 * @param updatedAt 平台详情的成员记录更新时间，UTC；不代表最后活跃时间
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回经字段策略处理的成员资料，隐藏字段以空引用省略，脱敏字段只携带脱敏值")
public record MemberRecord(
        @NotBlank @Schema(description = "成员 ID，不是账号 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @PublicField
        String id,
         @Schema(description = "可见显示名称；隐藏时省略")
        @FieldBinding(key = MemberFieldKey.VALUE_DISPLAY_NAME) String displayName,
         @Schema(description = "可见头像时效链接；隐藏时省略")
        @FieldBinding(key = MemberFieldKey.VALUE_AVATAR) String avatar,
         @Schema(description = "当前域联系手机号；可能已脱敏，隐藏时省略")
        @FieldBinding(key = MemberFieldKey.VALUE_PHONE) String phone,
         @Schema(description = "当前域联系邮箱；可能已脱敏，隐藏时省略")
        @FieldBinding(key = MemberFieldKey.VALUE_EMAIL) String email,
         @Schema(description = "已关联全局账号的登录名；平台成员填写，租户成员省略")
        @PublicField
        String username,
        @NotNull @Schema(description = "该域的成员资格", requiredMode = Schema.RequiredMode.REQUIRED)
        @PublicField
        MemberStatus status,
        @NotNull @Schema(description = "仅包含可见部门关系；平台成员为空数组", requiredMode = Schema.RequiredMode.REQUIRED)
        @PublicField
        List<@NotNull @Valid MemberDepartmentView> departments,
        @Schema(description = "平台成员首次加入时间，UTC；仅平台详情填写", accessMode = Schema.AccessMode.READ_ONLY)
        @FieldBinding(key = MemberTimeFields.JOINED_AT) Instant joinedAt,
        @Schema(description = "账号最近成功登录时间，UTC；包含平台及组织身份，仅平台详情填写", accessMode = Schema.AccessMode.READ_ONLY)
        @FieldBinding(key = MemberTimeFields.LAST_LOGIN_AT) Instant lastLoginAt,
        @Schema(description = "成员记录更新时间，UTC；仅平台详情填写，不代表最后活跃时间", accessMode = Schema.AccessMode.READ_ONLY)
        @FieldBinding(key = MemberTimeFields.UPDATED_AT) Instant updatedAt) {

    /**
     * 构造不携带详情时间的成员投影，供列表及租户成员响应使用。
     */
    public MemberRecord(String id, String displayName, String avatar, String phone, String email, String username,
                        MemberStatus status, List<MemberDepartmentView> departments) {
        this(id, displayName, avatar, phone, email, username, status, departments, null, null, null);
    }

    /**
     * 复制输出集合，避免外部修改改变已经计算的响应视图；必填空引用由校验拒绝。
     */
    public MemberRecord {
        if (departments != null) {
            departments = Collections.unmodifiableList(new ArrayList<>(departments));
        }
    }
}
