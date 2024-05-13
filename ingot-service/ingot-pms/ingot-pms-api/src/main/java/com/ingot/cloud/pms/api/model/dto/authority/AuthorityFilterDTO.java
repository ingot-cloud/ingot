package com.ingot.cloud.pms.api.model.dto.authority;

import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : AuthorityFilterDTO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/5/13.</p>
 * <p>Time         : 16:11.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AuthorityFilterDTO extends SysAuthority {
    /**
     * 组织类型
     */
    private String orgTypeText;
}
