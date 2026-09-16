package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回可见授权记录，保留主体、固定版本、范围及来源链。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 授权记录 ID
 * @param assignment 有效分配定义
 * @param status 生效或撤销状态；到期另按时间判断
 * @param source 写入来源
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回可见授权记录，保留主体、固定版本、范围及来源链")
public record AssignmentRecord(
        @NotBlank @Schema(description = "授权记录 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotNull @Valid @Schema(description = "有效分配定义", requiredMode = Schema.RequiredMode.REQUIRED)
        AssignmentInput assignment,
        @NotNull @Schema(description = "生效或撤销状态；到期另按时间判断", requiredMode = Schema.RequiredMode.REQUIRED)
        GrantStatus status,
        @NotNull @Schema(description = "写入来源", requiredMode = Schema.RequiredMode.REQUIRED)
        AssignmentSource source) {
}
