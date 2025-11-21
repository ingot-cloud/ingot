package com.ingot.cloud.pms.api.model.bo.role;

import com.ingot.cloud.pms.api.model.bo.common.BizSetBO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : 角色用户关系绑定实体.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/19.</p>
 * <p>Time         : 16:09.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizRoleAssignUsersBO extends BizSetBO<Long, Long> {
    /**
     * 部门ID，可以为空，如果绑定的角色为部门角色该字段不为空
     */
    private Long deptId;
    /**
     * 当前绑定ID是否为元数据
     */
    private boolean metaFlag;
}
