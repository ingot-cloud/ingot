package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回当前域内可见部门或必要导航骨架。</p>
 * <p>租户管理部门列表填充 {@code memberCount}；详情、通讯录与导航骨架省略该字段。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 部门 ID
 * @param parentId 父部门 ID，根节点为空
 * @param name 可见部门名称
 * @param sortOrder 展示顺序
 * @param navigationOnly 是否仅作为必要祖先导航骨架
 * @param leaderSummary 管理列表或详情的可见负责人摘要，其他场景省略
 * @param memberCount 本部门直属 ACTIVE 成员数；仅租户列表填充
 * @param parentDepartment 仅详情返回当前读取范围内的上级部门名称
 * @param leaderPage 仅详情返回已授权的负责人首屏，后续沿关联接口分页
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回当前域内可见部门或必要导航骨架；租户列表可携带直属 ACTIVE 成员数")
public record DepartmentRecord(
        @NotBlank @Schema(description = "部门 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
         @Schema(description = "父部门 ID，根节点为空")
        String parentId,
        @NotBlank @Schema(description = "可见部门名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
         @Schema(description = "展示顺序", requiredMode = Schema.RequiredMode.REQUIRED)
        int sortOrder,
         @Schema(description = "是否仅作为必要祖先导航骨架", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean navigationOnly,
         @Min(0) @Schema(description = "本部门直属 ACTIVE 成员数，仅租户管理部门列表填充")
        Integer memberCount,
        @Schema(description = "仅管理列表或详情的负责人摘要") DepartmentLeaderSummary leaderSummary,
        @Schema(description = "仅详情返回当前读取范围内的上级部门") DepartmentNavigationEntry parentDepartment,
        @Schema(description = "仅详情返回已授权的负责人第一页，默认20条") DepartmentLeaderPage leaderPage) {

    /** 构造不含详情聚合字段的既有部门输出。 */
    public DepartmentRecord(String id, String parentId, String name, int sortOrder, boolean navigationOnly,
                            Integer memberCount, DepartmentLeaderSummary leaderSummary) {
        this(id, parentId, name, sortOrder, navigationOnly, memberCount, leaderSummary, null, null);
    }

    /** 构造不包含负责人摘要的既有部门输出。 */
    public DepartmentRecord(String id, String parentId, String name, int sortOrder, boolean navigationOnly, Integer memberCount) {
        this(id, parentId, name, sortOrder, navigationOnly, memberCount, null);
    }
}
