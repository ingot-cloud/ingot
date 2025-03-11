package com.ingot.cloud.pms.api.model.vo.user;

import com.ingot.cloud.pms.api.model.domain.SysUser;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : UserWithDeptVO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/11.</p>
 * <p>Time         : 09:13.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserWithDeptVO extends SysUser {
    private Long deptId;
}
