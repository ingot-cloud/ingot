package com.ingot.cloud.pms.api.model.dto.client;

import com.ingot.framework.base.model.dto.BaseQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : RoleSystemQueryDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/21.</p>
 * <p>Time         : 4:06 PM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RoleClientQueryDto extends BaseQueryDto {
    private String roleId;
    private String clientId;
    private String description;
}
