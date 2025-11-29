package com.ingot.cloud.pms.api.model.bo.permission;

import java.io.Serializable;

import lombok.Data;

/**
 * <p>Description  : PermissionIdBO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/29.</p>
 * <p>Time         : 09:39.</p>
 */
@Data
public class PermissionIdBO implements Serializable {
    /**
     * 权限ID
     */
    private Long id;
    /**
     * 是否为元数据角色绑定的权限
     */
    private Boolean metaRoleBind;

    public static PermissionIdBO of(Long id, Boolean metaRoleBind) {
        PermissionIdBO bo = new PermissionIdBO();
        bo.setId(id);
        bo.setMetaRoleBind(metaRoleBind);
        return bo;
    }
}
