package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>显式切换配置状态并校验读取版本。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param status 目标配置状态
 * @param expectedVersion 预览或读取获得的配置版本；提交时比较并重新鉴权
 */
@Schema(description = "显式切换配置状态并校验读取版本")
public record ConfigurationStatusInput(
        @NotNull @Valid @Schema(description = "目标配置状态", requiredMode = Schema.RequiredMode.REQUIRED)
        ConfigurationStatus status,
        @NotBlank @Schema(description = "预览或读取获得的配置版本；提交时比较并重新鉴权", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion) {
}
