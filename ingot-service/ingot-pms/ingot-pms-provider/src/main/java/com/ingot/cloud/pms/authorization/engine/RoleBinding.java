package com.ingot.cloud.pms.authorization.engine;

import lombok.Builder;
import lombok.Getter;

/**
 * <p>用户在当前租户下的一条角色绑定，保留部门任职上下文。</p>
 *
 * <p>同一角色绑定到不同部门时保留多条，不能扁平化为用户全部任职部门。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Builder
public class RoleBinding {

    /**
     * 角色 ID。
     */
    private final long roleId;

    /**
     * 是否平台预设角色。
     */
    private final boolean platformRole;

    /**
     * 角色编码。
     */
    private final String roleCode;

    /**
     * 绑定部门 ID；非部门角色为 {@code null}。
     */
    private final Long deptId;

    /**
     * 角色是否按部门过滤数据范围。
     */
    private final boolean filterDept;
}
