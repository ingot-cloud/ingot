package com.ingot.cloud.pms.api.model.dto.user;

import com.ingot.framework.base.model.dto.BaseQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : UserWithRoleInfoQueryDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/27.</p>
 * <p>Time         : 3:30 PM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserWithRoleInfoQueryDto extends BaseQueryDto {
    private String username;
    private String real_name;
}
