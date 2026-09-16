package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回当前身份的一致授权视图，不包含全租户授权快照或其他身份权限。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param context 经认证的当前身份
 * @param profile 当前成员最小资料
 * @param applications 当前可用应用
 * @param menus 当前可见导航
 * @param actionCodes 已展开的精确操作码
 * @param version 一致授权视图版本
 * @param expiresAt 授权到期时间，UTC；不可延长既有有效期
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回当前身份的一致授权视图，不包含全租户授权快照或其他身份权限")
public record Bootstrap(
        @NotNull @Valid @Schema(description = "经认证的当前身份", requiredMode = Schema.RequiredMode.REQUIRED)
        AuthorizationContext context,
        @NotNull @Valid @Schema(description = "当前成员最小资料", requiredMode = Schema.RequiredMode.REQUIRED)
        CurrentProfile profile,
        @NotNull @Schema(description = "当前可用应用", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ApplicationSummary> applications,
        @NotNull @Schema(description = "当前可见导航", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid MenuNode> menus,
        @NotNull @Schema(description = "已展开的精确操作码", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotBlank String> actionCodes,
        @NotBlank @Schema(description = "一致授权视图版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String version,
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING) @Schema(description = "授权到期时间，UTC；不可延长既有有效期", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant expiresAt) {

    /**
     * 复制输出集合，避免外部修改改变已经计算的响应视图；必填空引用由校验拒绝。
     */
    public Bootstrap {
        if (applications != null) {
            applications = Collections.unmodifiableList(new ArrayList<>(applications));
        }
        if (menus != null) {
            menus = Collections.unmodifiableList(new ArrayList<>(menus));
        }
        if (actionCodes != null) {
            actionCodes = Collections.unmodifiableList(new ArrayList<>(actionCodes));
        }
    }
}
