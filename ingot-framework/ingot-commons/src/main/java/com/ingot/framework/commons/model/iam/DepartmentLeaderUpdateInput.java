package com.ingot.framework.commons.model.iam;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * <p>提交负责人增删差量及主负责人选择，不覆盖未加载或不可见关系。</p>
 *
 * @param expectedVersion 编辑时读取的部门版本
 * @param addMemberIds 本次新增成员，不覆盖未加载的关系
 * @param removeMemberIds 本次明确移除的成员
 * @param primaryMemberId 新的主负责人成员 ID，省略时保留原主负责人
 * @author jy
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "提交负责人增删差量及主负责人选择，不覆盖未加载或不可见关系。")
public record DepartmentLeaderUpdateInput(
        @NotBlank String expectedVersion,
        @NotNull @Size(max = DepartmentLeaderUpdateInput.MAX_DELTA_MEMBERS) List<@NotBlank String> addMemberIds,
        @NotNull @Size(max = DepartmentLeaderUpdateInput.MAX_DELTA_MEMBERS) List<@NotBlank String> removeMemberIds,
        String primaryMemberId) {
    /** 单次负责人增删差量的最大成员数。 */
    public static final int MAX_DELTA_MEMBERS = 200;
}
