package com.ingot.framework.commons.model.iam;

import java.util.List;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * <p>统一表单中的负责人差量，沿用外层部门版本，不覆盖未加载关系。</p>
 * @author jy
 * @since 1.0.0
 * @param addMemberIds 新增成员
 * @param removeMemberIds 明确移除成员
 * @param primaryMemberId 新主负责人，省略则保留原主
 */
@Schema(description = "负责人增删与设主差量，使用外层部门版本")
public record DepartmentLeaderChanges(
        @NotNull @Size(max = DepartmentLeaderUpdateInput.MAX_DELTA_MEMBERS) List<@NotBlank String> addMemberIds,
        @NotNull @Size(max = DepartmentLeaderUpdateInput.MAX_DELTA_MEMBERS) List<@NotBlank String> removeMemberIds,
        String primaryMemberId) { }
