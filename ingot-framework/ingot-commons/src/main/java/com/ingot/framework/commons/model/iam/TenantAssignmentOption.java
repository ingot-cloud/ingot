package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * <p>返回受限分页候选身份及可披露名称。</p>
 * @author jy
 * @since 1.0.0
 * @param id 对象ID
 * @param name 可披露名称
 * @param kind 封闭候选类型
 * @param roleKind 角色归属类型
 */
@Schema(description = "返回受限分页候选身份及可披露名称")
public record TenantAssignmentOption(
        String id,
        String name,
        TenantAssignmentOptionKind kind,
        RoleKind roleKind) {
}
