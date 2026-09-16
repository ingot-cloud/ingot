package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>保存租户定制版本的最小元数据覆盖，空值表示继承固定基础版本。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param name 名称覆盖，空值继承
 * @param description 说明覆盖，空值继承
 * @param groupName 展示分组覆盖，空值继承
 */
@Schema(description = "保存租户定制版本的最小元数据覆盖，空值表示继承固定基础版本")
public record RoleMetadataOverrides(
        @Schema(description = "名称覆盖，空值继承")
        String name,
        @Schema(description = "说明覆盖，空值继承")
        String description,
        @Schema(description = "展示分组覆盖，空值继承")
        String groupName) {
}
