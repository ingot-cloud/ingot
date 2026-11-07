package com.ingot.cloud.iam.domain.group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * <p>标出成员进入租户组的来源，直接绑定与部门规则可同时成立。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param direct 是否存在显式组成员关系
 * @param departments 命中的组部门规则，按规则部门 ID 升序
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "成员进入租户组的来源，直接绑定与部门规则可同时成立")
public record GroupMembership(
        @Schema(description = "是否存在显式组成员关系", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean direct,
        @NotNull @Schema(description = "命中的组部门规则", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid GroupMembershipDepartment> departments) {

    /**
     * 固定来源集合快照。
     */
    public GroupMembership {
        departments = departments == null ? null : Collections.unmodifiableList(new ArrayList<>(departments));
    }
}
