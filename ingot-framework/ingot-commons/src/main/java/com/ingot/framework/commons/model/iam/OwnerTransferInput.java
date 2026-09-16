package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>在当前组织内原子转交所有者职责。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 预览或读取获得的配置版本；提交时比较并重新鉴权
 * @param newOwnerMemberId 当前组织内有效的新所有者成员 ID
 */
@Schema(description = "在当前组织内原子转交所有者职责")
public record OwnerTransferInput(
        @NotBlank @Schema(description = "预览或读取获得的配置版本；提交时比较并重新鉴权", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @NotBlank @Schema(description = "当前组织内有效的新所有者成员 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String newOwnerMemberId) {
}
