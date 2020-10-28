package com.ingot.cloud.pms.api.model.dto.user;

import com.ingot.framework.base.model.dto.BaseQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : UserDetailQueryDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/9.</p>
 * <p>Time         : 4:33 PM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserDetailQueryDto extends BaseQueryDto {
    private String username;
    private String real_name;
    private String mobile;
    private String status;
    private long tenant_id = 0;
    private long dept_id = 0;
    private String client_id;
    private long role_id = 0;
}