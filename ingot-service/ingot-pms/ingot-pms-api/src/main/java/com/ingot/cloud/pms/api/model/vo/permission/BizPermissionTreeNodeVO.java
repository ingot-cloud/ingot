package com.ingot.cloud.pms.api.model.vo.permission;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : BizAuthorityTreeNodeVO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/18.</p>
 * <p>Time         : 16:27.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizPermissionTreeNodeVO extends PermissionTreeNodeVO {
    /**
     * 是否为元数据角色绑定的权限
     */
    private boolean metaRoleBind;
}
