package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>调整既有授权的版本范围和期限并保留来源校验。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 预览或读取获得的配置版本；提交时比较并重新鉴权
 * @param assignment 待保存授权；服务端禁止借此转移或伪造委派来源
 */
@Schema(description = "调整既有授权的版本范围和期限并保留来源校验")
public record AssignmentUpdateInput(
        @NotBlank @Schema(description = "预览或读取获得的配置版本；提交时比较并重新鉴权", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @NotNull @Valid @Schema(description = "待保存授权；服务端禁止借此转移或伪造委派来源", requiredMode = Schema.RequiredMode.REQUIRED)
        AssignmentInput assignment) {
}
