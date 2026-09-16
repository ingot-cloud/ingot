package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回经字段策略处理的成员资料，隐藏字段以空引用省略，脱敏字段只携带脱敏值。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 成员 ID，不是账号 ID
 * @param displayName 可见显示名称；隐藏时省略
 * @param avatar 可见头像；隐藏时省略
 * @param phone 允许输出的手机号字符串；可能已脱敏，隐藏时省略
 * @param email 允许输出的邮箱字符串；可能已脱敏，隐藏时省略
 * @param status 该域的成员资格
 * @param departments 仅包含可见部门关系；平台成员为空数组
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回经字段策略处理的成员资料，隐藏字段以空引用省略，脱敏字段只携带脱敏值")
public record MemberRecord(
        @NotBlank @Schema(description = "成员 ID，不是账号 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
         @Schema(description = "可见显示名称；隐藏时省略")
        String displayName,
         @Schema(description = "可见头像；隐藏时省略")
        String avatar,
         @Schema(description = "允许输出的手机号字符串；可能已脱敏，隐藏时省略")
        String phone,
         @Schema(description = "允许输出的邮箱字符串；可能已脱敏，隐藏时省略")
        String email,
        @NotNull @Schema(description = "该域的成员资格", requiredMode = Schema.RequiredMode.REQUIRED)
        MemberStatus status,
        @NotNull @Schema(description = "仅包含可见部门关系；平台成员为空数组", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid MemberDepartmentView> departments) {

    /**
     * 复制输出集合，避免外部修改改变已经计算的响应视图；必填空引用由校验拒绝。
     */
    public MemberRecord {
        if (departments != null) {
            departments = Collections.unmodifiableList(new ArrayList<>(departments));
        }
    }
}
