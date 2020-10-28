package com.ingot.cloud.pms.api.model.dto.authority;

import com.ingot.framework.base.model.dto.BaseQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : AuthorityQueryDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/3/7.</p>
 * <p>Time         : 2:33 PM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AuthorityQueryDto extends BaseQueryDto {
    private String authorityName;
    private String authorityCode;
    private String status;
}
