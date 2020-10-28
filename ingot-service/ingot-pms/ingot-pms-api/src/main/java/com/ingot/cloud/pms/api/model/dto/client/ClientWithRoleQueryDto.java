package com.ingot.cloud.pms.api.model.dto.client;

import com.ingot.framework.base.model.dto.BaseQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : SystemWithRoleQueryDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/3/10.</p>
 * <p>Time         : 9:38 AM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ClientWithRoleQueryDto extends BaseQueryDto {
    private String clientId;
    private String description;
}
