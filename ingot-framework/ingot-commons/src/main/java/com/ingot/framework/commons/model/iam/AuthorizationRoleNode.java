package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>角色分配树的最小展示节点，不包含版本的操作授权或范围参数。</p>
 *
 * @param id 当前层角色或版本的真实标识
 * @param roleId 所属角色标识
 * @param roleName 角色名称，不依赖展示文字拆分
 * @param name 当前节点名称
 * @param nodeType 节点类型；角色只展开，版本可单选
 * @param revisionNumber 版本号；角色节点为空
 * @param roleRevisionRef 固定版本引用；角色节点为空
 * @author jy
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "角色分配树的最小展示节点")
public record AuthorizationRoleNode(
        @NotBlank String id,
        @NotBlank String roleId,
        @NotBlank String roleName,
        @NotBlank String name,
        @NotNull AuthorizationRoleNodeType nodeType,
        @JsonSerialize(using = IamCountSerializer.class) Long revisionNumber,
        @Valid RoleRevisionRef roleRevisionRef) {
}
