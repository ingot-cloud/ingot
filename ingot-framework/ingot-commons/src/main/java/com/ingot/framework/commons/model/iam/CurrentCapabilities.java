package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>刷新当前身份的精确操作集合及其有效期限。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param actionCodes 当前有效的精确操作码
 * @param version 授权视图版本
 * @param expiresAt UTC 授权到期时间
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "刷新当前身份的精确操作集合及其有效期限")
public record CurrentCapabilities(
        @NotNull @Schema(description = "当前有效的精确操作码", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotBlank String> actionCodes,
        @NotBlank @Schema(description = "授权视图版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String version,
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING) @Schema(description = "UTC 授权到期时间", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant expiresAt) {

    /**
     * 复制输出集合，避免外部修改改变已经计算的响应视图；必填空引用由校验拒绝。
     */
    public CurrentCapabilities {
        if (actionCodes != null) {
            actionCodes = Collections.unmodifiableList(new ArrayList<>(actionCodes));
        }
    }
}
