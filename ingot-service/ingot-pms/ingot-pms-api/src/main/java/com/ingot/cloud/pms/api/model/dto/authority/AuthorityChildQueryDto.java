package com.ingot.cloud.pms.api.model.dto.authority;

import com.ingot.framework.base.model.dto.BaseQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : AuthorityChildQueryDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/12.</p>
 * <p>Time         : 11:07 AM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AuthorityChildQueryDto extends BaseQueryDto {
    private String pid;
    private String authorityName;
    private String authorityCode;
    private String status;
}
