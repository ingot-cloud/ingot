package com.ingot.framework.commons.model.iam;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * <p>成员当前有效绑定的角色与版本摘要，不披露组标识或名称。</p>
 * @author jy
 * @since 1.0.0
 * @param roleId 角色定义 ID
 * @param name 角色名称
 * @param roleRevisionRef 固定版本引用
 * @param revisionNumber 固定版本号
 * @param sourceTypes 直接或组继承来源种类
 */
@Schema(description = "成员当前有效绑定的角色与版本摘要，不披露组标识或名称")
public record MemberBoundRole(
        @Schema(description = "角色定义 ID") String roleId,
        @Schema(description = "角色名称") String name,
        @Schema(description = "固定版本引用") RoleRevisionRef roleRevisionRef,
        @Schema(description = "固定版本号") String revisionNumber,
        @Schema(description = "直接或组继承来源种类") List<SubjectType> sourceTypes) {
}
