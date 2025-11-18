package com.ingot.cloud.pms.api.model.vo.authority;

import com.ingot.cloud.pms.api.model.domain.MetaAuthority;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : BizAuthorityVO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/18.</p>
 * <p>Time         : 16:26.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizAuthorityVO extends MetaAuthority {
    /**
     * 是否为元数据角色绑定的权限
     */
    private boolean metaRoleBind;
}
