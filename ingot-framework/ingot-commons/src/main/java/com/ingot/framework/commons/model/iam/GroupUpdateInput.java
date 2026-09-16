package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>整体替换静态组并检查引用影响。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 预览或读取获得的配置版本；提交时比较并重新鉴权
 * @param group 组完整内容
 */
@Schema(description = "整体替换静态组并检查引用影响")
public record GroupUpdateInput(
        @NotBlank @Schema(description = "预览或读取获得的配置版本；提交时比较并重新鉴权", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @NotNull @Valid @Schema(description = "组完整内容", requiredMode = Schema.RequiredMode.REQUIRED)
        GroupDraft group) {
}
