package com.ingot.framework.commons.model.iam;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * <p>按版本撤销成员直接分配。</p>
 * @author jy
 * @since 1.0.0
 * @param assignmentId 分配 ID
 * @param expectedVersion 读取的分配版本
 */
@Schema(description = "按版本撤销成员直接分配")
public record MemberRoleRemoval(
        @Schema(description = "分配 ID") @NotBlank String assignmentId,
        @Schema(description = "读取的分配版本") @NotBlank String expectedVersion) {
}
