package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * <p>返回当前权限保存结果与待补范围计数。</p>
 * @author jy
 * @since 1.0.0
 * @param roleId 角色身份ID
 * @param version 技术配置锁
 * @param scopeIssueAssignments 可披露待补分配计数
 */
@Schema(description = "返回当前权限保存结果与待补范围计数")
public record CurrentRolePermissionResult(
        String roleId,
        String version,
        Long scopeIssueAssignments) {
}
