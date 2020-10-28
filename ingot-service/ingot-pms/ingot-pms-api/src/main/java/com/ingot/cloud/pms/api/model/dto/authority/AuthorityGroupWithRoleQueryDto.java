package com.ingot.cloud.pms.api.model.dto.authority;

import com.ingot.framework.base.model.dto.BaseQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : AuthorityGroupWithRoleQueryDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/3/10.</p>
 * <p>Time         : 10:05 AM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AuthorityGroupWithRoleQueryDto extends BaseQueryDto {
    private String authority_code;
    private String authority_name;
    private long roleId;
}
