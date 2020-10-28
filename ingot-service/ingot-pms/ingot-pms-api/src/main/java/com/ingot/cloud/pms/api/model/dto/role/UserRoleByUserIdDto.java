package com.ingot.cloud.pms.api.model.dto.role;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : UserRoleByUserIdDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/3/8.</p>
 * <p>Time         : 12:51 PM.</p>
 */
@Data
public class UserRoleByUserIdDto implements Serializable {
    private long user_id;
    // 条件参数，可以不传
    private String role_code;
    private String role_name;
}
