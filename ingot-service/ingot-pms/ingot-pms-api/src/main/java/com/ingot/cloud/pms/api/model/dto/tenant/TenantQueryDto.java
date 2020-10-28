package com.ingot.cloud.pms.api.model.dto.tenant;

import com.ingot.framework.base.model.dto.BaseQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : TenantQueryDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/8.</p>
 * <p>Time         : 3:30 PM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TenantQueryDto extends BaseQueryDto {
    private String name;
    private String code;
    private String status;
}
