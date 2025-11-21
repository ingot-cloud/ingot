package com.ingot.cloud.pms.api.model.bo.role;

import java.io.Serializable;

import lombok.Data;

/**
 * <p>Description  : BizAssignRoleBO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/19.</p>
 * <p>Time         : 18:03.</p>
 */
@Data
public class BizAssignRoleBO implements Serializable {
    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 是否为元数据角色
     */
    private Boolean metaRole;

    /**
     * 部门ID，可以为空，部门角色该字段不为空
     */
    private Long deptId;

}
