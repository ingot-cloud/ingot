package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>整体替换通讯录策略，提交必须重验版本及授权。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 预览或读取获得的配置版本；提交时比较并重新鉴权
 * @param policy 通讯录完整配置
 */
@Schema(description = "整体替换通讯录策略，提交必须重验版本及授权")
public record DirectoryPolicyInput(
        @NotBlank @Schema(description = "预览或读取获得的配置版本；提交时比较并重新鉴权", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @NotNull @Valid @Schema(description = "通讯录完整配置", requiredMode = Schema.RequiredMode.REQUIRED)
        DirectoryPolicyDraft policy) {
}
