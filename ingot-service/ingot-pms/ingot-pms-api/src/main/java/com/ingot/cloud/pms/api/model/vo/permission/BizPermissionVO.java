package com.ingot.cloud.pms.api.model.vo.permission;

import com.ingot.cloud.pms.api.model.domain.MetaPermission;
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
public class BizPermissionVO extends MetaPermission {
    /**
     * 是否为元数据角色绑定的权限
     */
    private boolean metaRoleBind;
}
