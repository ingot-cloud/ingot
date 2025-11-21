package com.ingot.cloud.pms.api.model.dto.role;

import com.ingot.framework.commons.model.common.AssignDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : BizRoleAssignUsersDTO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/20.</p>
 * <p>Time         : 17:33.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizRoleAssignUsersDTO extends AssignDTO<Long, Long> {
    /**
     * 部门ID，可以为空，如果绑定的角色为部门角色该字段不为空
     */
    private Long deptId;
}
