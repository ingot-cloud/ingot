package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * <p>展示共享或组织私有角色身份，不暴露权限快照版本。</p>
 * @author jy
 * @since 1.0.0
 * @param id 角色ID
 * @param code 稳定编码
 * @param name 角色名称
 * @param description 可空说明
 * @param groupName 可空分组
 * @param kind 角色归属类型
 * @param status 启停状态
 */
@Schema(description = "展示共享或组织私有角色身份，不暴露权限快照版本")
public record CurrentRoleSummary(
        String id,
        String code,
        String name,
        String description,
        String groupName,
        RoleKind kind,
        ConfigurationStatus status) {
}
