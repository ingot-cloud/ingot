package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回创建结果的对象 ID 与乐观锁版本。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 创建的对象 ID
 * @param version 创建后的版本，字符串传输
 */
@Schema(description = "返回创建结果的对象 ID 与乐观锁版本")
public record CreatedResource(
        @NotBlank @Schema(description = "创建的对象 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "创建后的版本，字符串传输", requiredMode = Schema.RequiredMode.REQUIRED)
        String version) {
}
