package com.ingot.framework.commons.model.iam;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * <p>成员资料与角色差量预览，不包含字段原值。</p>
 * @author jy
 * @since 1.0.0
 * @param profileFields 变化的资料字段编码
 * @param additions 新增分配数
 * @param updates 范围调整数
 * @param removals 撤销分配数
 */
@Schema(description = "成员资料与角色差量预览，不包含字段原值")
public record PlatformMemberEditPreview(
        @Schema(description = "变化的资料字段编码") List<String> profileFields,
        @Schema(description = "新增分配数") int additions,
        @Schema(description = "范围调整数") int updates,
        @Schema(description = "撤销分配数") int removals) {
}
