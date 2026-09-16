package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>整体调整委派并原子检查所有受影响派生授权。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 预览或读取获得的配置版本；提交时比较并重新鉴权
 * @param delegation 完整委派限制
 */
@Schema(description = "整体调整委派并原子检查所有受影响派生授权")
public record DelegationUpdateInput(
        @NotBlank @Schema(description = "预览或读取获得的配置版本；提交时比较并重新鉴权", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @NotNull @Valid @Schema(description = "完整委派限制", requiredMode = Schema.RequiredMode.REQUIRED)
        DelegationInput delegation) {
}
