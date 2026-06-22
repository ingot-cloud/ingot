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
     * 是否为平台角色绑定的权限
     */
    private Boolean platformRoleBind;
    /**
     * 是否为预设权限
     */
    private Boolean defaultFlag;

    public static PermissionIdBO of(Long id, Boolean platformRoleBind, Boolean defaultFlag) {
        PermissionIdBO bo = new PermissionIdBO();
        bo.setId(id);
        bo.setPlatformRoleBind(platformRoleBind);
        bo.setDefaultFlag(defaultFlag);
        return bo;
    }
}
