package com.ingot.cloud.iam.api.model.dto.authorization;

import java.io.Serial;
import java.io.Serializable;

import lombok.Data;

/**
 * <p>授权快照中的一条角色绑定，保留部门任职上下文。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
public class AuthorizationRoleBindingDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色 ID。
     */
    private Long roleId;

    /**
     * 是否平台预设角色。
     */
    private Boolean platformRole;

    /**
     * 角色编码。
     */
    private String roleCode;

    /**
     * 绑定部门 ID；非部门角色为 {@code null}。
     */
    private Long deptId;

    /**
     * 角色是否按部门过滤数据范围。
     */
    private Boolean filterDept;
}
