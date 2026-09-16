package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>定位预览或提交校验问题，不要求客户端解析中文错误消息。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param path 输入字段或条目路径
 * @param code 稳定业务错误码
 * @param message 可展示的安全中文说明
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "定位预览或提交校验问题，不要求客户端解析中文错误消息")
public record ValidationIssue(
        @NotNull @Schema(description = "输入字段或条目路径", requiredMode = Schema.RequiredMode.REQUIRED)
        String path,
        @NotNull @Schema(description = "稳定业务错误码", requiredMode = Schema.RequiredMode.REQUIRED)
        IamReasonCode code,
        @NotBlank @Schema(description = "可展示的安全中文说明", requiredMode = Schema.RequiredMode.REQUIRED)
        String message) {
}
