package com.ingot.cloud.iam.api.model.vo.permission;

import com.ingot.cloud.iam.api.model.domain.PlatformPermission;
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
public class BizPermissionVO extends PlatformPermission {
    /**
     * 是否为平台角色绑定的权限
     */
    private boolean platformRoleBind;
    /**
     * 是否为预设权限
     */
    private Boolean defaultFlag;
}
